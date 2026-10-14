# ADR-0025: WMS role을 창고 관리자·admin·viewer·service로 합친다

## 상태
Accepted. ADR-0012의 role 계약 목록을 대체한다(인가 구조와 창고 접근 범위 결정은 그대로다).

## 배경 (Context)
ADR-0012는 업무별 role 7개(`inbound_manager`, `outbound_manager`, `return_manager`, `disposal_manager`, `stock_audit_manager`, `inventory_viewer`, `warehouse_admin`)와
서비스 간 호출용 `service`를 계약으로 삼았다. 이후 정해진 방향은 다음과 같다.

- 재고의 원본과 재고 조회는 inventory로 분리됐고(ADR-0017, ADR-0024), 사용자는 서비스별 audience의 role을 각각 받는다. 어드민 콘솔의 창고 관리 모드는 배정 창고의 일을 하는 창고 관리자 한 종류만 둔다.
- `dozy-auth`의 "role은 크게" 원칙에 따라 업무마다 role을 쪼개지 않는다. 입고·출고·반품·폐기·실사를 담당자별로 나눈다는 요구는 없다.
- `warehouse_admin`이라는 이름은 "모든 창고의 대시보드를 보는 사람"처럼 읽히지만, 실제로는 마스터(창고·Zone·작업구역·Location) 등록, 직원-창고 배정, 출고 취소와 임계치 초과 실사 조정의 승인, inventory 호출 아웃박스 운영 API를 맡는다. 전 창고를 한 화면에서 보는 WMS 화면은 없고(전사 재고 현황은 inventory의 재고 관리 모드가 맡는다), 이 role에 콘솔 모드를 만들지 않는다.
- 어드민 콘솔의 재고 관리 모드는 재고 화면에서 창고 이름을 보여줘야 한다. 이를 위해 업무 데이터는 보지 못하고 창고 목록·이름만 읽는 role이 필요하다.

## 결정 (Decision)
1. **role은 `warehouse_manager`, `viewer`, `admin`, `service` 넷이다** (audience `wms`, 토큰에서는 `wms:` prefix).
   - `warehouse_manager`: 배정된 창고의 입고·출고·반품·폐기·실사와 그 조회. 기존 5개 담당 role을 합친 것이다.
   - `viewer`: 창고 마스터(`GET /api/warehouses`, `GET /api/warehouses/{id}`)만 읽는다. 업무 데이터(입고·출고·상품·Zone 현황 등) 조회와 모든 쓰기는 403이다.
   - `admin`: 기존 `warehouse_admin`의 이름만 바꾼 것이다. 기존에 허용하던 API를 그대로 허용한다(마스터 등록, 직원 배정, 승인, 운영 API, 업무 API). 업무 API까지 허용을 유지한 것은 승인 API(실사 마감 등)가 업무 API와 같은 엔드포인트라서이며, `admin`의 업무 쓰기를 줄이는 일은 별도 결정으로 남긴다.
   - `service`: 그대로다.
2. **인가 식은 `READ`(업무 조회: `warehouse_manager`, `admin`)와 `MASTER_READ`(창고 마스터 조회: `warehouse_manager`, `viewer`, `admin`)로 나눈다.** 창고 컨트롤러의 조회 두 개만 `MASTER_READ`를 쓰고, Zone·작업구역·Location 조회를 포함한 나머지 조회는 `READ`다.
3. **창고 접근 범위**: `admin`과 `service`는 모든 창고(기존과 같다), `viewer`만 가진 사용자도 모든 창고를 본다(창고 이름을 모두 보여줘야 하고 마스터 조회 외에는 어떤 API도 열려 있지 않다). `viewer`와 `warehouse_manager`를 함께 가지면 배정된 창고로 제한한다(창고 관리자의 범위를 `viewer`가 넓히지 않는다).
4. **실사 승인자**는 `admin`이다(기존 `warehouse_admin`).

## 결과 (Consequences)
- 얻는 것: 콘솔에서 사용자에게 줄 role이 서비스당 한두 개로 줄고(창고 관리자는 `wms:warehouse_manager`와 `inventory:warehouse_manager`), `admin`이 콘솔 화면 없이 운영 권한이라는 점이 이름에서 드러난다. 다른 서비스 화면이 창고 이름을 읽기 위해 업무 데이터 권한을 받을 필요가 없다.
- 감수하는 것: 한 담당자가 입고만 하고 출고는 못 하게 하는 구분이 사라진다(필요해지면 role을 다시 나눈다). Auth에 등록한 기존 role(`wms:inbound_manager` 등)은 배포 전에 새 role로 교체해야 한다. 코드의 role code는 등록 후 바꿀 수 없다.
- 후속: `admin`의 일상 업무 쓰기 권한을 줄일지, 창고·Zone 마스터 관리와 직원 배정을 콘솔 화면으로 만들지는 필요할 때 따로 정한다.
