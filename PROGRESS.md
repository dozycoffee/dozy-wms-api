# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway는 V23까지 적용.
- 인증은 `CurrentAccessScopeProvider` + `MockAccessScopeProvider` 단계다. 소비 지점 교체와
  `warehouseIds` 교집합 검증까지 완료, 실제 JWKS 연동은 인증 서비스 준비 후(ADR-0011).
- 테스트는 Entity/Service/Controller와 `*PersistenceAdapterTest`(`@DataR2dbcTest`, 실 MySQL) 레이어가
  있다. FIFO 정렬·만료 스캔·Allocation HELD 유니크 등 SQL 의존 로직은 이미 이 레이어가 커버한다.
  (CLAUDE.md의 `*RepositoryTest` 명칭과 실제 `*PersistenceAdapterTest`가 불일치 — 문서 정정 필요)

## 세션 로그

### 2026-09-30

- 구현 현황을 점검하고 `feature_list.json`, `PROGRESS.md`를 도입했다 (`chore/task-tracking-files`).
- F-013: 초기 진단("RepositoryTest 0개 = DB 검증 공백")이 틀렸음을 확인했다. 기존
  `*PersistenceAdapterTest` 26개가 실 DB로 SQL 로직을 검증 중이었다. 중복 테스트는 제거하고, 실제 공백이던
  V22/V23 인덱스 존재·컬럼 순서 검증(`InventoryIndexMigrationTest`)만 추가했다 (`test/repository-test-layer`).

## 다음 세션에서 할 일

1. CLAUDE.md 테스트 전략 표의 `*RepositoryTest`를 실제 명칭(`*PersistenceAdapterTest`)으로 정정
2. F-016: `MockAccessScopeProvider`의 빈 `warehouseIds` 의미 확인
3. F-014: 이벤트 전환 1단계 착수 여부 재검토 (원자성 상실, AFTER_COMMIT 유실 리스크)
4. F-015: 입고 검수 로직 보강
5. F-017~F-019: 선행 조건(인증 서비스, 서비스 분리, 실측 병목) 충족 시 착수
