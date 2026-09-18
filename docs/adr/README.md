# ADR (Architecture Decision Record)

이 디렉터리는 프로젝트에서 내린 **되돌리기 번거로운 결정**과 그 이유를 기록한다.
`CLAUDE.md`가 "지금 규칙이 무엇인가"를 담는다면, ADR은 "왜 그 규칙이 됐는가·다른 대안은 무엇이었는가"를
담는다. `docs/architecture-checklist.md`, `docs/business-rules-checklist.md`를 감사(audit)할 때 "왜
이렇게 만들었는지"를 되짚어야 하면 여기를 먼저 본다.

## 언제 ADR을 쓰는가

- 여러 대안이 있었고, 그중 하나를 선택한 근거를 남겨야 나중에 재논의할 수 있는 결정
- 되돌리는 데 비용이 큰 결정 (아키텍처 스타일, 기술 스택, 데이터 모델의 근본 구조, 정책 방향)
- CLAUDE.md에 "규칙"으로만 적혀 있고 "왜"가 없는 항목을 발견했을 때 — 규칙을 옮기지 말고, 근거를
  ADR로 남긴 뒤 CLAUDE.md/체크리스트에서 링크

사소한 구현 선택(변수명, 특정 메서드의 내부 로직)은 ADR 대상이 아니다.

## 템플릿

```markdown
# ADR-XXXX: 제목

## 상태
Proposed | Accepted | Deprecated | Superseded by ADR-YYYY

## 배경 (Context)
어떤 문제/제약 때문에 결정이 필요했는가. 고려한 대안은 무엇이었는가.

## 결정 (Decision)
무엇을 선택했는가.

## 결과 (Consequences)
이 결정으로 얻는 것과, 감수해야 하는 트레이드오프.
```

## 목록

| ID | 제목 | 상태 |
|---|---|---|
| [0001](0001-hexagonal-ports-adapters-architecture.md) | 도메인별 Hexagonal(Ports & Adapters) 구조 채택 | Accepted |
| [0002](0002-reactive-webflux-r2dbc-stack.md) | WebFlux + R2DBC 논블로킹 스택 채택 | Accepted |
| [0003](0003-inbound-capacity-preflight-rejection.md) | 입고 시 Zone capacity 부족을 경고가 아닌 반려로 처리 | Accepted |
| [0004](0004-common-code-centralized-table.md) | 상태/분류 코드를 `common_code` 테이블로 중앙 관리 | Accepted |
| [0005](0005-kotlin-adoption-for-new-domains.md) | 신규 도메인부터 Kotlin 도입 | Superseded by 0007 |
| [0006](0006-kotlin-coroutines-for-new-domains.md) | 신규 Kotlin 도메인은 Coroutines 사용 | Accepted |
| [0007](0007-full-kotlin-migration.md) | 전체 코드베이스 Kotlin 마이그레이션 | Accepted |
| [0008](0008-inventory-allocation-entity.md) | 재고 부분 점유를 위한 Allocation 엔티티 도입 | Accepted |
| [0009](0009-stock-audit-domain-design.md) | 재고 실사(Stock Audit) 도메인 설계 | Accepted |

새 ADR은 `NNNN-kebab-case-제목.md` 형식으로 추가하고, 이 표에도 반드시 등록한다.
