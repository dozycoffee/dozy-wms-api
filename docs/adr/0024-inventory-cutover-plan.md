# ADR-0024: 재고 컷오버 계획 — 흐름을 장부 기준으로 전환(1단계)하고 레거시를 삭제(2단계)한다

## 상태
Accepted (ADR-0023 결정 2의 컷오버 세부를 정함)

## 배경 (Context)
F-032~F-039로 입고·반품·폐기·실사·출고가 모두 inventory 호출을 기록·전달한다. 기존 `Inventory`·`Lot`·`Allocation`·`InventoryHistory`와
`LocationStockMirror` 미러링은 컷오버(F-037)까지 남아 있다([ADR-0023](0023-add-inventory-calls-before-cutover.md)). 컷오버에서 정할 것이 남았다.

처음에는 "읽기를 장부로 옮기고(1), 쓰기를 옮기고(2), 삭제(3)"의 3단계를 검토했다. 그러나 `disposal_item.inventory_id`, `stock_audit_item.inventory_id`가
레거시 재고 행 ID를 FK로 쥐고 있고 출고 점유도 레거시 `Allocation`이라, 읽기만 먼저 옮기려면 레거시 ID와 장부 행 ID를 오가는 임시 변환이 필요하다. 입고·반품 쓰기만
먼저 옮기면 아직 레거시를 읽는 흐름이 틀린 수량을 본다. 그래서 흐름 전환은 쪼개지 못하고 하나로 하되, 삭제를 분리한다.

## 결정 (Decision)

### 1. 두 단계로 나눈다
- **1단계(F-037): 모든 흐름을 장부 기준으로 전환한다.** 입고·반품·폐기·실사·출고가 장부(`location_stock`)를 직접 읽고 쓰며, 미러링과 레거시 쓰기를 멈추고, `LOT_EXPIRED`
  수신을 추가한다. 레거시 코드는 사용처가 없어진 채 남는다.
- **2단계(F-040): 레거시를 삭제한다.** `inventory` 패키지(Inventory·Lot·Allocation·이력·유통기한 스캔·조회 API)와 테이블, 미러링 포트를 지운다.

### 2. 장부가 Location 사용량(`usedCapacity`)을 소유한다 (ADR-0018 결정 2 적용)
- `increase`/`decrease`는 Location `usedCapacity`도 함께 증감한다. 흐름의 `occupyLocation`/`releaseLocation` 호출은 없앤다. WorkArea 점유는 지금처럼 각 흐름이 한다.

### 3. FK 대체
- `disposal_item.inventory_id`, `stock_audit_item.inventory_id`는 `location_stock_id`로 바꾼다. 장부는 수량 0 행도 유지하므로 ID가 안정적이다. 개발 데이터는 이관하지 않고
  마이그레이션이 두 문서 테이블의 기존 행을 지운다(ADR-0017 전제).

### 4. 출고 피킹 점유
- `location_stock`에 `held_quantity`를 둔다(가용 = `quantity − held_quantity`). 확정 예약의 Lot 할당마다 `outbound_allocation_pick`(할당 × 장부 행 × 수량)이 피킹 계획이다.
  피킹 시작에 점유를 잡고, 취소·재할당 때 풀며, 출고 완료 때 `quantity`와 `held_quantity`를 함께 줄이고 Location 사용량을 해제한다. 수량·예약의 진실은 inventory다.

### 5. `LOT_EXPIRED` 수신 (`dozy.inventory.lot`)
- 이벤트의 창고·Lot 단위 `transfers[].quantity`를 장부의 해당 Lot 정상 행에 배분한다: 가용 수량(`quantity − held_quantity`)이 많은 Location부터, 같으면 Location 코드
  오름차순으로 `NORMAL → DISPOSAL_SCHEDULED`로 옮긴다(`transferQuality`). 옮긴 행마다 폐기 신청(`REQUESTED`, 사유 `EXPIRED`)의 폐기 상품을 만든다.
- 이벤트는 최소 한 번 전달되고 같은 Lot에 나중에 다시 발행될 수 있으므로, 폐기 신청에 `source_event_id`(이벤트 `eventId`)를 유니크로 두어 같은 이벤트를 두 번 처리하지 않는다.
- 장부 정상 수량이 이벤트 수량보다 적으면 있는 만큼만 옮기고 로그로 알린다(대사가 드러낸다).

### 6. 부분 폐기를 허용한다
- 폐기 상품 수량은 장부 행 수량 이하이면 된다. 같은 행에 미완료 폐기 상품이 여럿이면 합이 행 수량을 넘지 않아야 한다. 확정 시 장부를 해당 수량만큼 줄이고 inventory 호출을 기록한다.

### 7. 반품 Lot 확인
- 반품 검수의 Lot 번호는 장부에 `(상품, Lot 번호)` 행이 하나라도 있어야 한다(수량 0 행 포함). Lot은 입고로만 생기고 입고는 장부에 반영되므로 inventory 조회 호출 없이 같은 결과를 낸다.
  장부에 없으면 지금처럼 409(`RETURN_ITEM_LOT_NOT_REGISTERED`)다.

### 8. 제거하는 API와 유지하는 API
- 제거: `/api/inventories`(목록·상세·불량/폐기예정 수동 전환), `/api/lots`, `/api/inventory-histories`, 출고 권고. 콘솔은 inventory API를 호출한다(ADR-0018 결정 6).
- 유지: Zone/Location 기준 현황(`zone-summary`)은 장부 기반으로 남긴다.

## 결과 (Consequences)
- 얻는 것: 임시 변환 없이 흐름이 한 번에 장부로 옮겨지고, 2단계는 사용처 없는 코드 삭제라 위험이 작다. 수량 0 행·부분 폐기·피킹 점유가 장부 하나로 설명된다.
- 감수하는 것: 1단계 PR이 입고·반품·폐기·실사·출고 서비스와 영속성을 함께 바꿔 크다. 피킹 점유 컬럼과 계획 테이블이 늘어 취소·재할당 해제 경로를 모두 맞춰야 한다. 실사는 장부 행이 있는
  항목만 센다(없던 위치에서 발견된 재고는 입고·조정 절차가 필요하다).
