# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway는 V24까지 적용. 개발용 목 데이터 시더(`devseed`)가 있다.
- 인증은 `CurrentAccessScopeProvider` + `MockAccessScopeProvider` 단계다. 소비 지점 교체와
  `warehouseIds` 교집합 검증까지 완료, 실제 JWKS 연동은 인증 서비스 준비 후(ADR-0011).
- 테스트는 Entity/Service/Controller와 `*PersistenceAdapterTest`(`@DataR2dbcTest`, 실 MySQL) 레이어가
  있다. FIFO 정렬·만료 스캔·Allocation HELD 유니크 등 SQL 의존 로직은 이미 이 레이어가 커버한다.

## 세션 로그

### 2026-10-01

- 테스트 DB를 개발 DB(`dozy_wms`)에서 Testcontainers MySQL로 분리했다(`MySqlTestContainerInitializer`).
- 창고 총 용량을 5,000으로 확대하고 F Zone(MD 상품)을 추가했다(`ZoneCode`/`AreaCode`/`ProductCategory.MD`, V24). 노션 시나리오 페이지를 현재 코드 기준으로 정정했다.
- F-020: 재고 실사 조정 시 Location `usedCapacity`가 갱신되지 않던 결함을 수정했다(`StockAuditService.close()`).
- F-021: 개발용 목 데이터 시더를 구현했다(`devseed` 패키지). `dev` 프로파일 + `wms.dev-seed.enabled=true`에서만 동작하고,
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

1. 개발 DB에 시드 적재(`SPRING_PROFILES_ACTIVE=dev WMS_DEV_SEED_ENABLED=true ./gradlew bootRun`) 후 UI/API로 확인
2. F-016: `MockAccessScopeProvider`의 빈 `warehouseIds` 의미 확인
3. F-014: 이벤트 전환 1단계 착수 여부 재검토 (원자성 상실, AFTER_COMMIT 유실 리스크)
4. F-015: 입고 검수 로직 보강
5. F-017~F-019: 선행 조건(인증 서비스, 서비스 분리, 실측 병목) 충족 시 착수
