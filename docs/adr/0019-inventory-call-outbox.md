# ADR-0019: inventory 호출을 DB 아웃박스(inventory_call)로 전달한다

## 상태
Accepted

## 배경 (Context)
ADR-0017 결정 5에 따라 WMS는 입고·반품·폐기·출고 확정을 로컬에서 먼저 커밋하고 `dozy-inventory-api` 호출은 아웃박스로 전달한다.
이 ADR은 아웃박스의 구조, 순서·재시도·격리 규칙, 클라이언트 설정을 정한다. inventory의 변경 API는 모두 `Idempotency-Key` 헤더가 필수이고
같은 키의 재요청은 같은 결과를 돌려주므로(inventory ADR-0019) 재전송은 안전하다.

고려한 대안:
- **inventory Outbox 방식(실패해도 같은 키의 후속 호출은 계속 전송)**: 전체 정지는 없지만, 입고 호출이 실패한 채 같은 Lot의 폐기 호출이
  먼저 나가면 inventory에서 404가 나는 식으로 의존하는 호출이 연쇄로 실패한다. 이벤트 발행과 달리 이 호출들은 서로 의존한다. 기각.
- **확정 트랜잭션 안에서 동기 호출**: inventory 장애가 현장 확정을 막아 inventory 원칙 8에 어긋난다. 기각.
- **단일 인스턴스 가정의 단순 폴링**: 구현은 쉽지만 인스턴스를 늘리면 같은 호출이 동시에 나간다(멱등이라 결과는 같지만 낭비와 경합).
  기각하고 `FOR UPDATE SKIP LOCKED`로 점유한다.

## 결정 (Decision)

### 1. 테이블과 상태
- `inventory_call`: `call_type`, `warehouse_id`, `idempotency_key`(유니크, `VARCHAR(100) ascii_bin`), `request_path`, `request_body`(JSON 원문),
  `status`, `attempt_count`, `next_attempt_at`, `last_error`, `response_body`, `sent_at`, `dead_at`과 감사 컬럼.
- 상태: `PENDING`(전송 대기) → `SENT`(inventory가 2xx로 응답), `PENDING` → `DEAD`(재시도해도 소용없는 응답으로 격리), `DEAD` → `PENDING`
  (재전송) 또는 `DEAD` → `DISCARDED`(운영자가 버림). 허용 값은 CHECK 제약으로 제한한다(ADR-0013).
- `call_type`: `INBOUND_RECEIPT`, `RETURN`, `DISPOSAL`, `FULFILLMENT`, `REALLOCATION`, `RELEASE`, `STOCK_ADJUSTMENT`. 요청 본문은 각 도메인이
  JSON 문자열로 만들어 기록하며, 종류별 요청·응답 DTO는 해당 도메인 전환 작업에서 추가한다.

### 2. 기록
- 확정 트랜잭션이 `RecordInventoryCallUseCase`로 같은 트랜잭션에 호출을 적는다. 멱등 키는 결정적 값 `wms-{종류}-{WMS 문서 ID}`
  (예: `wms-inbound-receipt-123`, 100자 이하 ASCII)이다.
- 같은 키·같은 내용의 재기록은 기존 행을 돌려주고, 같은 키에 다른 경로·본문이면 409(`INVENTORY_CALL_KEY_CONFLICT`)다. 유니크 제약 위반도
  같은 예외로 바꾼다.

### 3. 전송 순서는 창고별 순차이고 DEAD가 나면 그 창고의 큐가 멈춘다
- 한 창고의 호출은 기록된 순서(id)대로 한 건씩 전송한다. 앞의 호출이 `PENDING`이거나 `DEAD`이면 같은 창고의 뒤 호출은 나가지 않는다.
  다른 창고의 호출은 영향받지 않는다.
- `DEAD`는 운영자가 재전송(`requeue`)하거나 버려야(`discard`) 큐가 다시 흐른다. 입고→폐기처럼 의존하는 호출이 앞 실패를 건너뛰고
  나가 연쇄 실패하는 일을 막기 위한 선택이다.

### 4. 재시도와 격리
- 재시도 대상: 연결 실패, 시간 초과, 5xx, 408, 429. 이 경우 `PENDING`으로 두고 시도 횟수를 올리며 지수 백오프로 `next_attempt_at`을 미룬다
  (기본 5초에서 시작해 두 배씩, 최대 10분). **횟수 상한으로 격리하지 않는다.** inventory가 오래 장애여도 호출을 잃지 않고(원칙 8), 시도가
  `alert-after-attempts`(기본 10)에 이르면 `ERROR` 로그(`INVENTORY_CALL_RETRYING`)로 알린다.
- 즉시 `DEAD`: 그 외 4xx(400, 404, 409, 422 등)와 해석할 수 없는 응답. 같은 요청을 다시 보내도 결과가 달라지지 않기 때문이다.
  `ERROR` 로그(`INVENTORY_CALL_DEAD`)에 응답 본문과 키를 남긴다. 특히 `INV_IDEMPOTENCY_KEY_CONFLICT`(409)는 키 생성 오류나 데이터 불일치
  신호다.

### 5. 점유와 동시성
- 전송기는 후보를 `SELECT ... FOR UPDATE SKIP LOCKED`로 점유해 한 트랜잭션 안에서 전송하고 결과를 갱신한다. 인스턴스가 여러 개여도 같은
  호출이 동시에 나가지 않는다. 후보는 "같은 창고에서 자신보다 앞선 `PENDING`/`DEAD`가 없고 `next_attempt_at`이 지난" 행(창고별 머리)이다.
- 전송 시간 제한은 기본 10초다. 한 트랜잭션이 잡고 있는 시간은 호출 한 번의 시간 제한 이내로 묶는다.
- 스케줄러가 주기(기본 5초)마다 배치를 반복 실행해 처리할 호출이 없을 때까지(최대 반복 횟수 제한) 비운다.

### 6. 클라이언트와 설정
- `dozy-auth` system client(`svc-wms`)의 `WebClient`로 `POST`한다. 설정은 inventory 쪽과 같은 방식이다(`AUTH_CLIENT_ENABLED`,
  `AUTH_CLIENT_ID`, `AUTH_CLIENT_SECRET`)이며 비밀은 환경변수로만 주입한다. 대상 주소는 `INVENTORY_BASE_URL`이다.
- 응답 본문은 `response_body`에 저장한다. `quantityAfter`와 장부의 비동기 비교는 각 도메인 전환 작업에서 이 값을 읽어 만든다.

### 7. 운영 API (warehouse_admin)
- `GET /api/inventory-calls?status=&limit=`, `GET /api/inventory-calls/{id}`, `POST /api/inventory-calls/{id}/requeue`,
  `POST /api/inventory-calls/{id}/discard`. 재전송·버림은 `DEAD`에만 가능하고 그 외 상태는 409다.

## 범위 밖
- 종류별 요청·응답 DTO와 각 도메인의 호출 전환(F-032~F-036).
- 응답 `quantityAfter`와 장부의 비교·알림.
- 격리 호출의 메트릭·대시보드(로그 알림까지만).

## 결과 (Consequences)
- 얻는 것: inventory 장애가 현장 확정을 막지 않고, 재전송이 멱등이라 안전하며, 의존하는 호출이 순서를 지켜 전달된다. 인스턴스를 늘려도
  중복 전송이 없다.
- 감수하는 것: `DEAD` 하나가 그 창고의 모든 후속 호출을 멈추므로 운영자가 확인·처리해야 한다(알림 의존). 확정과 inventory 반영 사이에
  지연이 생기고 비교는 비동기다. 재시도 대상 오류가 계속되면 큐가 길어지며 상한이 없어 알림에 의존한다. 트랜잭션이 호출 한 번의 시간 동안
  행 잠금과 커넥션 하나를 잡는다.
