# ADR-0008: 재고 부분 점유를 위한 Allocation 엔티티 도입

## 상태
Accepted

## 배경 (Context)

`Inventory`는 최초 설계 시 `qualityStatus`(NORMAL/DEFECTIVE/DISPOSAL_SCHEDULED) × `allocationStatus`
(AVAILABLE/ALLOCATED) 두 축의 이진 상태로 재고를 표현했다. 이 구조는 두 가지 실질적인 문제를 갖는다.

**1. 부분 점유를 표현할 수 없다.** `allocationStatus`가 로우 전체를 이진값으로 다루기 때문에, 같은
상품/Lot/Location 재고 100개 중 50개만 출고 건에 점유되는 상황(FIFO 피킹에서 사실상 항상 발생하는
케이스)을 표현할 방법이 없다.

**2. 점유 요청의 출처가 출고로 한정되지 않는다.** 재고를 일시적으로 점유해야 하는 요청은 출고뿐 아니라
재고실사 중 홀드 등 다른 도메인에서도 발생할 수 있다. 점유 로직을 특정 도메인(Outbound)에 종속된 필드로
설계하면 다른 점유 요청마다 유사한 로직을 중복 구현하게 된다.

**3. 향후 Inbound/Outbound 등은 Inventory와 하나의 DB 트랜잭션으로 묶이지 않고, 이벤트를 통해 비동기로
연동될 계획이다.** 이벤트 기반 연동은 최소 1회(at-least-once) 전달을 전제하므로, 재고 점유/해제 처리는
중복 이벤트 수신에도 멱등(idempotent)해야 하고, 실패한 이후속 단계에 대한 보상(release) 처리도 정확한
근거 데이터 없이는 안전하게 수행할 수 없다.

고려한 대안:

- **`Inventory`에 `allocatedQuantity: Int` 카운터만 추가**: 부분 점유 자체는 표현 가능해진다. 하지만
  이벤트가 중복 수신될 경우 카운터를 중복 증감하는 것을 막을 방법이 도메인 모델 안에 없고(별도의 "처리한
  이벤트 ID" 테이블이 필요), 특정 참조(예: 출고 건)가 취소될 때 "그 참조가 정확히 얼마를 점유했는지"를
  Inventory 스스로는 알 수 없어 서비스 레이어가 별도로 추적해야 한다.
- **`Inventory` : `Allocation` = 1 : N 구조로, 점유 요청 단위를 별도 엔티티로 기록**: 각 점유 요청이
  레코드로 남으므로 멱등성·추적성·부분 해제를 엔티티 차원에서 보장할 수 있다. 다만 엔티티/리포지토리가
  하나 늘고, 가용 수량 계산에 집계가 필요해진다.

## 결정 (Decision)

`Inventory` 엔티티 안의 `allocationStatus`(AVAILABLE/ALLOCATED) enum과 `allocate()`/`release()` 메서드,
`InvalidInventoryStatusCombinationException`을 전부 제거하고, 별도의 `Allocation` 엔티티를 도입한다.

### Allocation의 필드

- `inventoryId`: 점유 대상 `Inventory`
- `referenceType`(`AllocationReferenceType` enum, 현재는 `OUTBOUND`만 존재) + `referenceId`(`Long`):
  점유를 요청한 주체를 가리키는 범용 참조. `outboundItemId` 같은 특정 도메인 FK를 직접 두지 않음으로써,
  `inventory` 바운디드 컨텍스트가 `outbound` 등 다른 도메인의 스키마를 몰라도 되게 한다. `referenceType`은
  향후 다른 점유 요청 주체가 생기면 값만 추가하면 되는 additive-safe 확장 포인트다.
- `quantity`: 점유 수량. **생성 후 불변**이다. 수량이 바뀌어야 하면 기존 레코드를 `RELEASED`하고 새
  `quantity`로 새 레코드를 만든다 — 레코드 하나가 "확정된 청구 하나"를 의미하는 원장(ledger) 성격을
  유지하기 위함이다.
- `status`(`AllocationStatus`: `HELD`/`RELEASED`/`FULFILLED`)

### 상태 전이 규칙

`HELD → RELEASED`, `HELD → FULFILLED`만 허용한다. `RELEASED`/`FULFILLED`는 완전 종단 상태이며 이후
어떤 전이도 금지한다(`Lot`의 순방향 전용 전이와 동일한 스타일). `FULFILLED → RELEASED`(사후 취소)나
`RELEASED → HELD`(재점유)는 허용하지 않는다 — 종결된 레코드를 다시 여는 것은 원장으로서의 불변성을
깨뜨리므로, 그런 케이스가 필요해지면 새 `Allocation` 레코드를 만드는 것으로 표현한다.

### Inventory와의 관계

`Inventory`는 `allocatedQuantity: Int`(홀드 합계 캐시) 필드를 유지하고, `availableQuantity =
quantity - allocatedQuantity`를 파생 프로퍼티로 제공한다. `Allocation`의 상태 전이에 대응해 `Inventory`가
아래 도메인 메서드로 자기 불변식을 스스로 지킨다.

| Allocation 전이 | Inventory 메서드 | 효과 |
|---|---|---|
| (신규) → `HELD` | `hold(amount)` | `qualityStatus == NORMAL`이고 `availableQuantity >= amount`일 때만 성공, `allocatedQuantity` 증가 |
| `HELD` → `RELEASED` | `releaseHold(amount)` | `allocatedQuantity` 감소 |
| `HELD` → `FULFILLED` | `fulfillHold(amount)` | `quantity`와 `allocatedQuantity`를 함께 감소 — 물리적으로 재고가 빠져나간 시점이므로 총 수량 자체가 줄어든다 |

`markDefective()`/`markDisposalScheduled()`는 `allocatedQuantity > 0`이면(HELD가 하나라도 남아있으면)
예외로 차단한다. 호출자가 먼저 관련 `Allocation`을 release해야 품질 상태를 전환할 수 있다 — 점유된
재고가 품질 문제로 조용히 빠지는 것을 막기 위한 명시적 실패다.

### 멱등성을 위한 유니크 제약

`(inventoryId, referenceType, referenceId)` 조합에 대해 **`status = HELD`인 레코드에만** 조건부 유니크
제약을 둔다(전체 이력에는 걸지 않는다). 같은 참조가 보낸 "점유해줘" 이벤트가 중복 도착해도 두 번째
INSERT는 유니크 제약에 걸려 자연스럽게 무시되므로, 별도의 "처리한 이벤트 ID" 테이블 없이 `Allocation`
자체가 멱등성 경계 역할을 한다. `RELEASED`/`FULFILLED`로 종결된 뒤에는 같은 참조가 같은 재고 로우를
다시 점유하는 재시도/재요청 흐름을 막지 않기 위해 조건부(HELD 한정) 인덱스로 설계한다.

### 트랜잭션 경계

`Inventory`와 `Allocation`은 같은 `inventory` 바운디드 컨텍스트에 속하므로, 하나의 로컬 DB 트랜잭션
안에서 함께 갱신된다 — 이는 "도메인 간(Outbound ↔ Inventory) 트랜잭션을 이벤트로 분리한다"는 계획과
모순되지 않는다. 피해야 하는 것은 Outbound 쪽 쓰기와 Inventory 쪽 쓰기를 하나의 분산 트랜잭션으로 묶는
것이며, Outbound가 발행한 이벤트를 inventory 쪽 리스너가 수신해 **자신의 트랜잭션 안에서** `Allocation`
생성과 `Inventory.hold()`를 함께 처리하는 것은 정상적인 로컬 트랜잭션이다.

## 결과 (Consequences)

- 얻는 것:
  - 재고 로우 하나에 대해 여러 요청이 부분적으로 점유하는 상황을 정확히 표현할 수 있다.
  - 점유 요청 주체가 `referenceType`/`referenceId`로 일반화되어 있어, 출고 이외의 도메인이 재고를
    점유해야 하는 요구가 생겨도 `Inventory`/`Allocation`의 구조를 바꾸지 않고 `AllocationReferenceType`
    값만 추가하면 된다.
  - `(inventoryId, referenceType, referenceId)` 조건부 유니크 제약이 이벤트 중복 수신에 대한 멱등성을
    엔티티 차원에서 보장하고, 특정 참조가 취소될 때 "그 참조가 정확히 얼마를 점유했는지"를 `Allocation`
    레코드에서 그대로 조회할 수 있어 보상(release) 처리가 안전해진다.
- 감수하는 것:
  - 엔티티와 리포지토리가 하나 늘었고, 가용 수량을 구하려면 `Inventory` 단독 조회로 끝나지 않고
    `Allocation` 집계(또는 캐시 필드 동기화)가 필요하다 — 이번 결정으로 `Inventory.allocatedQuantity`를
    캐시로 채택했으므로, `Allocation` 상태 변경과 `Inventory.allocatedQuantity` 갱신이 항상 같은
    트랜잭션 안에서 함께 이뤄지도록 이후 서비스 레이어 구현 시 반드시 지켜야 한다 — 둘이 어긋나면 캐시가
    실제 홀드 합계와 불일치하는 버그가 된다.
  - `AllocationReferenceType`은 현재 `OUTBOUND` 하나만 존재한다 — 실제로 다른 참조 주체가 생기기 전까지는
    추측성 설계라는 한계가 있으나, 값 추가는 애그리거트 경계를 새로 정의하는 것보다 훨씬 저위험한 변경이다.
  - 이 ADR은 `Allocation`의 도메인 모델 규칙만 확정한다. `(inventoryId, referenceType, referenceId)`
    조건부 유니크 인덱스의 실제 DB 마이그레이션, 이벤트 발행/구독 인프라(아웃박스 패턴 등), Outbound
    도메인과의 실제 연동은 각 도메인이 구현되는 시점에 별도로 다룬다.
- 감사 포인트: `docs/business-rules-checklist.md`의 "재고 상태 조합 (Inventory)" 규칙이 이 결정으로
  대체되어 "재고 점유 (Allocation)" 규칙로 갱신된다. `inbound`/`outbound` 도메인을 구현할 때 이 ADR의
  트랜잭션 경계 원칙(로컬 트랜잭션 vs 이벤트 기반 연동)이 실제로 지켜졌는지 확인한다.
