# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway 마이그레이션은 V1(테이블)·V2(외래키) baseline 이후 V3, V4가 추가돼 있다. 개발용 목 데이터 시더(`devseed`)가 있다.
- 재고(수량·Lot·예약·상품 마스터)는 `dozy-inventory-api`로 분리됐다. 이 저장소의 `inventory` 패키지는 제거 대상이며, 연동·제거 계획은 ADR-0017과
  F-027~F-037로 추적한다. 그 전까지는 기존 코드가 그대로 동작한다.
- 인증은 ADR-0012 기준으로 `Actor`(누구인가)와 `WarehouseAccess`(어느 창고)를 분리해 구현돼 있다. dozy-auth 스타터 연동(토큰 검증,
  F-017), 창고 접근 가드(F-022), role 인가와 `@PreAuthorize`(F-023)가 완료됐고 role의 Auth 등록만 배포 작업으로 남아 있다.
- 에러 응답은 `dozy-auth` 규약의 RFC 9457 Problem Details(`code`, `traceId`, `errors[]`)로 통일돼 있고 모든 응답에
  `X-Trace-Id`가 붙는다(ADR-0015, F-026). 소비자인 `dozy-admin-console`의 메시지 파싱 갱신이 남아 있다.
- 테스트는 Entity/Service/Controller와 `*PersistenceAdapterTest`(`@DataR2dbcTest`, 실 MySQL) 레이어가
  있다. FIFO 정렬·만료 스캔·Allocation HELD 유니크 등 SQL 의존 로직은 이미 이 레이어가 커버한다.

## 세션 로그

### 2026-10-13

- 재고 관련 코드(재고, Lot, 재고 이력, 점유, 상품 마스터)가 `dozy-inventory-api`로 분리·이관돼 있음을 확인했다. inventory 쪽 문서(`principles`,
  `scenarios`, `events`, ADR)와 WMS의 `inventory` 패키지(약 100개 파일)·이를 쓰는 inbound/outbound/return_request/disposal/stock_audit/devseed를
  대조해 이관·연동 범위를 정리했다.
- ADR-0017 작성: 수량·Lot·예약의 진실은 inventory, WMS는 Location 단위 물리 위치 보조 장부를 두고, 출고는 예약 이벤트 기반만 두며,
  반품은 등록된 Lot만 허용하고, inventory 호출은 로컬 커밋 + DB 아웃박스로 보낸다. 후속 작업을 F-027~F-037로 등록했다.
- 이전에 진행하던 반품 검수 수령 라인 전환(구 ADR-0017 초안, F-028 구상)은 폐기했다. 반품 Lot 규칙이 바뀌어 F-034에서 다시 설계한다.
  F-014·F-018(이벤트 전환·아웃박스)은 F-028(호출 아웃박스)·F-035(예약 이벤트 기반 출고)가 대체하므로 feature_list에서 삭제했다.
- F-027: Location 보조 장부(`location_stock`, V5)를 추가했다(ADR-0018). `(location, product, lot_number, quality_status)` 유니크, Lot 번호는
  대소문자를 구분하고 수량 0 행은 유지한다. `increase`/`decrease`/`transferQuality`가 행을 `FOR UPDATE`로 잠그고 갱신하며 첫 생성의 동시
  충돌은 409(`LOCATION_STOCK_DUPLICATE`)로 바꾼다. 기존 재고 변경 지점(`register`, `markDefective`, `markDisposalScheduled`,
  `confirmDisposal`, `adjust`, `fulfill`, 유통기한 스캔)이 `LocationStockMirror` 포트로 같은 트랜잭션에서 장부에 반영한다. 미러링 기간에는
  Location 사용량을 기존 호출부가 갱신하므로 장부는 수량만 바꾼다. `DevSeedRunnerTest`가 장부와 `inventory`의 키 단위 수량 일치,
  장부 합계와 Location 사용량 일치를 검증한다. 전체 986개 테스트 통과.
- F-028: inventory 호출 아웃박스(`inventory_call`, V6)를 추가했다(ADR-0019). 확정 트랜잭션이 `RecordInventoryCallUseCase`로 결정적 멱등 키
  (`wms-{종류}-{문서 ID}`)와 함께 호출을 기록하면, 전송기(스케줄러, 5초)가 `FOR UPDATE SKIP LOCKED`로 창고별 맨 앞 호출만 점유해 `svc-wms`
  system client로 보낸다. 5xx·408·429·연결 실패·시간 초과는 지수 백오프로 무한 재시도(10회부터 `INVENTORY_CALL_RETRYING` 로그), 그 외 4xx는 `DEAD`로
  격리하고 그 창고의 큐를 멈춘다. 운영자는 `/api/inventory-calls`(warehouse_admin)로 재전송·버림을 한다. 종류별 요청 DTO와 각 흐름의 전환은
  F-032~F-036에서 한다. 시스템 클라이언트 `WebClient`가 스타터 0.2.1에 있어 `dozyAuthVersion`을 0.1.0에서 올렸다. 설정은
  `INVENTORY_BASE_URL`, `AUTH_CLIENT_ENABLED/ID/SECRET`이다. 전체 테스트 통과.

### 2026-10-04

- 유니크 제약 위반이 500으로 나가던 문제를 고쳤다. Zone·WorkArea·Location 등록은 사전 조회가 없어 중복이면 항상 500이었고,
  Product·Lot·창고 배정은 동시 요청이 사전 조회를 함께 통과하면 500이었다. `translatingDuplicateKey`가 MySQL 1062(유니크 위반)만
  도메인 `Duplicate*Exception`(409)으로 바꾸고 FK·CHECK 위반은 그대로 둔다(실 MySQL로 판별 확인). `dozy-auth` 코드 표에 없는
  범용 코드 대신 도메인 접두사 코드를 쓰므로 규약 확정을 기다리지 않는다. 창고 배정(`assign`)은 idempotent라 순차 중복은 기존 배정을
  반환하지만 동시 충돌은 `WAREHOUSE_MEMBER_ALREADY_ASSIGNED` 409다(같은 트랜잭션의 재조회가 REPEATABLE READ 스냅샷에서 보이지 않음).
- 개발 DB를 `reset-dev-db.sh`로 초기화하고 시드를 재적재했다(V1~V4 적용, 입고 5건·수령 라인 6건 등). 서버는 적재 후 종료했다.
- F-015: 입고 검수를 수령 라인(`InboundReceipt`) 단위로 전환했다(ADR-0016). 한 입고 상품에 로트가 여러 개일 수 있고 같은 로트의
  정상/불량 라인을 나눠 기록할 수 있다(일부 파손). 검수 시점에 `Product.shelfLifeDays` 유무로 유통기한 필수/불가를 판단하고,
  유통기한이 지난 라인의 정상 판정·예정 수량 초과·같은 로트 번호의 유통기한 불일치를 거부한다. 불량 라인은 `DefectReason`이
  필수이며 폐기 연계 사유(`EXPIRED`/`INSPECTION_DEFECT`)로 매핑된다. 입고 예정에서 예정 로트·유통기한을 선택 입력할 수 있다.
  `complete()`는 `lotAssignments`를 받지 않고 수령 라인으로 Lot과 Inventory를 만든다. `V4__add_inbound_receipt.sql`로 스키마를 바꿨다.
  전체 테스트 통과.
- 유통기한이 NULL인 Lot이 재고 목록 `sortBy=EXPIRATION_DATE`에서 맨 앞에 나오던 문제를 별도 `fix`로 분리했다(#105). 출고 FIFO 피킹은
  Kotlin `nullsLast()`라 영향이 없었다.
- F-026: 에러 응답을 `dozy-auth`의 RFC 9457 Problem Details 규약(`code`, `traceId`, `errors[]`)에 맞췄다(ADR-0015).
  `ErrorResponseDto`를 제거하고 `GlobalExceptionHandler`가 WebFlux `ResponseEntityExceptionHandler`를 상속하도록 바꿨다.
  변경 전에는 깨진 JSON·405·415·매핑 없는 404가 모두 500 `COMMON_INTERNAL_SERVER_ERROR`로 응답됐음을 재현으로 확인했고, 이제
  각각 400/405/415/404다. 500은 내부 정보 없이 `INTERNAL_ERROR`만 싣는다. `TraceIdWebFilter`가 모든 응답에 `X-Trace-Id`를
  싣고 Security 체인보다 먼저 요청 헤더를 정규화해 스타터의 401/403 `traceId`와 일치시킨다.
  `CommonErrorCode`의 범용 코드를 `dozy-auth` 에러 코드 표 이름(`VALIDATION_FAILED`, `INTERNAL_ERROR`)으로 바꿨다.
  예외 클래스 작성 기준(필드 검증은 `InvalidDomainValueException`, 타입 구분이 필요한 규칙 위반은 전용 클래스)을 CLAUDE.md에 명시했다.
  전체 919개 테스트 통과(신규 21개: 필터 4, 핸들러 슬라이스 11, 통합 4, 보안 통합 2).
- `dozy-admin-console`이 에러 메시지를 `message`로 읽어(`httpClient.ts`) 새 형식(`detail`)에서는 서버 메시지 대신 기본 문구가
  표시된다. `code`는 `errorCode ?? code`로 이미 호환된다. 콘솔 쪽 갱신이 필요하다(WMS 저장소 밖 작업).
- 로그 아카이브: 2026-10-01 이전 세션 로그를 `docs/progress/archive.md`로 옮겼다.

### 2026-10-03

- F-025: 비동기 스타일을 Coroutines로 통일했다(ADR-0014). `warehouse` 4개 하위 도메인의 UseCase/Repository/Service/Persistence/Controller를
  `suspend`/`Flow`/`CoroutineCrudRepository`로 전환하고, inbound·outbound·return_request·disposal·stock_audit·warehouse_member·devseed의
  `awaitSingle()`/`collectList()` 브릿지를 제거했다. 테스트 `StepVerifier`/`.block()`은 `runTest`로 바꾸고 `reactor-test` 의존성을 뺐다.
  `DatabaseClient`는 Spring Kotlin 확장(`awaitRowsUpdated`/`awaitOne`/`flow`)으로 정리했다. Reactor는 `SecurityConfig` 컨버터, Reactor Context 접근, `ReactiveAuditorAware` 경계에만 남는다. 전체 898개 테스트 통과.
- F-024: `common_code` 테이블과 패키지를 제거하고 참조 FK 19개를 컬럼별 CHECK 제약으로 대체했다(ADR-0013, ADR-0004 대체). 미배포라 기존 V1~V25 마이그레이션을 V1(테이블·인덱스·CHECK)/V2(외래키) baseline으로 통합했다. 기존 DB는 `./scripts/reset-dev-db.sh`로 초기화해야 한다(Flyway 체크섬 불일치). FK 삭제 때 남던 불필요한 상태 컬럼 인덱스 19개는 baseline에서 제외했다. 아래 이전 로그의 V번호는 통합 전 기준이다.
  저장 값에서 `{GROUP}_` 접두사를 제거하고 enum 상수 이름(`name`)을 그대로 저장한다(`CommonCodes` 유틸과 `*_GROUP` 상수 삭제, `enum.name`/`Enum.valueOf`
  사용). `EnumCheckConstraintMigrationTest`가 CHECK 허용 값과 enum의 일치, 테이블 부재를 검증한다.


## 다음 세션에서 할 일

1. F-029 stock-quantities API(장부 완료로 착수 가능), F-030 WAREHOUSE_ACCESS_CHANGED 발행 — inventory가 이미 기다리는 계약
2. 이후 F-031(상품 사본) → F-032~F-036(입고·폐기·반품·출고·실사 연동) → F-037(재고 코드 제거·devseed 개편)
3. 배포 체크리스트: dozy-auth admin에 `wms:` role 7개 등록 (코드 작업 아님)
4. `dozy-admin-console`의 `httpClient`가 에러 메시지를 `detail`(없으면 `code` 기반 문구)로 읽도록 갱신 (F-026 후속, 콘솔 저장소 작업)
5. (후순위) 창고 배정 변경 시 요청마다 `warehouse_member`를 조회하는 비용 점검: 실측 후 필요할 때만 캐시 검토
6. (후순위) 입고 검수 후속: 입고 등록이 같은 상품의 중복 줄을 허용하는지 확인(ADR-0016 가정), 잔여 유통기한 비율 기준·수량 허용 오차·로트 자동 생성 등 범위 밖 항목 재검토
7. (후순위) F-019: 실측 병목 확인 시 착수
