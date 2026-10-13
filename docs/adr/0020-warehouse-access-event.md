# ADR-0020: 직원 창고 소속 변경 이벤트를 공통 envelope으로 발행한다

## 상태
Accepted

## 배경 (Context)
`dozy-inventory-api`는 창고 관리자의 창고별 접근을 제어하려고 직원의 창고 소속 사본(`warehouse_access`)을 유지한다(inventory ADR-0032).
소속은 WMS가 소유하므로(`warehouse_member`), WMS가 소속이 바뀔 때마다 `WAREHOUSE_ACCESS_CHANGED` 이벤트를 발행해야 한다. 이벤트가 나가지 않으면
사본이 없는 창고 관리자는 inventory의 모든 요청이 403이다. inventory가 제안한 MSA 공통 이벤트 형식(envelope)을 따른다.

이벤트의 의미는 "그 직원의 소속 창고 전체 목록으로 교체"다. 같은 이벤트를 여러 번 받아도 결과가 같고, 놓치거나 순서가 바뀌어도 다음 이벤트로 바로잡힌다.

고려한 대안:
- **커밋 직후 바로 Kafka로 발행**: 가장 단순하지만 커밋과 발행 사이에 장애가 나면 이벤트가 유실돼 사본이 틀어진다. 기각.
- **변경마다 완성된 이벤트를 아웃박스에 쌓기**: 교체 의미에서는 오래된 이벤트를 쌓을 이유가 없고 순서·격리 규칙이 따라온다. 기각.
- **변경한 직원 목록만 남기고 발행 시점의 소속을 읽기(채택)**: 직원당 한 행이라 연속 변경이 합쳐지고, 내용을 변경 시점에 스냅샷하지 않으므로
  동시 변경이 서로의 소속을 가리는 문제(REPEATABLE READ에서 각자 못 보는 행)가 없다.

## 결정 (Decision)

### 1. 형식은 공통 envelope이다
- 토픽 `dozy.wms.warehouse-access`, 메시지 키 `principalId`. 값은 `{eventId, eventType: "WAREHOUSE_ACCESS_CHANGED", eventVersion: 1, occurredAt, source: "wms",
  data: {principalId, warehouseIds}}`다.
- 헤더: `event-id`, `event-type`, `event-version`, `source`, `aggregate-type`(`WAREHOUSE_ACCESS`), `aggregate-id`(`principalId`), 추적 컨텍스트가 있을 때 `traceparent`.
- `eventId`는 UUID v7, 날짜·시각은 오프셋 포함 ISO-8601, 값이 `null`인 필드는 보내지 않는다.
- `warehouseIds`는 중복 없는 양수이고 최대 1000개다. 소속이 없으면 빈 배열이다.
- 공통 직렬화(`EventEnvelope`)는 `global/event`에 두어 이후 WMS 이벤트가 같은 형식을 쓰게 한다.

### 2. 발행 대기는 직원당 한 행이다
- `warehouse_access_event`: `principal_id`(유일), `event_id`, `occurred_at`, `attempt_count`, `next_attempt_at`, `last_error`와 감사 컬럼.
- `assign`/`remove`가 소속을 바꾸는 같은 트랜잭션에서 그 직원의 행을 upsert한다(`INSERT ... ON DUPLICATE KEY UPDATE`). 이미 행이 있으면 `event_id`와
  `occurred_at`을 새로 갱신하고 시도 정보를 초기화한다. 이미 배정된 직원을 다시 배정하는 등 아무것도 바뀌지 않으면 기록하지 않는다.
- 이벤트의 `warehouseIds`는 **발행 시점의 커밋된 소속**이다. 변경 시점 스냅샷이 아니므로 `occurredAt`은 마지막 변경이 기록된 시각이고 내용은 그 이후까지
  반영될 수 있다. 교체 의미라 항상 최신 상태가 맞다.
- 같은 행을 재시도하는 동안 `eventId`는 같다. 다시 변경되면 새 `eventId`가 된다.

### 3. 발행기
- 스케줄러(기본 5초)가 보낼 차례인 행을 `FOR UPDATE SKIP LOCKED`로 점유해 현재 소속을 읽고 Kafka로 보낸다(브로커 확인까지 기다린다). 성공하면 같은 `event_id`일 때만
  행을 지운다. 발행 중에 다시 변경되어 `event_id`가 바뀌었다면 행이 남아 다음 주기에 새 이벤트로 나간다.
- 실패하면 시도 횟수를 올리고 지수 백오프(5초에서 두 배, 최대 5분)로 다음 시도를 미룬다. **격리하지 않는다.** 교체 의미라 늦게 보내도 최신 상태이므로
  잃는 것이 없다. 시도가 10회부터 `ERROR` 로그(`WAREHOUSE_ACCESS_EVENT_RETRYING`)로 알린다.
- Kafka가 죽어 있어도 `assign`/`remove`는 성공한다(발행은 비동기).

### 4. 처음 연동과 복구
- `POST /api/warehouse-members/republish`(`warehouse_admin`)가 소속이 있는 모든 직원의 행을 새로 만든다. 처음 연동할 때 사본을 채우고, 사본이 어긋났다고
  의심될 때 복구하는 용도다. 앱 기동 시 자동 전체 발행은 하지 않는다.
- 토픽은 `cleanup.policy=compact`로 만들어 직원마다 최신 상태만 보관한다(키가 `principalId`). inventory의 새 컨슈머 그룹이 처음부터 읽어도 사본이 채워진다.
  앱이 `NewTopic`으로 생성하며(`wms.warehouse-access-event.topic.*`), 운영에서 인프라가 미리 만들면 그 설정이 우선한다.

### 5. 로컬 개발과 테스트
- 로컬 `docker-compose.yml`에 Kafka(KRaft 단일 노드)를 추가한다. 호스트 포트는 같은 PC에서 inventory 저장소의 Kafka(`9092`)와 겹치지 않게 `9094`로 둔다.
  운영·배포 환경의 브로커 주소는 `KAFKA_BOOTSTRAP_SERVERS` 환경변수로 따로 주입하므로 이 포트와 무관하다.
- 발행기 통합 테스트는 Testcontainers Kafka를 쓴다. 나머지 컨텍스트 테스트는 발행기를 끈다(`wms.warehouse-access-event.publisher.enabled=false`).

## 범위 밖
- inventory 수신부가 envelope(`data` 아래)를 읽도록 바꾸는 작업. 두 서비스가 첫 배포 전이므로 같은 시점에 맞춘다(inventory 저장소 작업).
- WMS가 이후 발행할 다른 이벤트, 이벤트 스키마 레지스트리.

## 결과 (Consequences)
- 얻는 것: 소속 변경이 유실 없이 inventory 사본에 반영되고, 연속 변경이 합쳐지며, 재시도에도 `eventId`가 고정되어 구독자가 중복을 거를 수 있다. 사본이 어긋나면
  재발행 API 한 번으로 복구된다.
- 감수하는 것: 이벤트 이력이 남지 않는다. 이벤트 내용이 변경 시점이 아니라 발행 시점의 소속이다. 장기 Kafka 장애에는 로그 알림에만 의존한다. 발행 트랜잭션이
  Kafka 확인 시간 동안 행 잠금과 커넥션을 잡는다. inventory 수신부 변경이 선행되지 않으면 inventory가 이 형식을 읽지 못한다.
