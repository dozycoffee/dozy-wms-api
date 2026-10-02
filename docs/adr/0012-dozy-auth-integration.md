# ADR-0012: dozy-auth 연동과 접근 제어 모델

## 상태
Accepted (ADR-0010을 대체한다. ADR-0011의 "1. 인증/인가" 절은 이 문서로 구체화한다)

## 배경 (Context)

[ADR-0010](0010-current-access-scope-provider-port.md)은 인증 서비스가 없던 시점에 `AccessScope`
(`userId` + `warehouseIds`)와 `CurrentAccessScopeProvider` 포트, Mock 어댑터를 만들어 두었다. 이후
`dozy-auth`의 명세가 확정되면서 두 가지가 분명해졌다.

- `dozy-auth`는 JWT(RS256)와 JWKS로 **"누구인가"(`principalId`)와 "어떤 role을 가졌는가"(`roles`)만
  보증**한다. 토큰에는 창고 같은 대상별 범위 정보가 없다. 대상별 권한을 role로 만들지 않는 것이
  `dozy-auth`의 원칙이다(`dozy-auth` ADR-0005: 점주-매장 관계를 Auth가 아닌 Store가 관리).
- 서비스가 할 일은 토큰 검증(스타터 제공), 기능 인가(role), **리소스 인가(소유·소속 여부, 서비스 자체
  데이터)** 다. 리소스 인가는 모든 서비스의 필수 규칙이다.

`AccessScope`는 "누구인지"와 "어느 창고에 접근 가능한지"를 한 객체에 섞고 있어, 두 정보의 출처가 다른
Auth 모델과 맞지 않는다. 또한 `warehouseIds`가 빈 리스트면 "제한 없음"으로 해석되어, 접근 가능한 창고가
없는 사용자가 전체를 조회하게 되는 fail-open 위험이 있다.

## 결정 (Decision)

### 1. 책임을 셋으로 나눈다

| 질문 | 출처 | WMS 구성요소 |
|---|---|---|
| 누구인가 | Auth 토큰(`principalId`) | `Actor`, `CurrentActorProvider` |
| 어떤 기능을 쓸 수 있나 | Auth 토큰(`roles`) | 컨트롤러 `@PreAuthorize`, role 상수 |
| 어느 창고를 볼 수 있나 | **WMS 자체 데이터** | `WarehouseAccess`, `CurrentWarehouseAccessProvider` |

### 2. `Actor`는 WMS 자체 타입이다

- `UserActor(principalId: UUID, roles: Set<String>)`와 `SystemActor`로 나눈다.
- `SystemActor`는 스케줄러·시더처럼 요청 컨텍스트가 없는 작업에 쓴다.
- 서비스 계층이 Spring Security나 `auth-core` 타입에 직접 의존하지 않도록 어댑터가 토큰에서 변환한다.
- 감사 컬럼(`created_by` 등)에는 `Actor.auditName`을 기록한다. 사용자는 `principalId` UUID 문자열,
  시스템 작업은 `"system"`이다. 기존 `VARCHAR(100)` 컬럼에 그대로 들어가므로 스키마 변경이 없다.

### 3. 창고 접근 범위는 sealed 타입으로 표현한다

- `WarehouseAccess`는 `AllWarehouses`와 `OnlyWarehouses(ids)`로 나눈다. 조회 조건은
  `WarehouseFilter`(`Unfiltered` / `In(ids)` / `None`)로 반환해, "전체"와 "없음"을 빈 리스트로 구분하던
  모호성을 없앤다. 접근 가능한 창고가 없으면(`OnlyWarehouses(empty)`) 요청과 무관하게 `None`이다.
- 사용자-창고 매핑(WMS 자체 테이블)은 **창고가 2개 이상으로 늘기 전에** 도입한다. 그 전까지
  `AllWarehousesAccessProvider`가 모든 행위자에게 `AllWarehouses`를 반환한다.
- 매핑 도입 시 서비스 계층의 가드(`require(warehouseId)`)를 입고·출고·반품·폐기·실사의 등록과 조회에
  함께 적용한다. 호출부가 없는 가드를 미리 만들지 않기 위해 지금은 가드를 만들지 않는다.

### 4. 로컬 실행은 `local` 프로필에서만 인증을 우회한다

- Auth의 로그인·개발용 토큰 API가 준비되기 전까지 수동 호출이 막히지 않도록, `local` 프로필에서만
  고정 행위자로 요청을 통과시킨다. 다른 프로필에서는 이 빈이 만들어지지 않는다.
- 운영 프로필과 `local`이 함께 켜지면 기동에 실패하게 한다.
- Auth 로그인이 준비되면 우회를 제거한다.

## 검토한 대안

- **`AccessScope` 유지, 빈 리스트 의미만 변경**: 두 정보의 출처 차이가 그대로 남고, 인증 어댑터가
  창고 범위를 어디서 채울지 불명확하다.
- **창고별 role(`wms:inbound_manager_warehouse_1`)**: role이 창고 수만큼 늘고 Auth가 창고 데이터를 알아야
  한다. `dozy-auth`의 원칙(ADR-0005)과 충돌한다.
- **토큰 claim으로 창고 목록 전달**: Auth가 창고를 모르며, 토큰 계약(major 변경)을 건드려야 한다.
- **`auth-core`의 `AuthenticatedPrincipal`을 서비스 계층에서 직접 사용**: 구현은 단순하지만 서비스가
  인증 라이브러리의 타입에 결합한다.

## 결과 (Consequences)

- 감사 컬럼의 값이 `"system"`에서 사용자별 `principalId`로 바뀐다. 요청 컨텍스트 밖의 작업은 계속
  `"system"`이다. 기존 데이터는 변경하지 않는다.
- 창고가 1개인 동안 창고 접근은 실질적으로 제한되지 않는다. **창고를 추가하기 전에 매핑 도입이 선행
  조건**이다(이 문서가 감사 포인트).
- WMS의 role은 코드에서 쓰기 전에 Auth에 등록해야 한다(`dozy-auth` ADR-0017).
- 아직 검증이 필요한 항목: `suspend` 서비스 안에서 Reactor Context의 보안 컨텍스트가 실제로 읽히는지,
  스타터 기본 필터 체인에 CORS 설정이 없어 사전 요청(OPTIONS)이 막히지 않는지.
- CI와 로컬 빌드는 GitHub Packages 읽기 권한(`read:packages`)이 필요하다.
