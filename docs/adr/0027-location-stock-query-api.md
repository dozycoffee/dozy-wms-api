# ADR-0027: Location별 재고 조회 API를 장부에서 읽기 전용으로 제공한다

## 상태
Accepted. ADR-0018 결정 6의 "장부 기반으로 남기는 조회는 Location·Zone 기준 현황(zone-summary)이다"를 **Location 단위 행 조회까지** 넓힌다.

## 배경 (Context)
재고 컷오버(ADR-0024, F-040)로 WMS의 `/api/inventories` 조회가 삭제됐다. 수량·Lot·이력의 진실은 inventory 서비스이고 inventory는 물리 위치를 모른다. 그 결과 "어느 Location에 어떤 상품·Lot이 몇 개 있는가"를 읽을 API가 어디에도 없다.

- `GET /api/locations/{id}`와 `GET /api/zones/{id}/locations`는 용량(`maxCapacity`, `usedCapacity`)만 준다.
- `zone-summary`는 Zone 단위 합계라 Location과 상품을 구분하지 못한다.
- 장부(`location_stock`)에는 Location×상품×Lot×품질 행이 있지만 읽는 REST 경로가 없다. 입고·폐기·실사·출고 서비스만 장부를 읽는다.

어드민 콘솔의 창고 관리 화면은 이 정보가 필요하다. Location별 재고 현황과, 폐기 등록이 `locationStockId`를 받으므로 폐기 대상(불량·폐기 예정 행)을 고르는 화면이 그렇다.

대안:

| 대안 | 평가 |
|---|---|
| inventory 서비스가 위치별 재고를 준다 | inventory는 위치를 모르고 알아서도 안 된다(ADR-0017). 기각 |
| 기존 Location 조회에 재고를 끼워 넣는다 | 마스터 조회가 장부 테이블에 의존하고 응답이 무거워진다. 기각 |
| **장부를 읽는 조회 전용 API를 별도로 둔다(채택)** | 데이터는 이미 있고 쓰기 경로는 건드리지 않는다 |

## 결정 (Decision)
1. `GET /api/location-stocks`를 추가한다. 읽기 전용이고 `WmsAuthorize.READ`다.
2. 응답은 `{"items":[...]}`이고 한 행은 장부 행(`locationStockId`, `productId`, `lotNumber`, `qualityStatus`, `quantity`, `heldQuantity`, `availableQuantity`, `updatedAt`)에 위치(`warehouseId`, `zoneId`, `zoneCode`, `locationId`, `locationCode`)를 붙인 것이다. 상품명·유통기한·Lot 상태는 WMS가 소유하지 않으므로 주지 않는다.
3. 조회 조건은 `warehouseIds`, `zoneId`, `locationId`, `productId`, `qualityStatus`, `includeEmpty`이고 모두 선택이다. 수량 0 행은 장부가 삭제하지 않고 유지하므로 `includeEmpty=true`일 때만 준다.
4. 접근 범위는 창고 접근 범위(`WarehouseAccess.narrow`)를 SQL의 창고 조건에 항상 적용한다. Zone·Location 조건을 줘도 접근 범위 밖의 행은 나오지 않는다.
5. 구현은 `zone-summary`와 같은 조회 전용 프로젝션이다. 장부 도메인 모델을 거치지 않고 `location_stock`과 `location`, `zone`을 조인해 결과를 바로 만든다.
6. 페이징은 두지 않는다. 창고 구성이 고정(Location 15개)이고 다른 목록 API도 페이징이 없다. 창고·Location이 늘어 행이 수천을 넘으면 그때 정한다.

## 결과 (Consequences)
- 콘솔은 상품명을 `GET /api/products`로 채운다(입고·폐기 화면과 같은 방식).
- 쓰기 경로와 스키마는 바뀌지 않는다. 기존 인덱스(`location_id` 선두 유니크 키, `(product_id, lot_number, quality_status)`)로 조회 조건을 받는다.
- 장부 행이 많아지면 응답이 커진다. 조건 없는 전체 조회는 접근 가능한 모든 창고의 행을 준다.
