# ADR-0018: Location 보조 장부(location_stock) 설계와 기존 재고에서의 미러링 전환

## 상태
Accepted

## 배경 (Context)
ADR-0017로 재고 수량·Lot·예약·이력의 진실은 `dozy-inventory-api`가 맡고, WMS는 "수량이 어느 Location에 있는가"를 기록하는
Location 보조 장부를 두기로 했다. 이 ADR은 그 장부의 모델, 기존 `inventory`에서의 전환 방식, 장부가 지켜야 할 규칙을 정한다.

현재 `inventory`는 Location 단위 행이지만 Lot 엔티티(`lot_id`), 점유(`allocated_quantity`, `allocation`), 이력(`inventory_history`),
soft delete까지 함께 가진다. `disposal_item.inventory_id`와 `stock_audit_item.inventory_id`가 이 행을 FK로 참조하고, 실사는
`inventory_history`로 스냅샷 이후 미반영 이동을 판정한다.

고려한 대안:
- **빅뱅 전환(`Inventory`를 장부로 바로 바꾸고 모든 흐름을 한 번에 수정)**: 임시 코드는 없지만 변경이 입고·반품·출고·폐기·실사 전체에
  걸려 한 PR이 너무 크다. 점유(Allocation)가 `inventory_id`에 묶여 있어 출고 재설계 전까지 중간 상태가 어색하다. 기각.
- **장부에 이동 이력 테이블을 둔다**: 실사 판정과 대사 불일치 추적이 정확해지지만 이력을 inventory로 넘기는 이관 방향과 반대다.
  필요해질 때 추가한다. 기각.
- **미러링 후 흐름별 전환**: 채택. 기존 변경 지점이 장부에도 같은 트랜잭션으로 반영하고, 각 흐름이 inventory 연동으로 바뀔 때
  미러링을 걷어낸다. 단계마다 테스트가 통과한다.

## 결정 (Decision)

### 1. 장부 모델
- 테이블 `location_stock`: `location_stock_id`, `location_id`, `product_id`, `lot_number`, `quality_status`, `quantity`와 감사 컬럼.
- `(location_id, product_id, lot_number, quality_status)`는 유니크다. 조회·대사용 인덱스로 `(product_id, lot_number, quality_status)`를 둔다.
- `quality_status`는 기존 `QualityStatus`(NORMAL/DEFECTIVE/DISPOSAL_SCHEDULED)와 같은 CHECK 제약을 쓴다(ADR-0013). `quantity >= 0`.
- Lot은 번호만 가진다. 유통기한·Lot 상태는 두지 않는다(inventory가 소유). 점유량, 이력, soft delete도 두지 않는다.
- 수량이 0이 된 행은 삭제하지 않고 유지한다(inventory의 0 수량 행 규칙과 같다). 같은 키의 재고가 다시 들어오면 그 행에 더한다.
  덕분에 `disposal_item`·`stock_audit_item`이 장부 행을 FK로 안전하게 참조한다.

### 2. 변경 경로는 둘뿐이다
- `increase(location, product, lotNumber, quality, quantity)`와 `decrease(...)`만 장부 수량을 바꾼다. 최종 목표는 같은 트랜잭션에서
  Location `usedCapacity`도 함께 갱신해, 서비스마다 흩어져 있던 사용량 갱신을 장부가 소유하는 것이다. 다만 미러링 기간(결정 5)에는
  기존 호출부가 사용량을 이미 갱신하므로 장부는 수량만 바꾸고, 각 흐름이 inventory 연동으로 전환될 때 사용량 갱신을 장부로 옮긴다.
- 불변식: 한 Location의 장부 수량 합계(모든 품질 상태)는 그 Location의 `usedCapacity`와 같다. 사용량 갱신을 장부가 소유하게 되는
  시점에 정합성 테스트로 고정한다.
- 품질 상태 전환은 `transferQuality(location, product, lotNumber, from, to, quantity)`로 한 행에서 빼 다른 행에 더한다. `usedCapacity`는
  변하지 않는다.

### 3. 품질 상태 전환을 장부가 따라간다
- inventory의 유통기한 스캔은 정상 수량 일부를 `DISPOSAL_SCHEDULED`로 옮긴다. 장부가 이를 반영하지 않으면 하루 1회 대사에서
  영구 불일치가 생긴다.
- 따라서 `LOT_EXPIRED` 수신(F-033) 시 WMS는 이벤트의 Lot 단위 수량을 Location별 장부 수량으로 배분해 `NORMAL → DISPOSAL_SCHEDULED`로
  옮기고 폐기 신청을 만든다. 배분 규칙은 F-033에서 정한다.
- 입고·반품의 불량 판정은 장부에 처음부터 `DEFECTIVE`로 들어간다.

### 4. 실사의 이동 판정은 `updated_at`으로 한다
- 실사 스냅샷은 장부 행(`location_stock_id`, 수량)으로 만든다. 스냅샷 이후 같은 행의 `updated_at`이 스냅샷 시각보다 늦으면 "미반영 이동
  있음"으로 표시한다.
- 품질 전환도 이동으로 잡히는 약한 과탐이 있지만 실사는 사람이 확인하고 이동 이력 테이블을 새로 두지 않는다. 필요해지면 추가한다.

### 5. 전환은 미러링으로 한다
> 흐름별 전환 방식(둘째 항목)은 [ADR-0023](0023-add-inventory-calls-before-cutover.md)이 대체했다. 미러링은 F-037 컷오버까지 유지한다.

- F-027에서 장부 테이블과 UseCase를 추가하고, 기존 재고 변경 지점(`register`, `adjust`, `confirmDisposal`, 출고 점유 확정, 품질 전환)이
  같은 트랜잭션에서 장부에도 반영한다. "장부 합계 = 기존 `inventory` 합계" 정합성 테스트를 둔다.
- F-032~F-036에서 흐름별로 미러링을 inventory 연동 + 장부 직접 갱신으로 바꾸고, F-037에서 `inventory`·`lot`·`allocation`·
  `inventory_history`와 미러링 코드를 제거한다. 이때 `disposal_item.inventory_id`·`stock_audit_item.inventory_id`는 장부 행 ID로 대체한다.
- 임시 코드이므로 미러링은 `inventory`의 출력 포트(`LocationStockMirror`)와 그 어댑터 한곳으로 모은다. 서비스는 포트만 호출하고
  어댑터가 `location_stock`의 UseCase를 부른다.

### 6. 조회 API
- WMS의 재고·Lot·이력 조회 API(`/api/inventories`, `/api/lots`, 이력, 출고 권고)는 `dozy-admin-console`이 inventory-api를 호출하도록
  바뀔 때까지 유지하고 F-037에서 일괄 제거한다. 장부 기반으로 남기는 조회는 Location·Zone 기준 현황(zone-summary)이다.

## 범위 밖
- `stock-quantities` API(F-029), 흐름별 inventory 연동(F-032~F-036), 기존 조회 API 제거(F-037).
- Lot 단위 수량을 Location별 장부로 배분하는 규칙(F-033).

## 결과 (Consequences)
- 얻는 것: 위치·적재·실사·대사 계산의 근거가 한 모델로 모이고, Location 사용량 갱신이 한 곳이 된다. 각 PR이 독립적으로 동작한다.
- 감수하는 것: 전환 기간 동안 같은 수량이 `inventory`와 장부에 이중으로 존재하고 미러링 코드가 임시로 늘어난다. 실사 이동 판정이
  이력 기반보다 거칠다. 장부에 유통기한이 없어 유통기한이 필요한 화면은 inventory 조회를 써야 한다.
