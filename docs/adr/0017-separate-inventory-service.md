# ADR-0017: 재고(수량·Lot·예약)를 dozy-inventory-api로 분리하고 WMS는 물리 위치 장부와 호출 아웃박스를 둔다

## 상태
Accepted

## 배경 (Context)
재고 수량, Lot, 재고 이력, 점유(Allocation), 상품 마스터는 `dozy-inventory-api`(이하 inventory)로 이관됐다. inventory는 재고의
단일 진실 공급원(SSOT)이며 `창고 × 상품 × Lot × 품질 상태` 단위의 총·예약·가용 수량, 예약과 FEFO 할당, 유통기한 스캔, 수량 변동
이력, 조정 확정을 책임진다. Zone·Location·Capacity는 알지 못한다. 데이터는 이관하지 않고 개발 DB를 시더로 다시 채운다(inventory
ADR-0005).

WMS는 이제 재고 코드를 덜어내고 inventory와 연동해야 한다. 그런데 WMS의 현재 구조에는 이관만으로 풀리지 않는 지점이 있다.

- **WMS 재고 행은 Location 단위다.** 입고 적재, 폐기 release, 출고 피킹, 실사 스냅샷이 모두 `locationId`를 필요로 하고,
  `disposal_item`·`stock_audit_item`이 이 행을 FK로 참조한다. inventory는 Location을 모른다.
- **출고 진입점이 뒤집힌다.** 현재는 WMS가 출고 요청을 받아 자체 FIFO 할당을 한다. 이관 후에는 OMS/Store가 inventory에 예약하고
  WMS는 그 결과를 따른다.
- **Lot 생성 규칙이 다르다.** inventory는 입고에서만 Lot을 만들고, 반품은 등록된 Lot만 받는다(없으면 404).
- **원자성이 사라진다.** 입고 완료 같은 확정이 한 트랜잭션에서 HTTP 호출로 나뉜다.

고려한 대안:
- **Location 장부 없이 WMS가 수량을 전혀 갖지 않는다**: 적재 위치, Location 용량 정합성, 실사 스냅샷, 대사용 `stock-quantities`를
  계산할 근거가 없다. 기각.
- **WMS가 수동 출고 요청 경로를 유지한다(직접 예약 생성)**: 예약을 만드는 주체가 둘이 되어 가용 재고 규칙이 두 곳에서 생기고,
  시나리오에도 없다. 기각.
- **반품에서 신규 Lot 생성을 inventory에 요청한다**: 유연하지만 "반품은 이미 입고된 Lot"이라는 inventory의 설계 원칙과 어긋난다.
  기각.
- **inventory 호출 실패 시 WMS 확정을 롤백한다(동기 실패)**: inventory 장애가 현장 작업을 멈춘다. inventory 원칙 8(현장을
  inventory 장애에 묶지 않는다)에 어긋난다. 기각.

## 결정 (Decision)

### 1. 책임 경계
- 수량·Lot·이력·예약·유통기한 상태의 진실은 inventory다. WMS는 이 데이터를 소유하지 않는다.
- WMS는 창고 마스터, Zone/Location/Capacity, 입고·출고·반품·폐기·실사의 작업 흐름(검수·적재·피킹·승인)을 소유한다.
- 상품 마스터는 inventory가 소유한다(inventory ADR-0004). WMS는 상품 이벤트로 로컬 사본을 유지하고 카테고리→Zone 매핑을 가진다.
  상품 ID는 기존 값을 그대로 승계한다.

### 2. Location 보조 장부
- WMS는 `Location × 상품 × Lot 번호 × 품질 상태 × 수량`만 가진 물리 위치 장부를 둔다. 수량의 진실이 아니라 "그 수량이 어느
  Location에 있는가"의 기록이다.
- 적재·피킹·폐기 release·실사 스냅샷과 inventory 대사용 창고 합계(`GET /api/v1/stock-quantities`)는 이 장부로 계산한다.
- Lot은 번호만 가진다(Lot 엔티티·유통기한 판정은 두지 않는다). 유통기한이 필요하면 inventory 조회나 이벤트 데이터를 쓴다.
- 장부 갱신과 inventory 호출(결정 4)은 항상 같은 확정 트랜잭션에서 함께 기록한다. 두 값이 어긋나면 inventory의 하루 1회 대사
  배치가 드러낸다(inventory 원칙 7: 물리 사실은 WMS가 기준이다).

### 3. 출고는 이벤트 기반만 둔다
- OMS/Store가 inventory에 예약하고, WMS는 `RESERVATION_CONFIRMED`(`allocations[]`의 Lot별 수량)를 구독해 출고 작업을 만든다.
- 피킹 완료 후 `POST /reservations/{id}/fulfillment`로 실출고 수량을 보낸다. 피킹 불가 Lot은 `reallocation`으로 보고하고, 취소 확정
  후에는 `release`를 호출한다.
- WMS 자체의 수동 출고 요청 경로와 FIFO 피킹 추천·할당 로직은 제거한다. 내부 출고 같은 새 요구가 생기면 예약을 만드는 쪽이 늘어나는
  형태로 이벤트 기반 흐름에 합류시킨다.

### 4. 반품 Lot은 기존 Lot만 허용한다
- 반품 검수의 로트 번호는 inventory에 등록된 Lot이어야 한다. 등록되지 않은 Lot이면 검수에서 거부하고 담당자가 확인한다. WMS는
  반품으로 Lot을 새로 만들지 않는다.
- 입고에서만 inventory가 Lot을 만든다. 입고 확정 요청에 로트 번호·제조일자·유통기한을 담아 보낸다.
- 불량 판정은 입고·반품 요청의 `qualityStatus`(`NORMAL`/`DEFECTIVE`)로 전달한다. 유통기한 경과로 인한 `DISPOSAL_SCHEDULED` 전환은
  inventory의 유통기한 스캔이 하고, WMS는 `LOT_EXPIRED`로 폐기 신청(`REQUESTED`)을 만든다.

### 5. inventory 호출은 로컬 커밋 + DB 아웃박스로 보낸다
- 입고·반품·폐기·출고 확정은 WMS에서 먼저 커밋하고, 호출 요청을 같은 트랜잭션으로 WMS DB의 아웃박스 테이블에 저장한다.
  별도 발송기가 inventory를 호출하고 실패하면 재시도한다.
- 확정 건마다 `Idempotency-Key`를 고정해 재전송해도 중복 반영되지 않게 한다. 이력에 대응하는 WMS 문서 ID(입고 상품, 반품 상품,
  폐기 상품, 예약)를 원인 문서로 쓴다.
- inventory 판단이 필요한 작업(실사 조정, Lot 재할당)은 inventory가 응답할 때까지 대기한다.
- 응답의 `quantityAfter`와 WMS 장부의 비교는 비동기로 한다. 불일치는 알림 대상으로 남긴다.
- 격리(DEAD)된 호출은 운영자가 확인해 재전송한다.

### 6. 인증과 이벤트
- WMS는 `dozy-auth` system client `svc-wms`로 inventory를 호출한다(`inventory:service` role).
- WMS는 직원 창고 소속 변경을 `dozy.wms.warehouse-access` 토픽의 `WAREHOUSE_ACCESS_CHANGED`로 발행하고(처음 연동 시 전 직원 분을
  한 번 발행), inventory 대사용 `GET /api/v1/stock-quantities`를 제공한다.
- 구독 이벤트는 최소 한 번 전달이므로 `event-id` 헤더로 중복을 거른다.

## 범위 밖
이 ADR은 방향과 경계만 정한다. 아래는 별도 작업 또는 후속 ADR에서 다룬다.
- Location 장부의 스키마와 기존 `inventory` 테이블에서의 전환 방법, `disposal_item`·`stock_audit_item`의 FK 대체.
- 아웃박스 테이블 스키마, 발송기·재시도 정책, 이벤트 소비 인프라(Kafka 클라이언트 설정).
- 출고 도메인(`Outbound`)의 상태 흐름 재설계와 취소·재할당 연계.
- 실사 조정 단위(상품×Lot번호×품질)와 승인 임계치 중복의 정리, 부분 폐기 허용 여부.
- 반품 검수 수령 라인 전환(폐기한 구 ADR-0017 초안)의 재작성. 이 ADR의 결정 4(기존 Lot만 허용)를 전제로 다시 설계한다.
- `devseed` 개편과 개발 DB 초기화 절차.

## 결과 (Consequences)
- 얻는 것: 재고 수량·예약·가용 계산이 한 곳으로 모여 초과 판매 방지와 채널 확장(OMS, Store)이 가능해진다. WMS는 현장 작업과
  물리 위치에 집중한다. inventory 장애가 입고·출고·반품 확정을 막지 않는다.
- 감수하는 것: 같은 수량이 inventory와 WMS 장부에 이중으로 존재하고 두 값은 최종 일관성이다. 확정과 반영 사이에 지연이 생기고
  불일치 감지는 비동기이며 대사에 의존한다. WMS에 아웃박스, 이벤트 소비, Location 장부, 연동 클라이언트가 추가되어 운영 구성요소가
  늘어난다. 출고·반품 API 계약과 흐름이 바뀌어 기존 코드와 테스트를 광범위하게 고쳐야 한다. 반품은 등록되지 않은 Lot을 만나면
  자동 처리되지 않고 사람의 확인이 필요하다.
- 전제: WMS에 보존할 운영 데이터가 없다(개발 단계). 운영 데이터가 생긴 뒤의 전환이라면 이 ADR을 다시 검토한다.
