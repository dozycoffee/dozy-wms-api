# ADR-0009: 재고 실사(Stock Audit) 도메인 설계

## 상태
Accepted

## 배경 (Context)

`CLAUDE.md`의 "재고 실사" 절과 `docs/business-rules-checklist.md`는 실사 시나리오(상태 흐름,
스냅샷 비교, 조정 임계치 승인, `inventory_history.history_type = ADJUSTMENT` 연계)를 문장으로만
정의해두고 있었고, ERD/테이블은 존재하지 않았다. 이 ADR은 구현에 착수하기 전에 꼭 필요한 구조적
결정 세 가지를 확정한다.

### 1. 실사 대상 범위(Scope)

실사 1건이 매핑되는 단위를 Zone/Location/Warehouse 중 어디로 잡을지 결정이 필요했다.

- **Warehouse 전체**: 가장 단순하지만 한 번에 수백 건의 Inventory가 묶여 부분 재실사가 어렵다.
- **Location 단위**: 가장 세밀하지만 대상 선택 UI/API가 복잡해지고, 실무에서도 "구역 담당자가 자기
  구역을 실사"하는 흐름과 맞지 않는다.
- **Zone 단위(채택)**: `Warehouse → Zone → Location` 계층에서 Zone은 이미 "구역 담당" 단위로
  쓰이고 있고(`AreaCode`/`ZoneCode` 등 기존 관례), 실무 실사도 보통 구역별로 진행된다.

### 2. 조정 임계치 관리 방식

"조정 수량이 임계치를 초과하면 상위 관리자 승인이 필요하다"는 규칙의 임계치 값을 어디에 둘지
결정이 필요했다.

- **DB 설정 테이블**: 운영 중 값을 바꿀 수 있지만, 이를 변경하는 관리자 UI/API가 이번 범위에
  없어 과설계다.
- **`application.yaml` 고정값(채택)**: 비밀값이 아닌 비즈니스 정책값이라 `CLAUDE.md`의 Secrets
  규칙과 무관하다. 값이 바뀌면 배포가 필요하지만, 지금 범위에서는 배포 없이 바꿀 요구가 없다.
  `wms.stock-audit.adjustment-approval-threshold`로 노출한다.

### 3. "단순 오차 vs 실제 재고 이상" 구분 방법

실사 기준 시점(스냅샷) 이후에 정상적인 입출고로 수량이 바뀐 경우와, 실제로 재고가 맞지 않는
경우를 구분해야 한다는 규칙이 있다. 두 가지를 섞으면 안 되는 지점이 있다.

- **실사 항목의 `discrepancy`(카운트값 - 스냅샷값)는 항상 스냅샷 기준으로 고정한다.** 이 값과
  별개로, 스냅샷 시각 이후 해당 `inventoryId`에 대한 `InventoryHistory`가 하나라도 있으면
  `StockAuditItem.hasUncommittedMovement = true`로 표시한다 — "차이가 나더라도 그 사이에 정상
  입출고가 있었다"는 신호를 실사자에게 남기기 위함이며, 자동으로 오차를 무시하지는 않는다(사람이
  최종 판단).
- **실제로 `inventory_history.ADJUSTMENT`에 기록하고 `Inventory.quantity`에 반영하는 조정량은
  스냅샷이 아니라 "확정(CLOSED) 시점의 실시간 `Inventory.quantity`"를 기준으로 다시 계산한다**
  (`counted - 실시간 quantity`). 스냅샷 기준으로 조정을 적용하면 스냅샷 이후에 발생한 정상
  입출고까지 덮어써서 재고가 깨진다. 이 재계산과 승인 임계치 비교는 `StockAudit`/`StockAuditItem`
  엔티티가 아니라 서비스 계층(구현 시점에 확정)의 책임이다 — 임계치 비교 대상은 이 최종 조정량이다.

## 결정 (Decision)

`stock_audit` 바운디드 컨텍스트를 Zone 단위 실사로 신설한다. 다른 도메인과 동일한 Hexagonal
패키지 구조(`adapter/in/out`, `application/port/service`, `domain/model/enumeration/exception`)를
따르고, Coroutines 스타일(ADR-0006)을 사용한다.

### 엔티티

- **`StockAudit`**(헤더): `stockAuditId`, `warehouseId`, `zoneId`, `status`, `assignee`(담당자,
  배정 전 null), `approvedBy`(임계치 초과 시 승인자, 필요 없으면 null)
- **`StockAuditItem`**(스냅샷+실사 결과): `stockAuditItemId`, `stockAuditId`, `inventoryId`,
  `snapshotQuantity`(등록 시점 `Inventory.quantity` 스냅샷, 불변), `countedQuantity`(실사자 입력,
  실사 전 null), `hasUncommittedMovement`(위 3번 참고)

`StockAudit` : `StockAuditItem` = 1 : N이며, `Inventory`/`Disposal` 등과 마찬가지로 R2DBC
cascade에 의존하지 않고 각자 리포지토리로 명시적으로 처리한다(ADR-0002).

### 상태 흐름

`SCHEDULED → IN_PROGRESS → COMPLETED → CLOSED`(완전 순방향, 역행/스킵 불가 — 다른 도메인과 동일한
`canTransitionTo` 스타일).

- `SCHEDULED`: 등록 시점. 대상 Zone의 모든 Location에 속한 Inventory를 조회해 `StockAuditItem`
  스냅샷을 생성한다(이 조회는 warehouse 도메인과의 조율이 필요해 서비스 계층 책임).
  - 실사 시점 Inventory `qualityStatus` 필터링 여부는 구현 시점에 결정한다. 이 ADR은 스냅샷
    생성 로직의 존재만 확정한다.
- `IN_PROGRESS`: 담당자 배정과 동시에 전환(`StockAudit.assign(assignee)`가 배정+전환을 한 번에
  수행 — 별도의 "배정" 상태를 두지 않는다).
- `COMPLETED`: 모든 `StockAuditItem`이 카운트를 마쳤는지 확인(서비스 계층 책임) 후 전환. 이 시점에
  각 항목의 `hasUncommittedMovement`를 계산해 표시한다.
- `CLOSED`: 조정 확정. `StockAudit.close(requiresApproval, approvedBy)`가 `requiresApproval`이
  참인데 `approvedBy`가 없으면 거부한다. `requiresApproval` 판단(항목별 최종 조정량이 임계치를
  넘는지)은 서비스 계층이 계산해 엔티티에 전달한다 — 엔티티는 "승인 없이 넘어갈 수 없다"는 불변식만
  지킨다.

### 이번 ADR이 확정하지 않는 것

- 서비스/영속성/REST 어댑터 계층의 구체 구현, DB 마이그레이션 스크립트, UseCase 인터페이스
  시그니처는 각 구현 PR에서 정한다.
- 실사 항목 대상에서 `qualityStatus != NORMAL` 재고(DEFECTIVE/DISPOSAL_SCHEDULED)를 포함할지는
  구현 시점에 결정한다.

## 결과 (Consequences)

- 얻는 것: Zone 단위로 범위를 좁혀 한 번에 다루는 Inventory 행 수를 통제할 수 있고, 기존 구역
  담당 관례와 자연스럽게 맞는다. 조정량을 실시간 `Inventory.quantity` 기준으로 재계산하도록 못박아
  둬서, 구현 단계에서 스냅샷을 그대로 덮어쓰는 실수를 ADR 차원에서 미리 차단한다.
- 감수하는 것: Zone 단위이므로 여러 Zone을 한 번에 실사하려면 실사 건을 여러 개 만들어야 한다.
  조정 임계치가 `application.yaml` 고정값이라 값 변경 시 배포가 필요하다 — 운영 중 빈번한 조정이
  필요해지면 DB 설정 테이블로 전환을 재검토한다.
- 감사 포인트: `docs/business-rules-checklist.md`의 "재고 실사" 절 각 규칙이 실제 구현 PR에서
  이 ADR의 결정(특히 조정량 재계산 기준)을 따랐는지 확인한다.
