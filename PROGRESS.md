# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway 마이그레이션은 미배포 단계라 V1(테이블)·V2(외래키) baseline 두 파일로 통합했다. 개발용 목 데이터 시더(`devseed`)가 있다.
- 인증은 ADR-0012 기준으로 `Actor`(누구인가)와 `WarehouseAccess`(어느 창고)를 분리하는 단계다. 감사 주체와 창고
  범위는 타입으로 분리됐고, dozy-auth 스타터 연동(토큰 검증, role 인가)은 F-017로 진행 중이다.
- 에러 응답은 `dozy-auth` 규약의 RFC 9457 Problem Details(`code`, `traceId`, `errors[]`)로 통일돼 있고 모든 응답에
  `X-Trace-Id`가 붙는다(ADR-0015, F-026). 소비자인 `dozy-admin-console`의 메시지 파싱 갱신이 남아 있다.
- 테스트는 Entity/Service/Controller와 `*PersistenceAdapterTest`(`@DataR2dbcTest`, 실 MySQL) 레이어가
  있다. FIFO 정렬·만료 스캔·Allocation HELD 유니크 등 SQL 의존 로직은 이미 이 레이어가 커버한다.

## 세션 로그

### 2026-10-04

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

## 다음 세션에서 할 일

1. 개발 DB에 시드 적재(`SPRING_PROFILES_ACTIVE=local WMS_DEV_SEED_ENABLED=true ./gradlew bootRun`) 후 UI/API로 확인
2. 배포 체크리스트: dozy-auth admin에 `wms:` role 7개 등록 (코드 작업 아님)
3. (신규) 창고 배정 변경 시 요청마다 조회하는 비용 점검
4. `dozy-admin-console`의 `httpClient`가 에러 메시지를 `detail`(없으면 `code` 기반 문구)로 읽도록 갱신 (F-026 후속, 콘솔 저장소 작업)
5. 유니크 제약 위반(`DataIntegrityViolationException`)을 409로 변환: 사전 중복 조회를 동시 요청이 통과하면 여전히 500이다. 변환할 `code`가 `dozy-auth` 에러 코드 표에 없어 규약 확정 후 진행
6. F-015: 입고 검수 로직 보강
7. F-019: 실측 병목 확인 시 착수
8. (최후순위) F-014 이벤트 전환 1단계 → F-018 아웃박스·브로커: 원자성 상실, AFTER_COMMIT 유실 리스크와 서비스 분리 구체화 후 재검토
