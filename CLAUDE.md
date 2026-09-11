# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

본 프로젝트는 커피 프랜차이즈 DOZY COFFEE를 위한 카페 원부자재 창고 관리 시스템(WMS) 백엔드 API 서버이다.

원두, 부자재, 포장재 등 카페 운영에 필요한 재고를 체계적으로 관리하며, 입고·출고·반품·폐기·재고 실사·유통기한 모니터링 등 창고 운영 전반을 지원한다.

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring WebFlux |
| DB Access | Spring Data R2DBC |
| Validation | Spring Validation |
| Build | Gradle |

## Common Commands

```bash
# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.dozycoffee.wms.domain.product.ProductServiceTest"

# Run a single test method
./gradlew test --tests "com.dozycoffee.wms.domain.product.ProductServiceTest.상품_등록_성공"
```

## Architecture

### Package Structure

```
src
├── main
│   ├── java/com/dozycoffee/wms
│   │   ├── global                            // 전역 설정 및 공통 모듈
│   │   │   ├── common                        // BaseEntity
│   │   │   ├── config                        // R2dbcConfig 등
│   │   │   └── error                         // ErrorCode, BusinessException, GlobalExceptionHandler
│   │   │
│   │   ├── warehouse                         // 창고(Warehouse) · 구역(Zone) · 작업구역(WorkArea) · 위치(Location)
│   │   ├── product                           // 상품 마스터
│   │   ├── inventory                         // 재고(Inventory) · Lot · 재고 이력(InventoryHistory)
│   │   ├── inbound                           // 입고(Inbound) · 입고 상품(InboundItem, 검수 포함)
│   │   ├── outbound                          // 출고(Outbound) · 출고 상품(OutboundItem)
│   │   ├── disposal                          // 폐기(Disposal) · 폐기 상품(DisposalItem)
│   │   └── return_request                    // 반품(ReturnRequest) · 반품 상품(ReturnItem)
│   │
│   │   # 각 도메인의 내부 구조
│   │   └── {domain}
│   │       ├── adapter
│   │       │   ├── in
│   │       │   │   ├── web                   // REST Controller, Request/Response DTO
│   │       │   │   └── event                 // 도메인 이벤트 수신 어댑터
│   │       │   └── out
│   │       │       └── persistence
│   │       │
│   │       ├── application
│   │       │   ├── port
│   │       │   │   ├── in                    // UseCase 인터페이스, Command, Result
│   │       │   │   └── out                   // Port 인터페이스
│   │       │   └── service                   // 애플리케이션 서비스 (UseCase 구현체)
│   │       │
│   │       └── domain
│   │           ├── model                     // 도메인 모델 (순수 POJO)
│   │           ├── enums                     // 도메인 열거형
│   │           ├── exception                 // 도메인 예외 및 에러 코드
│   │           ├── valueobject               // 값 객체
│   │           └── service                   // 도메인 서비스
│   │
│   └── resources
│       └── application.yaml                  // 환경 설정
│
└── test
    └── java/com/dozycoffee/wms
        ├── warehouse
        ├── product
        ├── inventory
        ├── ...
        └── */fixture                         // 테스트 픽스처 빌더
```

### Key Patterns

**Error Handling**

- `ErrorCode` (interface) → domain-specific enum implementing it (e.g., `ProductErrorCode`) → typed `BusinessException`
  subclasses (e.g., `ProductNotFoundException`)
- `@RestControllerAdvice` 기반 `GlobalExceptionHandler`가 `BusinessException` → `ErrorResponseDto { errorCode, message, timestamp }` 로 변환
- WebFilter/Security 레벨 예외가 필요한 경우 `WebExceptionHandler` 추가

**Entities**

- All entities extend `BaseEntity` (provides `createdAt`, `createdBy`, `updatedAt`, `updatedBy` via R2DBC Auditing — `@EnableR2dbcAuditing`)
- Soft delete 적용 테이블(product, inventory)은 `deletedAt`, `deletedBy` 컬럼 추가
- Entities use static `create()` factory methods that run validation
- Business logic and invariant enforcement lives on the entity
- R2DBC는 JPA cascade / orphanRemoval을 지원하지 않으므로 연관 엔티티는 별도 Repository를 통해 명시적으로 처리

**Common Code**

- `common_code` 테이블이 상태·분류 코드를 중앙 관리 — `zone_status`, `temperature_type`, `product_status`, `inbound_status` 등 `VARCHAR(50)` FK 컬럼들이 `common_code.code`를 참조
- 코드 형식: `{GROUP}_{VALUE}` (예: `TEMPERATURE_TYPE_AMBIENT`, `LOT_STATUS_NORMAL`)
- 애플리케이션 레벨에서는 Java enum으로 정의하고 DB에는 코드 문자열로 저장

**DTOs**

- Use Java `record` types for all request/response DTOs

**Inventory Status**

- `Inventory` 엔티티는 두 축의 상태를 가짐: `qualityStatus` (NORMAL / DEFECTIVE / DISPOSAL_SCHEDULED) × `allocationStatus` (AVAILABLE / ALLOCATED)
- 유효하지 않은 상태 조합은 생성 시점에 `IllegalArgumentException`으로 차단

**Warehouse Domain Structure**

- `Warehouse` → `Zone` (보관 구역) → `Location` (적재 위치)
- `WorkArea`: 입고/출고/반품/폐기 전용 작업 구역. Zone과 분리되어 `Warehouse`에 직속
- Zone code: `A`~`E`, D Zone만 `COLD`(냉장), 나머지는 `AMBIENT`(상온)
- Location code 형식: `A-01`
- `usedCapacity` / `maxCapacity` 컬럼으로 적재량 실시간 추적 (DB `CHECK` 제약 적용)
- WorkArea는 온도 구분 없는 공용 작업 공간으로 가정 — 냉장 상품도 작업 완료 후 즉시 D Zone으로 이동

**Warehouse Capacity Spec** (창고 구성 고정값 — 변경 없다고 가정)

```
WorkArea : 입고 처리장(50) / 출고장(50) / 반품 처리장(30) / 폐기 처리장(20)
A Zone   : 원두     (상온) — A-01(70), A-02(60), A-03(50)          = 180
B Zone   : 시럽     (상온) — B-01(60), B-02(60)                    = 120
C Zone   : 분말/파우더(상온) — C-01(50), C-02(50)                  = 100
D Zone   : 유제품   (냉장) — D-01(40), D-02(40)                    =  80
E Zone   : 컵/소모품/포장재(상온) — E-01(100), E-02(100), E-03(90), E-04(80) = 370
합계: 1,000
```

### Domain Status Flows

**Inbound**: `EXPECTED` → `WAITING` → `PROCESSING` → `COMPLETED`
- EXPECTED: 입고 예정 등록 완료
- WAITING: 창고 공간 점검 완료, 상품 도착 대기
- PROCESSING: 상품 도착, 입고 처리장 이동 후 검수 진행 중
- COMPLETED: 전체 적재 완료

**Outbound**: `REQUESTED` → `PICKING` → `INSPECTING` → `COMPLETED`

**ReturnRequest**: `RECEIVED` → `INSPECTING` → `COMPLETED`

**Disposal**: `REQUESTED` → `APPROVED` → `COMPLETED`

**Lot**: `NORMAL` → `EXPIRING_SOON` → `EXPIRED` (배치 스캔으로 자동 전환)

### Business Rules

**입고**
- 입고 예정 등록 시 Zone의 잔여 Capacity를 사전 점검 — 공간 부족 시 입고 반려 (경고가 아닌 반려)
- 단일 Location에 모두 적재 불가 시 여러 Location으로 분산 배치
- 입고 완료 후 해당 건이 점유했던 만큼 WorkArea의 `usedCapacity`를 release — WorkArea는 여러 입고 건이 동시에 점유할 수 있는 공유 자원이므로, 전체를 0으로 초기화(reset)하면 동시 처리 중인 다른 건의 점유량까지 지워지는 버그가 됨

**출고**
- 피킹은 **FIFO** (선입선출) 방식 — `lot.expiration_date` 기준 오름차순 선택

**재고 적재**
- 상품은 반드시 지정된 Zone(`product.default_zone_id` 또는 `inbound_item.zone_id`)에만 적재
- Location `usedCapacity`는 적재 즉시 갱신, `maxCapacity` 초과 불가

**유통기한 모니터링** (배치 스캔)
- 임박 기준: 유통기한 **30일 이내** → Lot 상태 `EXPIRING_SOON` 자동 전환
- 경과 재고: 유통기한 당일 경과 → Inventory `qualityStatus` = `DISPOSAL_SCHEDULED` 자동 전환, 출고 할당 즉시 제외

**재고 실사** (시나리오 존재, ERD 미포함 — 별도 구현 예정)
- 실사 상태: `SCHEDULED` → `IN_PROGRESS` → `COMPLETED` → `CLOSED`
- 조정 결과는 `inventory_history.history_type = ADJUSTMENT`로 기록

### Testing Strategy

Tests are split by layer with no overlap:

| Test class suffix | Annotation                            | Spring Context | Purpose                               |
|-------------------|---------------------------------------|----------------|---------------------------------------|
| `*EntityTest`     | none                                  | none           | Pure domain logic / entity invariants |
| `*ServiceTest`    | `@ExtendWith(MockitoExtension.class)` | none           | Service logic with mocked repository  |
| `*ControllerTest` | `@WebFluxTest`                        | Slice          | API contract (WebTestClient)          |
| `*RepositoryTest` | `@DataR2dbcTest`                      | Slice          | R2DBC queries against test DB         |

**Test fixtures** live in `fixture/` packages under each domain's test folder — use `XxxTestBuilder` for entity builders
and `XxxDtoBuilder` for DTO builders.

**Controller tests**: `@WebFluxTest`로 슬라이스 컨텍스트를 로드하고 `WebTestClient`로 검증한다.

## Git / PR Workflow

커밋을 만들거나 브랜치를 생성하거나 PR을 작성하기 전에 반드시 [docs/git-workflow.md](docs/git-workflow.md)를
읽고 그 규칙(Commit Format, Branch Strategy 등)을 따른다.

# Personal Preferences

## Language

* Always respond in Korean unless explicitly requested otherwise.
* Use English for Spring, R2DBC, WebFlux, and other commonly used technical terms.

## General Behavior

* Be concise and direct.
* Prioritize accuracy over agreement.
* Do not assume the existing implementation is optimal.
* Do not blindly follow existing patterns.
* Challenge existing designs when there is a better alternative.
* Explain trade-offs when recommending changes.
* Highlight risks, limitations, and future maintenance concerns.

## Code Comments

* Only write comments that are strictly necessary — constraints or intent the code itself cannot express.
* Never write temporal or plan-referencing comments (e.g., "Step 2에서 이동", "나중에 추가한다"). Migration plans belong in ADRs/PRs, not in code.
* Do not write comments that restate what the code does.

## Improvement Mindset

When analyzing code:

* Suggest improvements even if the code is currently working.
* Identify technical debt.
* Point out architectural weaknesses.
* Recommend industry-standard practices when beneficial.
* Explain why a proposed improvement is valuable.
* Distinguish between critical issues and optional improvements.

## Architecture Review

Review for:

* SRP violations
* Excessive responsibilities in Service classes
* God Objects
* Hidden coupling
* Tight coupling between layers
* Layer boundary violations
* Dependency direction issues
* Transaction boundary problems
* Scalability concerns

When appropriate:

* Suggest responsibility separation.
* Suggest better abstraction.
* Suggest more maintainable designs.
* Explain architectural trade-offs.

## Spring Boot Guidelines

* Prefer constructor injection.
* Avoid field injection.
* Avoid business logic in Controllers.
* Keep Controllers focused on request/response handling.
* Keep transaction boundaries in Service layer.
* Prefer explicit dependencies.
* Favor readability over unnecessary abstraction.

## R2DBC Review

Always review for:

* 불필요한 전체 엔티티 로딩 (필요한 컬럼만 조회)
* N+1에 준하는 반복 쿼리 (루프 내 개별 쿼리 → `IN` 절 또는 `flatMap` 병렬화로 개선)
* 트랜잭션 경계 누락 (`@Transactional` on reactive chain)
* blocking 코드 혼입 (R2DBC 체인 내 blocking I/O 금지)
* 잘못된 에러 전파 (`onErrorMap`, `onErrorResume` 누락)

When beneficial, suggest:

* DTO Projection (필요한 컬럼만 record로 매핑)
* `DatabaseClient`를 활용한 커스텀 쿼리
* `Flux.merge` / `Mono.zip`을 통한 병렬 쿼리
* `@Transactional(readOnly = true)` 활용

Do not suggest optimizations without explaining the expected benefit.

## Domain Modeling

Review domain models for:

* Rich domain behavior opportunities
* Excessive setter usage
* Anemic domain model tendencies
* Missing domain responsibilities
* Poor encapsulation

Prefer meaningful domain methods over exposing internal state.

## Code Quality

Always evaluate:

* Readability
* Maintainability
* Testability
* Extensibility
* Consistency
* Simplicity

If a simpler implementation exists:

* Explain why it is simpler.
* Explain potential trade-offs.

If a more robust implementation exists:

* Explain why it is more robust.
* Explain whether the additional complexity is justified.

## Naming Review

Review naming quality for:

* Classes
* Methods
* Variables
* DTOs
* Repositories
* Services

Suggest improvements when names do not clearly communicate intent.

## Testing Preferences

Prefer:

* Given-When-Then structure
* Readable tests
* Realistic test scenarios
* Focused test responsibilities

When generating tests:

* Explain the purpose of the test.
* Identify important edge cases.
* Suggest missing test scenarios.

## Refactoring Expectations

When suggesting refactoring:

1. Explain the current issue.
2. Explain the impact.
3. Explain alternative approaches.
4. Recommend the preferred approach.
5. Provide example code when useful.

Do not recommend refactoring solely for stylistic reasons.

## Learning Preference

Assume:

* Intermediate-to-advanced knowledge of Java
* Intermediate-to-advanced knowledge of Spring Boot
* Intermediate knowledge of R2DBC and reactive programming

Avoid explaining basic Java syntax.

For advanced topics:

* Explain underlying principles.
* Explain trade-offs.
* Explain practical usage.
* Explain common mistakes.

## Pull Request Review

Review as a senior backend engineer.

Do not only check correctness.

Also evaluate:

* Maintainability
* Scalability
* Hidden bugs
* Edge cases
* Naming quality
* Design quality
* Separation of concerns
* Future extensibility

Do not automatically approve code.

Provide actionable feedback.

## Project Pattern Analysis

Before generating new code:

* Analyze existing project patterns.
* Follow established conventions unless there is a strong reason not to.
* Explain when deviating from existing patterns.
* Avoid introducing unnecessary frameworks or architectural styles.

## Output Format

For code review:

1. Strengths
2. Issues
3. Risks
4. Recommendations

For architecture discussions:

1. Current approach
2. Pros
3. Cons
4. Recommended approach
5. Trade-offs

For implementation suggestions:

1. Problem
2. Cause
3. Solution
4. Example

Prefer bullet points over long paragraphs.
