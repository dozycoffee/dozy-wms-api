# PROGRESS

## 현재 상태

- 전체 도메인(warehouse, product, inventory, inbound, outbound, return_request, disposal,
  stock_audit)의 Hexagonal 3계층(도메인/서비스/영속성/REST)이 구현돼 있다. Flyway는 V23까지 적용.
- 인증은 `CurrentAccessScopeProvider` + `MockAccessScopeProvider` 단계다. 소비 지점 교체와
  `warehouseIds` 교집합 검증까지 완료, 실제 JWKS 연동은 인증 서비스 준비 후(ADR-0011).
- 테스트는 Entity/Service/Controller 레이어만 존재하고 `*RepositoryTest`(`@DataR2dbcTest`)가 0개다 —
  SQL 의존 로직이 실제 DB로 검증되지 않는 것이 현재 가장 큰 리스크.

## 세션 로그

### 2026-09-30

- 구현 현황을 점검하고 `feature_list.json`, `PROGRESS.md`를 도입했다 (`chore/task-tracking-files`).
- 다음 작업으로 `*RepositoryTest` 레이어 도입(F-013)을 별도 브랜치에서 진행한다.

## 다음 세션에서 할 일

1. F-013: `*RepositoryTest` 인프라 구성 및 FIFO 조회/만료 스캔/Allocation 조건부 유니크/인덱스 검증
2. F-016: `MockAccessScopeProvider`의 빈 `warehouseIds` 의미 확인
3. F-014: 이벤트 전환 1단계 착수 여부 재검토 (원자성 상실, AFTER_COMMIT 유실 리스크)
4. F-015: 입고 검수 로직 보강
5. F-017~F-019: 선행 조건(인증 서비스, 서비스 분리, 실측 병목) 충족 시 착수
