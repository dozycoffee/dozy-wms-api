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
| 상품은 반드시 지정된 Zone에만 적재 — `product.category`가 Zone을 1:1로 결정(`default_zone_id` 컬럼 없음), 실제 배치 시점의 FK는 `inbound_item.zone_id` | [InboundService.register()](../src/main/kotlin/com/dozycoffee/wms/inbound/application/service/InboundService.kt) — `product.category.zoneCode` → `ZoneCode` 변환 후 `GetZoneUseCase.getByWarehouseIdAndZoneCode()`로 대상 Zone을 결정해 `InboundItem.zoneId`에 저장 | [InboundServiceTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/application/service/InboundServiceTest.kt) | ✅ 검증됨 |
| Location `usedCapacity`는 적재 즉시 갱신, `maxCapacity` 초과 불가 | [Location.kt:61-92](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/Location.kt#L61-L92) (`occupy`/`release` + `validateCapacityNotExceeded`) | [LocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/LocationTest.kt) | ✅ 검증됨 |
| WorkArea 점유량은 건별로 점유/반환하며, 전체 reset으로 다른 건의 점유량을 지우면 안 됨 | [WorkArea.kt:54-68](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/WorkArea.kt#L54-L68) (`occupy`/`release`가 amount만큼만 증감, 전체 초기화 메서드 없음) | [WorkAreaTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/WorkAreaTest.kt) | ✅ 검증됨 |
| 비활성(INACTIVE) Zone/WorkArea/Location은 점유 시도 시 거부 | [WorkArea.kt:76-80](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/WorkArea.kt#L76-L80), [Location.kt:82-86](../src/main/kotlin/com/dozycoffee/wms/warehouse/domain/model/Location.kt#L82-L86) | [WorkAreaTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/WorkAreaTest.kt), [LocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/warehouse/domain/LocationTest.kt) | ✅ 검증됨 |

## 입고 (Inbound)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 입고 예정 등록 시 Zone 잔여 capacity 사전 점검, 부족 시 반려 (경고 아님) (ADR-0003) | [InboundService.register()](../src/main/kotlin/com/dozycoffee/wms/inbound/application/service/InboundService.kt) — Zone별 요청 수량 합산 후 `GetLocationUseCase.getByZoneId()`로 잔여 capacity 합계와 비교, 부족 시 `InsufficientZoneCapacityException`으로 등록 자체를 반려. 통과 시 `EXPECTED`를 거치지 않고 바로 `WAITING`까지 전환 | [InboundServiceTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/application/service/InboundServiceTest.kt) | ✅ 검증됨 |
| 단일 Location에 모두 적재 불가 시 여러 Location으로 분산 배치 | [InboundService.distributeToLocations()](../src/main/kotlin/com/dozycoffee/wms/inbound/application/service/InboundService.kt) — `complete()` 시점에 Zone 내 Location을 잔여 capacity 내림차순으로 순회하는 First-Fit 방식으로 여러 `Inventory` 레코드를 생성 | [InboundServiceTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/application/service/InboundServiceTest.kt) | ✅ 검증됨 |
| 입고 완료 후 해당 건이 점유했던 만큼만 WorkArea `usedCapacity`를 release (전체 reset 금지) | [InboundService.startProcessing()](../src/main/kotlin/com/dozycoffee/wms/inbound/application/service/InboundService.kt)/`complete()` — 입고 상품 `expectedQuantity` 합계만큼 occupy/release, `WorkArea.release(amount)` 기반 그대로 사용 | [InboundServiceTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/application/service/InboundServiceTest.kt) | ✅ 검증됨 |
| 상태 흐름 `EXPECTED → WAITING → PROCESSING → COMPLETED` 준수 | [Inbound.kt](../src/main/kotlin/com/dozycoffee/wms/inbound/domain/model/Inbound.kt), [InboundStatus.kt](../src/main/kotlin/com/dozycoffee/wms/inbound/domain/enumeration/InboundStatus.kt) `canTransitionTo` | [InboundTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/domain/InboundTest.kt), [InboundStatusTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/domain/InboundStatusTest.kt) | ✅ 검증됨 |
| 입고 검수: 실제 입고 수량과 예정 수량을 비교하고 차이를 기록 | [InboundItem.kt](../src/main/kotlin/com/dozycoffee/wms/inbound/domain/model/InboundItem.kt) `inspect()`/`quantityDiscrepancy` | [InboundItemTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/domain/InboundItemTest.kt) | ✅ 검증됨 |
| 입고 검수: 파손 여부·유통기한·품질 상태를 확인해 정상/불량 상품을 구분 | [InboundItem.kt](../src/main/kotlin/com/dozycoffee/wms/inbound/domain/model/InboundItem.kt) `inspect()`가 `InspectionResult`(NORMAL/DEFECTIVE)를 기록 — 파손/유통기한/품질을 판단하는 검수 로직 자체는 서비스 계층(또는 검수자 입력)의 책임이고, InboundItem은 판정 결과만 보관 | [InboundItemTest.kt](../src/test/kotlin/com/dozycoffee/wms/inbound/domain/InboundItemTest.kt) | ⚠️ 부분 구현 |
| 불량 상품은 정상 재고로 등록하지 않고 반품 처리장 또는 폐기 처리장으로 이동 | 미구현 — `InspectionResult.DEFECTIVE`까지는 기록되나(위 항목), 반품/폐기 도메인이 없어 실제 이동 처리는 없음 | 없음 | ❌ 미구현 |

## 출고 (Outbound)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 피킹은 FIFO — `lot.expiration_date` 기준 오름차순 선택 | 미구현 — [OutboundItem.pick()](../src/main/kotlin/com/dozycoffee/wms/outbound/domain/model/OutboundItem.kt)에 피킹 결과(수량)를 기록하는 자리는 준비됨. Lot 오름차순 조회 및 여러 Lot 분할 피킹, `Allocation` 생성은 서비스 계층 구현 시점으로 범위 분리 | 없음 | ❌ 미구현 |
| 상태 흐름 `REQUESTED → PICKING → INSPECTING → COMPLETED` 준수 | [Outbound.kt](../src/main/kotlin/com/dozycoffee/wms/outbound/domain/model/Outbound.kt), [OutboundStatus.kt](../src/main/kotlin/com/dozycoffee/wms/outbound/domain/enumeration/OutboundStatus.kt) `canTransitionTo` | [OutboundTest.kt](../src/test/kotlin/com/dozycoffee/wms/outbound/domain/OutboundTest.kt), [OutboundStatusTest.kt](../src/test/kotlin/com/dozycoffee/wms/outbound/domain/OutboundStatusTest.kt) | ✅ 검증됨 |

## 반품 (ReturnRequest)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `RECEIVED → INSPECTING → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |

## 폐기 (Disposal)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `REQUESTED → APPROVED → COMPLETED` 준수 | 미구현 | 없음 | ❌ 미구현 |
| 품질 상태가 `DISPOSAL_SCHEDULED`인 재고를 폐기 처리장으로 물리 이동시키고, 폐기 처리장 `usedCapacity`를 갱신 | 미구현 | 없음 | ❌ 미구현 |
| 폐기 승인 시 사유(유통기한 경과/검수 불량/반품 불량 등)와 수량을 기록 | 미구현 | 없음 | ❌ 미구현 |
| 폐기 확정 시 대상 Inventory를 가용/총 수량에서 완전히 제외(soft delete)하고, 폐기 처리장 `usedCapacity`를 감소 | 미구현 | 없음 | ❌ 미구현 |

## 유통기한 모니터링 (배치 스캔)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 유통기한 30일 이내 → Lot 상태 `EXPIRING_SOON` 자동 전환 | [Lot.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Lot.kt) `markExpiringSoon()`은 준비됨 — 30일 기준 판정 후 호출하는 배치 스캔 자체는 미구현 | [LotTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/LotTest.kt) | ⚠️ 부분 구현 |
| 유통기한 당일 경과 → Inventory `qualityStatus = DISPOSAL_SCHEDULED` 자동 전환, 출고 할당 즉시 제외 | [Lot.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Lot.kt) `markExpired()`, [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `markDisposalScheduled()`는 준비됨 — 배치 스캔에서 두 엔티티를 연계 호출하는 흐름은 미구현 | [LotTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/LotTest.kt), [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ⚠️ 부분 구현 |
| 임박 재고는 FIFO 피킹 순서와 별개로 우선 출고를 권고하는 알림을 남긴다 (피킹 순서 자체를 강제로 바꾸지는 않음 — `lot.expiration_date` 오름차순 FIFO로 이미 우선순위가 반영되므로 알림은 보조 수단) | 미구현 | 없음 | ❌ 미구현 |

## 재고 조회

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| Location/상품/품질상태로 재고 목록을 필터링 조회 | [InventoryController.getAll()](../src/main/kotlin/com/dozycoffee/wms/inventory/adapter/in/web/InventoryController.kt), [InventoryService.getAll()](../src/main/kotlin/com/dozycoffee/wms/inventory/application/service/InventoryService.kt) | [InventoryControllerTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/adapter/in/web/InventoryControllerTest.kt), [InventoryPersistenceAdapterTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/adapter/out/persistence/InventoryPersistenceAdapterTest.kt) | ✅ 검증됨 |
| Zone 단위로 재고 현황(수량, Capacity 대비 사용률)을 조회 — Location→Zone 집계는 warehouse 도메인 스키마 조인이 필요해 별도 조회 모델로 다룰 예정 | 미구현 | 없음 | ❌ 미구현 |
| 재고 수량/유통기한/입고일 기준 정렬 조회 | 미구현 | 없음 | ❌ 미구현 |
| 재고 상세 조회 시 연결된 Lot 정보(제조일자/유통기한)와 최근 재고 이력을 함께 제공 — Lot은 `GetLotUseCase`로 별도 조회 가능하나 Inventory 응답에 합쳐서 내려주지는 않음 | [LotController.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/adapter/in/web/LotController.kt) | 없음 | ⚠️ 부분 구현 |
| Lot 단위 조회 시 Zone/Location별 재고 분포와 유통기한 임박 여부를 함께 제공 | 미구현 | 없음 | ❌ 미구현 |
| 재고 이력 조회 시 변동 유형(입고/출고/반품/폐기/조정)·기간으로 필터링, 시간순 정렬 | 미구현 | 없음 | ❌ 미구현 |

## 재고 실사 (별도 구현 예정, ERD 미포함 — Notion 시나리오는 상세화되었으나 ERD에 실사 테이블은 여전히 없음)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 상태 흐름 `SCHEDULED → IN_PROGRESS → COMPLETED → CLOSED` 준수 | 미구현 | 없음 | ❌ 미구현 |
| 조정 결과는 `inventory_history.history_type = ADJUSTMENT`로 기록 | 미구현 | 없음 | ❌ 미구현 |
| 실사 계획 등록 시 대상 Zone/Location의 Inventory 수량을 스냅샷으로 저장 — 이후 실사 결과는 실시간 재고가 아닌 이 스냅샷과 비교 | 미구현 | 없음 | ❌ 미구현 |
| 실사 담당자 배정 시점에 상태가 `SCHEDULED`→`IN_PROGRESS`로 전환 | 미구현 | 없음 | ❌ 미구현 |
| 실사 차이 확인 시 실사 기준 시점 이후 미반영된 입출고 이력이 있는지 구분 (단순 오차 vs 실제 재고 이상) | 미구현 | 없음 | ❌ 미구현 |
| 조정 수량이 임계치를 초과하면 상위 관리자 승인을 요구 | 미구현 | 없음 | ❌ 미구현 |

## 재고 점유 (Allocation, ADR-0008)

| 규칙 | 구현 위치 | 테스트 | 상태 |
|---|---|---|---|
| 정상(NORMAL) 품질이 아닌 재고는 점유(`hold`)할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `hold()` → `InventoryNotAllocatableException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| 가용 수량(`availableQuantity`)을 초과해 점유할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `hold()` → `InsufficientAvailableQuantityException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| 점유가 남아있는(`allocatedQuantity > 0`) 재고는 품질 상태(`markDefective`/`markDisposalScheduled`)를 변경할 수 없다 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) → `InventoryHasActiveAllocationException` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| `Allocation` 상태 전이는 `HELD → RELEASED`, `HELD → FULFILLED`만 허용(완전 종단) | [Allocation.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Allocation.kt), [AllocationStatus.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/enumeration/AllocationStatus.kt) `canTransitionTo` | [AllocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationTest.kt), [AllocationStatusTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationStatusTest.kt) | ✅ 검증됨 |
| `Allocation.quantity`는 생성 후 불변 | [Allocation.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Allocation.kt) (변경 메서드 없음, `val`) | [AllocationTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/AllocationTest.kt) | ✅ 검증됨 |
| `FULFILLED` 전환 시 `Inventory.quantity`와 `allocatedQuantity`를 함께 차감 | [Inventory.kt](../src/main/kotlin/com/dozycoffee/wms/inventory/domain/model/Inventory.kt) `fulfillHold()` | [InventoryTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/domain/InventoryTest.kt) | ✅ 검증됨 |
| `(inventoryId, referenceType, referenceId)`는 `HELD` 상태에만 조건부 유니크(이벤트 재수신 멱등성) | [V8__create_lot_inventory_allocation.sql](../src/main/resources/db/migration/V8__create_lot_inventory_allocation.sql) — MySQL이 partial unique index를 지원하지 않아 `idempotency_key` STORED 생성 컬럼으로 우회, [AllocationService.hold()](../src/main/kotlin/com/dozycoffee/wms/inventory/application/service/AllocationService.kt)가 위반 시 기존 HELD를 재조회해 멱등 응답 | [AllocationPersistenceAdapterTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/adapter/out/persistence/AllocationPersistenceAdapterTest.kt), [AllocationServiceTest.kt](../src/test/kotlin/com/dozycoffee/wms/inventory/application/service/AllocationServiceTest.kt) | ✅ 검증됨 |
