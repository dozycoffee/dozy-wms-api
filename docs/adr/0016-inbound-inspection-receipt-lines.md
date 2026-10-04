# ADR-0016: 입고 검수를 수령 라인(Receipt) 단위로 전환하고 로트·수량·불량 사유 규칙을 서비스가 검증

## 상태
Accepted

## 배경 (Context)
입고 검수(`InboundItem.inspect(actualQuantity, result)`)는 실제 수량과 정상/불량 판정을 받아 그대로 저장할 뿐 판단하지 않는다
(`business-rules-checklist` ⚠️ 부분 구현). 구조상 다음 한계가 있다.

- **판정을 검증하지 못한다.** 검수 시점에는 로트·유통기한 정보가 없다. Lot 정보(`lotAssignments`)는 `complete()`에서 처음
  들어오므로 유통기한이 지난 상품이 NORMAL로 판정돼도 막을 수 없다.
- **한 상품 줄에 로트 하나만 연결된다.** 같은 상품이 로트 둘 이상으로 도착하는 상황을 표현할 수 없다.
- **일부 파손을 표현할 수 없다.** 한 줄이 전량 NORMAL 아니면 전량 DEFECTIVE다.
- **불량 사유가 없다.** 폐기 연계는 사유를 `INSPECTION_DEFECT` 하나로 고정한다.
- **수량 상한이 없다.** 입고 처리장 점유와 Zone 용량 사전 점검(ADR-0003)은 예정 수량 기준인데 실제 수량이 이를 넘을 수 있다.
- **같은 로트 번호의 유통기한 불일치를 무시한다.** `resolveLot()`은 `(productId, lotNumber)`로 기존 Lot을 찾으면 입력된 제조일자·
  유통기한을 버리고 재사용한다. FIFO 피킹과 만료 스캔이 `expiration_date` 기준이라 영향이 크다.

이 프로젝트는 실제 현업 데이터가 아니라 이상적인 실무 시나리오를 가정한다. 이상적인 실무에서는 공급사가 출하 정보(ASN)에
상품별 로트·유통기한·수량을 미리 보내고, 입고 시 박스 라벨의 실물 값과 대조해 차이를 기록하며, 판정과 사유는 로트(수령
라인) 단위로 남긴다. 로트 번호는 제조사가 부여하는 값이고, 모든 상품이 로트 번호를 가진다. 유통기한이 필요 없는 상품(빨대 등)이
있을 뿐이다. WMS가 로트 번호를 만들어내지 않는다.

고려한 대안:
- **현행 유지 + 검수 시점에 로트 정보 추가(단일 로트)**: 변경은 작지만 한 줄 한 로트 한계와 일부 파손 문제가 남고, 이후
  수령 라인으로 옮길 때 같은 코드를 두 번 고친다.
- **`complete()` 시점에 유통기한 검증**: 판정이 끝난 뒤라 NORMAL로 판정된 줄을 완료 단계에서 거부하게 되어 흐름이 어색하다.
- **수령 라인 모델 + 검수 시점 검증**: 채택. 마이그레이션이 V1/V2 baseline 단계(미배포)라 스키마 변경 비용이 낮다.

## 결정 (Decision)

### 1. 검수는 수령 라인 단위다
- `InboundReceipt`(수령 라인)를 도입한다. 입고 상품(`InboundItem`) 1 : 수령 라인 N이다.
- 수령 라인은 `lotNumber`, `manufactureDate`, `expirationDate`, `quantity`(1 이상), `inspectionResult`(NORMAL/DEFECTIVE),
  `defectReason`을 가진다.
- `InboundItem`의 `actualQuantity`는 수령 라인 수량의 합이다. 검수 전/후 구분은 `InspectionStatus`(PENDING/INSPECTED)로 하고,
  정상/불량 판정(`InspectionResult`)은 수령 라인으로 옮긴다.
- `inspect()`는 한 번만 호출할 수 있고(`InboundItemAlreadyInspectedException` 유지) 수령 라인 목록 전체를 한 번에 받는다.
- 미도착(수령 0)은 수령 라인 0건으로 표현한다. 수량 0인 라인은 만들지 않는다.

### 2. 로트 정보는 입고 예정에서 선택 입력, 검수에서 확정
- 입고 예정 등록은 상품 줄마다 예정 로트 번호·유통기한을 **선택**으로 받는다. 공급사가 ASN에 싣지 못하는 경우를 허용하기 위해서다.
- 검수에서는 수령 라인의 실제 로트 번호가 **필수**다. 예정 값과 실제 값이 다르면 입고를 거부하지 않는다. 실물이 진실이므로
  실제 값을 저장하고 예정 값은 참고로 함께 노출한다.
- `complete()`는 `lotAssignments`를 받지 않는다. Lot 검증은 검수 시점에, Lot 생성(`resolveLot`)과 Inventory 등록은 완료
  시점에 수령 라인 기준으로 수행한다.

### 3. 유통기한 규칙
- 유통기한 필요 여부는 `Product.shelfLifeDays`로 판단한다. 값이 있으면 `expirationDate`가 필수이고, 없으면 `expirationDate`를
  입력할 수 없다(거부). 이 판단은 서비스가 상품을 조회해 수행한다.
- `manufactureDate`는 선택이며 미래일 수 없고, `expirationDate`와 모두 있으면 `manufactureDate <= expirationDate`여야 한다.
- 유통기한이 오늘 이전인 수령 라인은 NORMAL로 판정할 수 없다(DEFECTIVE만 허용). 임박(30일 이내)은 막지 않는다. 임박 처리는
  기존 배치 스캔 규칙(Lot `EXPIRING_SOON`)에 맡긴다.
- 같은 `(productId, lotNumber)`가 이미 있는데 유통기한이 다르면 입고를 거부한다. 같은 검수 요청 안에서 같은 로트 번호의
  수령 라인끼리도 유통기한이 같아야 한다. 같은 로트의 NORMAL 라인과 DEFECTIVE 라인은 허용한다(일부 파손).
- `shelfLifeDays`와 제조일자로 계산한 유통기한을 입력값과 대조하는 검증은 채택하지 않는다(공급사별 기산 규칙이 달라 지나치게
  경직된다).

### 4. 수량 규칙
- 수령 라인 수량 합이 예정 수량을 **초과하면 거부**한다(`InboundOverReceivedException`). 입고 처리장 점유와 Zone 용량 점검이
  예정 수량 기준이므로 초과 입고는 용량 정합성을 깨기 때문이다.
- 부족 수령은 허용하고 `quantityDiscrepancy`로 차이를 기록한다. 허용 오차(%) 설정은 두지 않는다.

### 5. 불량 사유
- DEFECTIVE 라인은 `defectReason`이 필수이고 NORMAL 라인은 가질 수 없다.
- `DefectReason`: `DAMAGED`(파손), `EXPIRED`(유통기한 경과), `QUALITY`(품질 불량), `OTHER`. 컬럼은 `VARCHAR(50)` + CHECK 제약으로
  제한한다(ADR-0013).
- 폐기 연계 시 `EXPIRED`는 `DisposalReason.EXPIRED`로, 그 외는 `DisposalReason.INSPECTION_DEFECT`로 매핑한다. 유통기한이 지난
  라인이 NORMAL 불가이므로 `EXPIRED`는 DEFECTIVE 라인에서만 의미가 있다.

### 6. 완료 처리
- `complete()`는 수령 라인마다 Lot을 확정하고 Zone 내 Location에 분산 배치해 Inventory를 등록한다. DEFECTIVE 라인은 등록 직후
  DEFECTIVE로 전환하고 Disposal(REQUESTED)로 연계한다(기존 흐름 유지, 사유만 라인 기준).
- 입고 처리장 release는 기존대로 해당 입고 건의 예정 수량 합이다. 수령 수량은 예정 이하이므로 점유 해제량이 어긋나지 않는다.

### 7. 스키마
- baseline(V1/V2) 이후 마이그레이션(V3)이 이미 존재하므로 V1을 고치지 않고 새 마이그레이션(V4)으로 변경한다. `inbound_receipt`
  테이블 추가, `inbound_item`의 `inspection_result`를 `inspection_status`로 교체, 예정 로트 컬럼 추가, 불량 사유 CHECK 추가.
  기존 `inspection_result`가 PENDING이 아닌 입고 상품은 `INSPECTED`로 옮기지만 수령 라인은 복원하지 않는다(로트·판정 원본이
  `complete()` 이전에는 저장되지 않았다). 검수 후 완료 전 상태의 데이터가 있는 DB는 `./scripts/reset-dev-db.sh`로 초기화한다.

## 범위 밖
- WMS 자체 로트 번호 생성과 로트 추적 여부 플래그(전 상품이 로트 번호를 갖는다고 가정).
- 잔여 유통기한 비율 기준 입고 거부(예: 잔여 수명 2/3 미만 거부).
- 수량 허용 오차(%)와 예정 로트의 다중 지정(ASN의 한 줄이 로트 여럿인 경우는 같은 상품을 여러 줄로 등록해 표현한다).
- 유통기한 `NULL` 로트의 FIFO·정렬 동작 보완은 입고 검수와 논리적으로 다른 변경이므로 별도 `fix`로 다룬다. 현재
  `ORDER BY expiration_date ASC`는 MySQL에서 NULL을 맨 앞에 두어 유통기한 없는 상품이 먼저 선택된다.

## 결과 (Consequences)
- 얻는 것: 검수 시점의 판정 검증(만료 NORMAL 거부, 수량 초과 거부, 로트 유통기한 충돌 거부), 한 상품의 다중 로트와 일부 파손
  표현, 불량 사유 추적, 예정(ASN)과 실물의 대조 정보, 입고 처리장·Zone 용량 정합성 보호.
- 감수하는 것: 검수·완료 API 계약이 바뀐다(`inspect` 요청이 수령 라인 목록, `complete`에서 `lotAssignments` 제거). 검수·완료·
  `devseed`·기존 테스트를 함께 고쳐야 하고, 개발 DB를 초기화해야 한다.
- 유통기한 필요 여부를 `shelfLifeDays`의 유무로 판단하므로 두 의미가 한 필드에 묶인다. 프로젝트 규모에서는 허용하되, 의미가
  갈라지면(소비기한과 품질유지기한 구분 등) 별도 속성으로 분리한다.
- 검수 시점의 Lot 충돌 검사와 완료 시점의 Lot 생성 사이에 같은 로트가 다른 입고 건으로 먼저 등록되면 `complete()`에서 다시
  충돌할 수 있다. 완료 시점의 `resolveLot`도 같은 규칙으로 거부해 정합성을 지킨다.
