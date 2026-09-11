# ADR-0004: 상태/분류 코드를 `common_code` 테이블로 중앙 관리

## 상태
Accepted

## 배경 (Context)
`zone_status`, `temperature_type`, `product_status`, `inbound_status` 등 상태·분류 코드를 쓰는 `VARCHAR`
컬럼이 도메인마다 여러 개 존재한다.

고려한 대안:
- **도메인마다 개별 코드 테이블 또는 DB ENUM 타입**: 도메인 경계가 명확하지만, 코드 그룹이 늘어날 때마다
  테이블/타입이 늘어나고 공통 조회(코드 그룹별 목록 조회 등) UI를 만들기 번거롭다.
- **애플리케이션 enum만 쓰고 DB는 자유 문자열**: DB 레벨에서 잘못된 값이 들어가는 걸 막지 못한다.

## 결정 (Decision)
`common_code` 테이블 하나에 모든 상태·분류 코드를 `{GROUP}_{VALUE}` 형식 문자열로 중앙 관리한다
(예: `TEMPERATURE_TYPE_AMBIENT`, `LOT_STATUS_NORMAL`). 각 도메인 컬럼은 이 테이블의 `code`를 참조하는
`VARCHAR(50)` FK로 두고, 애플리케이션 레벨에서는 Java enum으로 정의해 타입 안전성을 확보한다.

## 결과 (Consequences)
- 얻는 것: 코드 그룹이 늘어나도 스키마 변경 없이 `common_code`에 행만 추가하면 되고, 코드 목록 조회/관리
  기능을 도메인 무관하게 한 군데서 구현할 수 있다.
- 감수하는 것: FK가 여러 도메인 컬럼에서 같은 테이블을 가리키므로, `common_code` 테이블 자체의 가용성이
  전체 도메인에 영향을 준다. 애플리케이션 enum과 DB `code` 값이 어긋나면(오타, 미등록 코드) 런타임에야
  발견되므로, enum ↔ DB 코드 동기화를 검증하는 테스트가 필요하다.
- 감사 포인트: `docs/architecture-checklist.md`의 "common_code 참조 컬럼은 `{GROUP}_{VALUE}` 형식 enum
  매핑" 항목이 이 결정을 근거로 한다.
