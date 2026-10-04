# ADR-0015: 에러 응답을 dozy-auth의 Problem Details 규약에 맞춤

## 상태
Accepted

## 배경 (Context)
`dozy-auth`는 에러 응답을 RFC 9457 Problem Details(`application/problem+json`)에 `code`와 `traceId`를 더한 형식으로
확정했다(`dozy-auth` `docs/api/conventions.md` §4, §11). 클라이언트는 `code`로만 분기한다. WMS가 쓰는
`auth-spring-boot-starter`도 401/403을 이 형식으로 응답한다.

반면 WMS의 나머지 에러는 `ErrorResponseDto { errorCode, message, timestamp }`였다. 한 서비스가 두 가지 에러 형식을
내보내는 상태였고(401/403은 Problem Details, 404/409 등은 별도 JSON), 필드 이름도 `code`와 `errorCode`로 달랐다.
`traceId`가 없어 서비스 간 장애 추적도 어려웠다.

변경 전 `GlobalExceptionHandler`는 `BusinessException`, `WebExchangeBindException`, 보안 예외만 구분하고 나머지를
`Exception` 핸들러가 받았다. 실제로 확인한 결과 깨진 JSON, 405, 415, 매핑 없는 경로의 404가 모두 **500
`COMMON_INTERNAL_SERVER_ERROR`**로 응답됐다. 클라이언트 오류가 서버 오류로 보이는 결함이었다.

고려한 대안:
- **현상 유지**: 응답 형식이 둘로 갈린 채 서비스가 늘어난다.
- **`dozy-auth`의 공통 에러 라이브러리를 공유**: `auth-server`의 핸들러는 Servlet(MVC) 전용이라 WebFlux인 WMS에서
  쓸 수 없고, 에러 형식 변경이 모든 서비스의 재배포를 묶는다. 스타터도 이를 피하려고 JSON을 직접 조립한다.
- **규약(스펙)만 공유하고 WebFlux로 구현**: 채택.

## 결정 (Decision)

### 1. 응답 형식은 `dozy-auth` 규약을 따른다
- 필드는 `type`(`https://docs.dozycoffee.com/errors/{code를 kebab-case로}`), `title`, `status`, `detail`, `instance`,
  `code`, `traceId`이고 검증 실패에는 `errors[{field, code, message}]`가 붙는다.
- `code`는 `ErrorCode.code`다. 앱은 `code`로만 분기하고 `detail`은 디버깅용이다.
- 500은 내부 정보(스택, SQL, 클래스명)를 응답에 싣지 않고 로그에만 남긴다.
- 모든 응답에 `X-Trace-Id` 헤더가 붙고 에러 응답의 `traceId`와 같은 값이다. `TraceIdWebFilter`는 Security 체인보다
  먼저 실행되어 요청 헤더를 정규화하므로 스타터의 401/403 응답과 traceId가 일치한다. 요청의 값은 `[A-Za-z0-9-]{1,64}`일
  때만 받아 쓴다(헤더 주입 방지, 스타터와 같은 규칙).

### 2. 구현은 WebFlux `ResponseEntityExceptionHandler`를 상속한다
- `GlobalExceptionHandler`가 `handleExceptionInternal`을 오버라이드해 프레임워크가 만든 4xx(400, 404, 405, 415 등)도
  같은 형식으로 맞춘다. `ErrorResponseDto`는 제거한다.
- `ErrorType` → HTTP 상태 매핑(`ErrorTypeHttpStatusMapper`)과 `BusinessException` 계층은 그대로 둔다.
  `AuthException(code, status)`처럼 상태 코드를 예외가 직접 들고 있지 않고, 도메인별 `ErrorCode` enum이 컴파일 타임에
  코드를 검증하는 현재 구조가 낫다.
- 보안 예외(`AccessDeniedException`, `AuthenticationException`)는 핸들러가 삼키지 않고 Security 체인에 재전파한다.

### 3. 범용 에러 코드는 `dozy-auth` 에러 코드 표의 이름을 쓴다
- `CommonErrorCode`: `INVALID_INPUT` → `VALIDATION_FAILED`, `INTERNAL_SERVER_ERROR` → `INTERNAL_ERROR`.
  프레임워크 에러는 상태에 따라 `VALIDATION_FAILED`(400), `NOT_FOUND`(404), `METHOD_NOT_ALLOWED`(405),
  `UNSUPPORTED_MEDIA_TYPE`(415) 등 표의 이름을 쓴다.
- 도메인 코드는 기존대로 도메인 접두사를 유지한다(`PRODUCT_NOT_FOUND`). 서비스가 늘어도 충돌하지 않는다.
  접두사 없는 코드는 서비스 공통 범용 코드로 한정한다.

### 4. 예외 클래스를 만드는 기준
- 필드 값의 단순 검증(null, 빈 값, 범위)은 전용 클래스 없이 `ErrorCode`만 등록하고 `InvalidDomainValueException`으로 던진다.
- 호출부가 타입으로 구분하거나 테스트가 `assertThrows<구체타입>`으로 검증할 규칙 위반(상태 전이, 용량, 수량 부족, NotFound,
  Duplicate 등)은 전용 예외 클래스를 만든다. 계층(`DomainException`/`ApplicationException`)은 던지는 위치를 나타낼 뿐
  핸들러 동작은 같다.

## 결과 (Consequences)
- 얻는 것: 한 서비스 안의 에러 형식 통일, `dozy-auth`와의 형식 일치, `traceId` 기반 추적, 클라이언트 오류가 500으로 보이던
  결함 해소, 검증 실패의 필드별 구조화.
- 감수하는 것: 응답 계약이 바뀐다(`errorCode` → `code`, `timestamp` 제거, `message` → `detail`, 검증 메시지 문자열 → `errors[]`).
  소비자인 `dozy-admin-console`의 `httpClient`는 `code`를 `errorCode ?? code`로 읽어 이미 호환되지만, 메시지는
  `message`만 읽으므로 콘솔이 `detail`을 읽도록 바꾸기 전까지 서버 메시지 대신 기본 문구("요청이 실패했습니다. (status N)")가
  표시된다. 이 변경과 함께 콘솔도 갱신해야 한다. 프레임워크 오버라이드가 `Mono`를 반환하므로 이 핸들러에는 Reactor 타입이
  남는다(ADR-0014의 "프레임워크 경계" 예외).
- 아직 처리하지 않은 것: 유니크 제약 위반(`DataIntegrityViolationException`)은 서비스의 사전 중복 조회를 동시 요청이
  통과하면 여전히 500이다. 변환할 `code`가 `dozy-auth` 에러 코드 표에 없어 규약 확정 후 별도로 다룬다.
- 감사 포인트: 새 `ErrorCode`를 추가할 때 `code`가 서비스 간에 겹치지 않는지, 범용 코드(접두사 없음)를 도메인 코드로
  재사용하지 않는지 확인한다.
