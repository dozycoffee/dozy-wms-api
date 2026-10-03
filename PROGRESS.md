# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway 마이그레이션은 미배포 단계라 V1(테이블)·V2(외래키) baseline 두 파일로 통합했다. 개발용 목 데이터 시더(`devseed`)가 있다.
- 인증은 ADR-0012 기준으로 `Actor`(누구인가)와 `WarehouseAccess`(어느 창고)를 분리하는 단계다. 감사 주체와 창고
  범위는 타입으로 분리됐고, dozy-auth 스타터 연동(토큰 검증, role 인가)은 F-017로 진행 중이다.
- 테스트는 Entity/Service/Controller와 `*PersistenceAdapterTest`(`@DataR2dbcTest`, 실 MySQL) 레이어가
  있다. FIFO 정렬·만료 스캔·Allocation HELD 유니크 등 SQL 의존 로직은 이미 이 레이어가 커버한다.

## 세션 로그

### 2026-10-03

- F-025: 비동기 스타일을 Coroutines로 통일했다(ADR-0014). `warehouse` 4개 하위 도메인의 UseCase/Repository/Service/Persistence/Controller를
  `suspend`/`Flow`/`CoroutineCrudRepository`로 전환하고, inbound·outbound·return_request·disposal·stock_audit·warehouse_member·devseed의
  `awaitSingle()`/`collectList()` 브릿지를 제거했다. 테스트 `StepVerifier`/`.block()`은 `runTest`로 바꾸고 `reactor-test` 의존성을 뺐다.
  `DatabaseClient`는 Spring Kotlin 확장(`awaitRowsUpdated`/`awaitOne`/`flow`)으로 정리했다. Reactor는 `SecurityConfig` 컨버터, Reactor Context 접근, `ReactiveAuditorAware` 경계에만 남는다. 전체 898개 테스트 통과.
- F-024: `common_code` 테이블과 패키지를 제거하고 참조 FK 19개를 컬럼별 CHECK 제약으로 대체했다(ADR-0013, ADR-0004 대체). 미배포라 기존 V1~V25 마이그레이션을 V1(테이블·인덱스·CHECK)/V2(외래키) baseline으로 통합했다. 기존 DB는 `./scripts/reset-dev-db.sh`로 초기화해야 한다(Flyway 체크섬 불일치). FK 삭제 때 남던 불필요한 상태 컬럼 인덱스 19개는 baseline에서 제외했다. 아래 이전 로그의 V번호는 통합 전 기준이다.
  저장 값에서 `{GROUP}_` 접두사를 제거하고 enum 상수 이름(`name`)을 그대로 저장한다(`CommonCodes` 유틸과 `*_GROUP` 상수 삭제, `enum.name`/`Enum.valueOf`
  사용). `EnumCheckConstraintMigrationTest`가 CHECK 허용 값과 enum의 일치, 테이블 부재를 검증한다.

### 2026-10-02

- F-022: 사용자-창고 접근 매핑과 가드를 구현했다(ADR-0012 갱신). `warehouse_member`(V25)와 배정 관리 API(`/api/warehouses/{id}/members`,
  warehouse_admin 전용), `WarehouseMemberAccessProvider`(시스템·warehouse_admin=전체, 그 외=배정된 창고, 배정 없음=접근 불가)를 추가하고
  `AllWarehousesAccessProvider`를 제거했다. `WarehouseAccessGuard`로 입고·출고·반품·폐기·실사의 등록/단건/상태 변경/목록과 Item 조회·처리를
  제한한다(403 `COMMON_WAREHOUSE_ACCESS_DENIED`). 목록은 SQL `IN`으로 좁힌다.
- 버그 2건을 함께 고쳤다: (1) `IS NULL OR ... IN (:ids)` 패턴이 창고 2개 이상에서 SQL 오류를 내던 재고 조회, (2) 출고 FIFO 피킹이
  창고 조건 없이 재고를 골라 다른 창고 재고를 피킹할 수 있던 문제(`pickFifo`에 출고 창고 조건 추가).
- F-023(진행 중): WMS role 7개(`WmsRole`: inbound/outbound/return/disposal/stock_audit_manager, inventory_viewer, warehouse_admin)를
  정의하고 전 컨트롤러 엔드포인트에 `@PreAuthorize`를 적용했다(조회=모든 WMS role, 쓰기=도메인 담당 role + warehouse_admin,
  마스터·재고 쓰기=warehouse_admin). `local` 필터 체인은 모든 role을 가진 개발 사용자를 주입한다.
  `GlobalExceptionHandler`가 `AccessDeniedException`을 500으로 바꾸던 문제를 보안 예외 재전파로 고쳤다(403 Problem Details).
- 실사 마감의 `approvedBy` 요청 필드를 제거하고 토큰 principal로 대체했다. 임계치 초과 조정은 `warehouse_admin`만 승인할 수
  있으며, 아니면 `StockAuditApprovalRequiredException`이다. `devseed`는 local 개발 사용자로 승인한다.
- F-023 마무리: 실사 `assignee`를 요청 바디 대신 토큰 principal로 기록하도록 바꿨다(`PATCH /assign`은 바디 없음, 사용자가 아닌
  행위자는 `INVALID_ASSIGNEE`). role Auth 등록은 코드 완료 조건에서 빼고 배포 체크리스트로 분리했다(ADR-0012에 role 계약 명시).
  한 사용자는 role을 여러 개 가질 수 있음을 확인했다(`roles` 집합 + `hasAnyRole`).
- `stock_audit.register`에서 대상 Zone이 요청 창고 소속인지 검증한다(`STOCK_AUDIT_ZONE_WAREHOUSE_MISMATCH`, 400).
  다른 창고 Zone을 지정해 접근 가드를 우회하던 경로를 막았다.
- 코드 분석으로 dozy-auth 연동 모델을 정리했다(ADR-0012). 토큰에는 `principalId`와 `roles`만 있고 창고 범위가 없어,
  창고 접근은 WMS 자체 데이터(사용자-창고 매핑)로 관리하는 것으로 결정했다. 노션 "서비스 연동 가이드"는 워크스페이스에서
  찾지 못해 dozy-auth 저장소의 명세(`docs/`)를 기준으로 삼았다.
- F-016: `AccessScope`/`MockAccessScopeProvider`를 폐기하고 `Actor`/`CurrentActorProvider`,
  `WarehouseAccess`/`WarehouseFilter`/`CurrentWarehouseAccessProvider`로 교체했다. 감사 주체는 `principalId` UUID.
- F-017: dozy-auth 스타터(`0.1.0`)를 연동했다. 토큰 검증은 스타터 빈을 쓰고 필터 체인은 CORS 때문에 직접 정의했다(`SecurityConfig`).
  `SecurityContextActorProvider`가 Reactor Context에서 행위자를 꺼내며(요청 밖은 `SystemActor`), 통합 테스트로 `created_by`가
  토큰의 `principalId`로 기록됨을 확인했다(스파이크 통과). `local` 프로필만 인증을 우회하고 `prod`와 함께 켜면 기동 실패한다.
  시더 프로파일을 `dev`에서 `local`로 맞췄다. 컨트롤러 테스트 18개는 `@WithDozyPrincipal`로 전환했다.
- 남은 확인: CI가 `GITHUB_TOKEN`으로 `dozy-auth` 패키지를 읽을 수 있는지(패키지 설정에서 이 저장소의 읽기 접근 허용 필요)는 PR의 CI에서 확인한다.

### 2026-10-01

- 테스트 DB를 개발 DB(`dozy_wms`)에서 Testcontainers MySQL로 분리했다(`MySqlTestContainerInitializer`).
- 창고 총 용량을 5,000으로 확대하고 F Zone(MD 상품)을 추가했다(`ZoneCode`/`AreaCode`/`ProductCategory.MD`, V24). 노션 시나리오 페이지를 현재 코드 기준으로 정정했다.
- F-020: 재고 실사 조정 시 Location `usedCapacity`가 갱신되지 않던 결함을 수정했다(`StockAuditService.close()`).
- F-021: 개발용 목 데이터 시더를 구현했다(`devseed` 패키지). `local` 프로파일 + `wms.dev-seed.enabled=true`에서만 동작하고,
  마스터(창고 1, WorkArea 4, Zone 6, Location 15, 상품 20) → 기초 재고 → 입고/출고/반품 흐름 → 유통기한 스캔 → 폐기 → 실사를
  한 트랜잭션으로 적재한다. `DevSeedRunnerTest`가 Location/WorkArea 사용량·점유 수량 정합성과 상태 분포를 검증한다.
  개발 DB 초기화는 `scripts/reset-dev-db.sh`. 실제 개발 DB에는 아직 적재하지 않았다.

### 2026-09-30

- 구현 현황을 점검하고 `feature_list.json`, `PROGRESS.md`를 도입했다 (`chore/task-tracking-files`).
- F-013: 초기 진단("RepositoryTest 0개 = DB 검증 공백")이 틀렸음을 확인했다. 기존
  `*PersistenceAdapterTest` 26개가 실 DB로 SQL 로직을 검증 중이었다. 중복 테스트는 제거하고, 실제 공백이던
  V22/V23 인덱스 존재·컬럼 순서 검증(`InventoryIndexMigrationTest`)만 추가했다 (`test/repository-test-layer`).
- CLAUDE.md, architecture-checklist.md의 `*RepositoryTest` 표기를 실제 명칭 `*PersistenceAdapterTest`로 정정했다.

## 다음 세션에서 할 일

1. 개발 DB에 시드 적재(`SPRING_PROFILES_ACTIVE=local WMS_DEV_SEED_ENABLED=true ./gradlew bootRun`) 후 UI/API로 확인
2. 배포 체크리스트: dozy-auth admin에 `wms:` role 7개 등록 (코드 작업 아님)
3. (신규) 창고 배정 변경 시 요청마다 조회하는 비용 점검
4. F-014: 이벤트 전환 1단계 착수 여부 재검토 (원자성 상실, AFTER_COMMIT 유실 리스크)
5. F-015: 입고 검수 로직 보강
6. F-018~F-019: 선행 조건(서비스 분리, 실측 병목) 충족 시 착수
