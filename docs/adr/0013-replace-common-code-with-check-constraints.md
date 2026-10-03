# ADR-0013: `common_code` 테이블을 컬럼별 CHECK 제약으로 대체

## 상태
Accepted (ADR-0004를 대체함)

## 배경 (Context)
ADR-0004는 상태·분류 코드를 `common_code` 테이블에 모으고 각 컬럼이 이를 FK로 참조하게 했다. 운영하면서
다음이 드러났다.

- 코드값은 Kotlin enum과 1:1로 고정돼 있고, 런타임에 코드를 추가·수정하는 기능이 없다.
- 애플리케이션이 `common_code`의 `name`/`sort_order`/`active`를 읽지 않는다 — 사실상 FK 대상으로만 쓰인다.
- enum과 `common_code` 행이 이중으로 존재해, 한쪽을 빠뜨리면 런타임 FK 위반으로 뒤늦게 발견된다.

고려한 대안:
- **`common_code` 유지**: 런타임 코드 관리·표시명 조회가 필요할 때 적합하지만, 현재 그런 요구가 없다.
- **MySQL `ENUM` 타입**: 값 추가 비용은 같고 이식성이 떨어진다.
- **컬럼별 `VARCHAR` + `CHECK`**: 허용 값을 DB에서 강제하면서 중간 계층이 없다.

## 결정 (Decision)
`common_code` 테이블과 `common_code` 패키지(도메인 모델·엔티티·Repository)를 제거하고, 참조하던 19개 컬럼은
FK 대신 `CHECK (col IN (...))` 제약(`ck_{table}_{column}`)으로 허용 값을 제한한다(V26). 저장 값은 기존
`{GROUP}_{VALUE}` 형식을 유지하므로 데이터 마이그레이션은 없고, `CommonCodes.toCode`/`fromCode`도 그대로 쓴다.

## 결과 (Consequences)
- 얻는 것: enum/DB 이중 관리 대상 중 시드 데이터와 관련 클래스가 사라지고, 쓰이지 않던 메타데이터
  (`name`/`sort_order`/`active`)가 정리된다.
- 감수하는 것: enum 상수를 추가·변경할 때 CHECK 제약을 `ALTER TABLE`로 바꿔야 한다(MySQL에서 테이블 복사가
  발생할 수 있다). DB를 직접 조회할 때 한글 표시명을 조인으로 볼 수 없다. CHECK는 MySQL 8.0.16 이상에서만
  강제된다.
- 후속: `{GROUP}_` 접두사는 FK 대상의 전역 유일성을 위한 것이었으므로 이제 불필요하다. 제거하려면 기존 컬럼
  값 UPDATE와 CHECK 재작성이 필요해 이번 변경과 분리했다. `work_area.area_code`는 기존에도 FK가 없어 CHECK를
  추가하지 않았다.
- 검증: `EnumCheckConstraintMigrationTest`가 각 CHECK의 허용 값이 enum 상수와 일치하는지, `common_code`
  테이블이 없는지 확인한다.
