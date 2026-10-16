# ADR-0021: 상품 마스터를 inventory 이벤트로 유지하는 로컬 사본으로 전환한다

## 상태
Accepted

## 배경 (Context)
inventory 서비스가 상품 마스터를 소유한다(inventory ADR-0004, WMS ADR-0017). 상품은 WMS의 입고·출고·반품이 카테고리(→ Zone 매핑)와 유통기한 일수를 읽는 기준이고
`lot`, `inventory`, `location_stock`, `inbound_item`, `outbound_item`, `return_item`이 `product_id`를 FK로 참조한다. inventory가 장애여도 WMS가 입고·출고를 진행해야
하므로(inventory 원칙 8) 상품 정보는 WMS가 로컬에 가져야 하고, inventory는 상품이 등록·변경될 때마다 `PRODUCT_REGISTERED`/`PRODUCT_STATUS_CHANGED`를
변경 후 전체 스냅샷으로 발행한다(토픽 `dozy.inventory.product`, 키 `productId`).

고려한 대안:
- **WMS가 상품 API를 계속 소유하고 inventory가 따라간다**: 소유가 둘이 되어 한쪽에만 있는 상품(입고 호출이 404)이 생긴다. 기각.
- **필요할 때마다 inventory를 조회**: inventory 장애가 WMS 작업을 막는다. 기각.
- **이벤트로 로컬 사본 유지(채택)**: 장애에 독립이고 FK를 유지할 수 있다.

## 결정 (Decision)

### 1. WMS는 상품을 쓰지 않는다
- 상품 등록·활성·비활성·삭제 API와 UseCase를 제거하고 조회(`GET /api/products`, `/{id}`)만 남긴다. 상품의 등록과 상태 변경은 inventory의 `/api/v1/products`에서 한다.
  `dozy-admin-console`이 그쪽을 호출하도록 바뀌어야 한다(콘솔 저장소 작업).
- 사본 모델은 불변 값이다. 상태를 바꾸는 메서드 없이 스냅샷으로 통째로 교체한다. `category → ZoneCode` 매핑은 WMS가 계속 가진다.

### 2. 상품 ID는 inventory의 값을 그대로 쓴다
- `product.product_id`는 이벤트의 `productId`다. 컬럼의 `AUTO_INCREMENT`는 그대로 두되 WMS가 채번하지 않는다(저장소는 ID가 이미 있으면 갱신, 없으면 그 ID로 새로 저장).
  기존 개발 데이터의 상품 ID는 inventory와 달라 충돌하므로 개발 DB는 `scripts/reset-dev-db.sh`로 초기화한다(ADR-0005와 같은 전제).
- V8 마이그레이션: soft delete 컬럼(`deleted_at`, `deleted_by`)을 제거하고 `source_occurred_at`(마지막으로 반영한 이벤트의 발생 시각)을 추가한다.

### 3. 이벤트 반영 규칙
- 사본이 없으면 만들고 있으면 스냅샷으로 교체한다(upsert). **반영한 이벤트와 같거나 오래된 `occurredAt`의 이벤트는 무시한다.** 중복 전달과 DLT 재처리처럼 순서가 바뀐 전달이 최신 사본을
  덮지 못하게 한다.
- 이벤트의 `occurredAt`은 `Asia/Seoul` 로컬 시각으로 바꿔 저장한다. 감사 컬럼에는 요청 밖 작업이므로 `system`이 기록된다.
- 상품 사본과 무관한 알 수 없는 이벤트 유형은 경고 로그를 남기고 무시한다(앞으로 토픽에 다른 상품 이벤트가 생겨도 소비자가 멈추지 않는다).
- 지원하지 않는 `eventVersion`, 알 수 없는 카테고리·상태, 필수 필드 누락, JSON 오류는 잘못 읽는 것보다 낫도록 **재시도 없이 DLT로 보내고** 다음 메시지로 넘어간다.

### 4. 소비자 구성
- `@KafkaListener` 한 건씩 처리, 그룹 `dozy-wms-api`, `auto-offset-reset: earliest`. 처리 실패는 1초 간격 3회 재시도 후 `{토픽}.dlt`로 보낸다(`KafkaConsumerConfig`).
  DLT 재처리 수단은 만들지 않고 로그 알림까지만 둔다.
- 설정: `wms.product-event.listener.enabled`(기본 true, 테스트에서 끔), `wms.product-event.listener.topic`(기본 `dozy.inventory.product`).

### 5. 처음 채우기와 누락 복구
- WMS는 토픽을 처음부터 읽어 사본을 채운다. 이벤트가 영구히 남도록 **`dozy.inventory.product` 토픽은 로그 압축(`cleanup.policy=compact`)이어야 한다.** 토픽은 서비스가 아니라 인프라가 이 설정으로 미리 만든다([ADR-0026](0026-topics-declared-by-infrastructure.md)).
  압축이 없으면 보관 기간이 지난 상품 이벤트가 사라져 새 환경의 WMS에 그 상품이 생기지 않는다.
- inventory를 조회해 사본을 맞추는 동기화 API는 이번 범위에 두지 않는다. 이벤트를 놓치는 일이 실제로 생기면 후속 작업으로 만든다.

### 6. 이벤트보다 먼저 쓰는 경우
- inventory에 상품을 등록한 직후 사본이 도착하기 전에 WMS에서 그 상품을 쓰면 `PRODUCT_NOT_FOUND`(404)가 나며 별도 대기 로직을 두지 않는다. 사본은 보통 수 초 안에 생기므로 재시도하면 된다.

## 범위 밖
- 토픽 생성과 압축 설정(인프라 저장소의 몫, ADR-0026), `dozy-admin-console`의 상품 쓰기 호출 변경.
- 사본 불일치를 찾아내는 동기화·대사 API, DLT 메시지 재처리 도구.

## 결과 (Consequences)
- 얻는 것: inventory 장애와 무관하게 입고·출고·반품이 상품 정보를 쓸 수 있고, 상품 소유가 한 곳이다. 중복·순서가 바뀐 전달에 안전하다.
- 감수하는 것: 상품 등록 직후 짧은 지연 동안 WMS에서 그 상품을 못 쓴다. 이벤트를 놓치면 사본이 어긋나고 복구 수단은 토픽 재소비(오프셋 재설정)뿐이다. 상품 쓰기 화면은
  inventory API로 옮겨질 때까지 동작하지 않는다.
