# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

본 프로젝트는 커피 프랜차이즈 DOZY COFFEE를 위한 카페 원부자재 창고 관리 시스템(WMS) 백엔드 API 서버이다.

원두, 부자재, 포장재 등 카페 운영에 필요한 재고를 체계적으로 관리하며, 입고·출고·반품·폐기·재고 실사·유통기한 모니터링 등 창고 운영 전반을 지원한다.

| 분류 | 기술 |
|------|------|
| Language | Kotlin ([ADR-0007](docs/adr/0007-full-kotlin-migration.md) — 전체 코드베이스 Kotlin 마이그레이션 완료, ADR-0005 대체) |
| Framework | Spring Boot 4.1.1 |
| Web | Spring WebFlux |
| DB Access | Spring Data R2DBC |
| Reactive Style | Warehouse/common_code/global: Reactor `Mono`/`Flux` (ADR-0007 범위 밖, 유지) / Product 이후 신규 도메인: Kotlin Coroutines `suspend`/`Flow` ([ADR-0006](docs/adr/0006-kotlin-coroutines-for-new-domains.md)) |
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
│   ├── kotlin/com/dozycoffee/wms
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
│   │           ├── model                     // 도메인 모델 (순수 Kotlin 클래스)
│   │           ├── enums                     // 도메인 열거형
│   │           ├── exception                 // 도메인 예외 및 에러 코드
│   │           ├── valueobject               // 값 객체
│   │           └── service                   // 도메인 서비스
│   │
│   └── resources
│       └── application.yaml                  // 환경 설정
│
└── test
    └── kotlin/com/dozycoffee/wms
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
- 애플리케이션 레벨에서는 Kotlin enum으로 정의하고 DB에는 코드 문자열로 저장

**DTOs**

- Use Kotlin `data class` types for all request/response DTOs

**Inventory Status & Allocation** (ADR-0008)

- `Inventory` 엔티티는 `qualityStatus` (NORMAL / DEFECTIVE / DISPOSAL_SCHEDULED) × `quantity`/`allocatedQuantity`(파생 `availableQuantity`)로 재고량을 관리한다 — 이진 `allocationStatus` 필드는 두지 않는다(재고 로우 하나에 대한 부분 점유를 표현할 수 없어 ADR-0008로 제거)
- 재고 점유는 별도의 `Allocation` 엔티티(`inventory` 도메인 소속)가 `Inventory` : `Allocation` = 1 : N으로 표현한다 — `referenceType`/`referenceId`로 점유 요청 주체를 범용적으로 참조하며 출고에 한정되지 않는다
- `Allocation` 상태는 `HELD → RELEASED`, `HELD → FULFILLED`만 허용하는 완전 종단 전이이며, `quantity`는 생성 후 불변이다
- `Inventory.hold()`/`releaseHold()`/`fulfillHold()`가 `Allocation`의 상태 전이에 대응해 `allocatedQuantity`(및 `fulfillHold`의 경우 `quantity`)를 갱신한다 — `qualityStatus != NORMAL`이면 `hold()` 불가, `allocatedQuantity > 0`이면 `markDefective()`/`markDisposalScheduled()` 불가
- `(inventoryId, referenceType, referenceId)`는 `status = HELD`인 레코드에만 조건부 유니크 — 이벤트 기반(비동기) 처리에서의 멱등성 보장 목적, 자세한 배경은 ADR-0008 참고

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

| Test class suffix | Annotation                          | Spring Context | Purpose                               |
|-------------------|-------------------------------------|----------------|---------------------------------------|
| `*EntityTest`     | none                                | none           | Pure domain logic / entity invariants |
| `*ServiceTest`    | `@ExtendWith(MockitoExtension::class)` | none        | Service logic with mocked repository  |
| `*ControllerTest` | `@WebFluxTest`                      | Slice          | API contract (WebTestClient)          |
| `*RepositoryTest` | `@DataR2dbcTest`                    | Slice          | R2DBC queries against test DB         |

**Test fixtures** live in `fixture/` packages under each domain's test folder — use `XxxTestBuilder` for entity builders
and `XxxDtoBuilder` for DTO builders.

**Controller tests**: `@WebFluxTest`로 슬라이스 컨텍스트를 로드하고 `WebTestClient`로 검증한다.

## Kotlin Coding Conventions

전체 코드베이스가 Kotlin이다 — 근거는
[ADR-0007](docs/adr/0007-full-kotlin-migration.md) 참고 (이전에는 Product부터의 신규 도메인만
Kotlin이고 Warehouse/common_code/global은 Java로 유지하는 혼용 방식이었으나, ADR-0005를 대체하고 전체
마이그레이션했다). 아래 컨벤션은 도메인 구분 없이 저장소 전체에 적용된다.

**타입 명시**

- 클래스 프로퍼티, 함수 파라미터, 함수 반환 타입은 타입 추론에 맡기지 않고 항상 명시한다.
- 함수/메서드 **본문 내부**의 지역 변수(`val`/`var`)는 타입 추론을 허용한다.

**Entity는 `data class`로 선언하지 않는다**

- `data class`는 모든 생성자 프로퍼티를 기준으로 `equals`/`hashCode`/`copy()`를 자동 생성하는데,
  `copy()`는 `create()` 팩토리의 invariant 검증을 그대로 우회한다 — 기존 "static create() 팩토리 +
  setter 미노출" 원칙(architecture-checklist.md)이 깨지는 지점이다.
- Entity는 일반 `class` + `companion object`의 `fun create(...)` 팩토리로 만들고, `equals`/`hashCode`는
  식별자(`id`) 기준으로 직접 오버라이드한다.
- DTO는 계속 `data class`를 사용한다 — 불변, 식별자 없음, invariant 검증 불필요.

**Null 안전성**

- `!!` 연산자를 사용하지 않는다. nullable은 `?.`/`?:`/스마트 캐스트로 처리하고, 부득이하게 `!!`가
  필요하면 이유를 주석으로 남긴다.
- Spring/R2DBC 등 Java로 작성된 라이브러리를 호출하는 경계에서 넘어오는 플랫폼 타입을 그대로 전파하지
  않고, 경계에서 즉시 nullable 여부를 명시적으로 처리한다.

**가시성**

- Kotlin 기본 접근제어자는 `public`이다 — Hexagonal 레이어 경계(ADR-0001)를 지키려면 `domain/model`의
  내부 구현 세부사항은 `internal`/`private`로 명시적으로 좁힌다.

**확장 함수**

- Entity ↔ 영속성 모델, 도메인 모델 ↔ DTO 매핑 등 어댑터 계층의 변환 용도로만 제한적으로 사용한다.
- 비즈니스 로직/불변식 검증을 확장 함수로 domain 밖에 두지 않는다 — "비즈니스 로직은 Entity 안에 둔다"
  원칙은 확장 함수에도 동일하게 적용된다.

**비동기 처리 스타일**

- Product부터 시작하는 신규 도메인은 Reactor(`Mono`/`Flux`) 대신 Coroutines(`suspend fun`, `Flow`)를
  사용한다 — 근거는 [ADR-0006](docs/adr/0006-kotlin-coroutines-for-new-domains.md) 참고.
- `warehouse`/`common_code`/`global`은 [ADR-0007](docs/adr/0007-full-kotlin-migration.md)로 Kotlin으로
  포팅됐지만, 언어만 전환하고 비동기 스타일은 그대로 Reactor `Mono`/`Flux`를 유지한다 — Coroutines 전환은
  ADR-0007의 범위 밖이다. 이 패키지들의 기존 코드를 참고할 때 Coroutines 스타일로 오해하지 않는다.
- Reactor 코드에서 coroutine 경계로 넘어갈 때는 `kotlinx-coroutines-reactor`의 `awaitSingle()` /
  `awaitSingleOrNull()` / `asFlow()`로 변환한다.
- Reactor 스타일 코드(Warehouse 등)가 Kotlin `suspend fun`을 직접 호출해야 하는 지점은 그 함수 쪽에
  `Mono`/`Flux`를 반환하는 어댑터 메서드를 별도로 노출한다.

**테스트**

- Kotlin 테스트 메서드명은 백틱(`` `상품 등록 성공`() ``)으로 한글 문장형 이름을 사용한다 — 기존 Java의
  스네이크케이스(`상품_등록_성공`) 대신 Kotlin 관례를 따른다.

## Documentation

`docs/`에는 CLAUDE.md가 다루지 않는 검증용 문서(비즈니스 규칙·아키텍처 체크리스트, ADR, Git 워크플로우)가
있다. 읽는 시점은 문서 성격에 따라 다르다:

- **작업 착수 시 1회**: 새 도메인 구현·리팩토링 등 작업을 시작할 때
  [docs/architecture-checklist.md](docs/architecture-checklist.md),
  [docs/business-rules-checklist.md](docs/business-rules-checklist.md)를 확인한다. 결정 배경이
  필요하면 [docs/adr/](docs/adr/README.md)를 본다. 작업 내내 폭넓게 적용되는 규칙이라 세션 초반에
  한 번 확인해두면 충분하다.
- **행위 직전 매번**: git 커밋 생성·브랜치 생성·PR 작성 직전에는, 세션 초반에 이미 읽었더라도 매번
  [docs/git-workflow.md](docs/git-workflow.md)를 다시 읽고 따른다. 세션이 길어지면 컨텍스트가
  요약되며 예전에 읽은 내용이 사라질 수 있는데, 이런 행위는 빈도가 낮고 되돌리기 번거로우므로 "세션당
  1회"가 아니라 행위 시점마다 확인해 유실 위험을 없앤다.

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
