# 비즈니스 규칙 체크리스트

## 목적과 사용법

이 문서는 진행 상황 추적용이 아니라 **구현 정확성 검증용**이다. `CLAUDE.md`의 "Business Rules"
섹션에 흩어진 규칙을 도메인별로 쪼개서, 각 규칙이 실제로 어디에 구현됐고 어떤 테스트로 커버되는지
추적한다.

- **갱신 시점**: 도메인 하나의 구현(모델/서비스/컨트롤러 등)이 끝날 때마다 관련 규칙 행을 채우거나
  상태를 갱신한다.
- **상태 값**: ✅ 검증됨 (코드+테스트로 확인됨) / ⚠️ 부분 구현 (코드는 있으나 테스트 없음, 또는 일부
  케이스만 처리) / ❌ 미구현
- `/code-review` 실행 시 이 문서를 참고 기준으로 사용한다 — 특히 "❌ 미구현"인데 실제로는 구현된
  항목을 잡아내는 용도로도 쓴다.
- 규칙의 배경/대안이 궁금하면 `docs/adr/`를 먼저 본다.

---

## 창고(Warehouse) / 재고 적재

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상품은 반드시 지정된 Zone에만 적재 — `product.category`가 Zone을 1:1로 결정(`default_zone_id` 컬럼 없음), 실제 배치 시점의 FK는 `inbound_item.zone_id` | [ProductCategory.kt](../src/main/kotlin/com/dozycoffee/wms/product/domain/enumeration/ProductCategory.kt) (category→zoneCode 매핑만 정의됨. inbound 도메인이 없어 실제 적재/용량 검증은 아직 미구현) | [ProductCategoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/product/domain/ProductCategoryTest.kt) | ⚠️ 부분 구현 |
| Location `usedCapacity`는 적재 즉시 갱신, `maxCapacity` 초과 불가 | [Location.kt:61-92](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/Location.kt#L61-L92) (`occupy`/`release` + `validateCapacityNotExceeded`) | [LocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/LocationTest.kt) | ✅ 검증됨 |
| WorkArea 점유량은 건별로 점유/반환하며, 전체 reset으로 다른 건의 점유량을 지우면 안 됨 | [WorkArea.kt:54-68](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/WorkArea.kt#L54-L68) (`occupy`/`release`가 amount만큼만 증감, 전체 초기화 메서드 없음) | [WorkAreaTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/WorkAreaTest.kt) | ✅ 검증됨 |
| 비활성(INACTIVE) Zone/WorkArea/Location은 점유 시도 시 거부 | [WorkArea.kt:76-80](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/WorkArea.kt#L76-L80), [Location.kt:82-86](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/Location.kt#L82-L86) | [WorkAreaTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/WorkAreaTest.kt), [LocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/LocationTest.kt) | ✅ 검증됨 |

## 입고 (Inbound)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 입고 예정 등록 시 Zone 잔여 capacity 사전 점검, 부족 시 반려 (경고 아님) (ADR-0003) | 미구현 (inbound 도메인 없음) | 없음 | ❌ 미구현 |
| 단일 Location에 모두 적재 불가 시 여러 Location으로 분산 배치 | 미구현 | 없음 | ❌ 미구현 |
| 입고 완료 후 해당 건이 점유했던 만큼만 WorkArea `usedCapacity`를 release (전체 reset 금지) | 미구현 — 단, WorkArea 쪽 `release(amount)` 기반은 준비됨 ([WorkArea.kt:67](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/WorkArea.kt#L67)) | 없음 | ❌ 미구현 |
| 상태 흐름 `EXPECTED → WAITING → PROCESSING → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |

## 출고 (Outbound)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 피킹은 FIFO — `lot.expiration_date` 기준 오름차순 선택 | 미구현 (outbound/inventory 도메인 없음) | 없음 | ❌ 미구현 |
| 상태 흐름 `REQUESTED → PICKING → INSPECTING → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |

## 반품 (ReturnRequest)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `RECEIVED → INSPECTING → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |

## 폐기 (Disposal)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `REQUESTED → APPROVED → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |

## 유통기한 모니터링 (배치 스캔)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 유통기한 30일 이내 → Lot 상태 `EXPIRING_SOON` 자동 전환 | [Lot.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Lot.kt) `markExpiringSoon()`은 준비됨 — 30일 기준 판정 후 호출하는 배치 스캔 자체는 미구현 | [LotTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/LotTest.kt) | ⚠️ 부분 구현 |
| 유통기한 당일 경과 → Inventory `qualityStatus = DISPOSAL_SCHEDULED` 자동 전환, 출고 할당 즉시 제외 | [Lot.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Lot.kt) `markExpired()`, [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `markDisposalScheduled()`는 준비됨 — 배치 스캔에서 두 엔티티를 연계 호출하는 흐름은 미구현 | [LotTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/LotTest.kt), [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ⚠️ 부분 구현 |

## 재고 실사 (별도 구현 예정, ERD 미포함)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `SCHEDULED → IN_PROGRESS → COMPLETED → CLOSED` 준수 | 미구현 | 없음 | ❌ 미구현 |
| 조정 결과는 `inventory_history.history_type = ADJUSTMENT`로 기록 | 미구현 | 없음 | ❌ 미구현 |

## 재고 점유 (Allocation, ADR-0008)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 정상(NORMAL) 품질이 아닌 재고는 점유(`hold`)할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `hold()` → `InventoryNotAllocatableException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| 가용 수량(`availableQuantity`)을 초과해 점유할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `hold()` → `InsufficientAvailableQuantityException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| 점유가 남아있는(`allocatedQuantity > 0`) 재고는 품질 상태(`markDefective`/`markDisposalScheduled`)를 변경할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) → `InventoryHasActiveAllocationException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| `Allocation` 상태 전이는 `HELD → RELEASED`, `HELD → FULFILLED`만 허용(완전 종단) | [Allocation.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Allocation.kt), [AllocationStatus.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/enumeration/AllocationStatus.kt) `canTransitionTo` | [AllocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationTest.kt), [AllocationStatusTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationStatusTest.kt) | ✅ 검증됨 |
| `Allocation.quantity`는 생성 후 불변 | [Allocation.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Allocation.kt) (변경 메서드 없음, `val`) | [AllocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationTest.kt) | ✅ 검증됨 |
| `FULFILLED` 전환 시 `Inventory.quantity`와 `allocatedQuantity`를 함께 차감 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `fulfillHold()` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| `(inventoryId, referenceType, referenceId)`는 `HELD` 상태에만 조건부 유니크(이벤트 재수신 멱등성) | 미구현 — 도메인 모델 규칙만 확정, R2DBC 마이그레이션/조건부 유니크 인덱스는 미구현 | 없음 | ❌ 미구현 |
