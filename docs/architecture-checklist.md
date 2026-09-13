# 아키텍처 체크리스트

## 목적과 사용법

이 문서는 진행 상황 추적용이 아니라 **구현 정확성 검증용**이다. `CLAUDE.md`의 "Key Patterns" 섹션을
도메인 무관 구조 규칙으로 옮겨 놓은 것으로, 도메인별 인스턴스를 기록하지 않고 규칙 목록만 유지하는
**재사용 체크리스트**다.

- 새 도메인/파일(Entity, Service, Controller, DTO 등)을 작성하거나 리뷰할 때 이 체크리스트와 대조한다.
- `/code-review` 실행 시 이 문서를 참고 기준으로 사용한다.
- 규칙이 왜 이렇게 정해졌는지 근거가 필요하면 `docs/adr/`를 먼저 본다 — 규칙 옆 `(ADR-XXXX)` 표기 참고.
- 규칙 자체가 바뀌면 `CLAUDE.md`를 먼저 수정하고 이 문서를 동기화한다 — 이 문서가 1차 출처가 아니다.

## 에러 처리

- [ ] `ErrorCode` 인터페이스 → 도메인별 enum(`XxxErrorCode`) → 타입별 `BusinessException` 서브클래스
      구조를 따른다
- [ ] `GlobalExceptionHandler`(`@RestControllerAdvice`)가 처리하도록 커스텀 예외를 개별 Controller에서
      직접 catch하지 않는다
- [ ] WebFilter/Security 레벨에서 발생하는 예외는 `WebExceptionHandler`로 처리한다

## Entity

- [ ] 모든 Entity는 `BaseEntity`를 상속한다 (`createdAt`/`createdBy`/`updatedAt`/`updatedBy` R2DBC
      Auditing)
- [ ] Soft delete 대상 테이블(product, inventory 등)은 `SoftDeletableEntity`를 상속해 `deletedAt`/
      `deletedBy`를 갖는다
- [ ] Entity 생성은 `static create()` 팩토리 메서드로만 하고, 그 안에서 검증을 수행한다
- [ ] 비즈니스 로직과 불변식(invariant) 검증은 Entity 안에 둔다 (Service로 새어나가지 않는다)
- [ ] setter를 노출하지 않는다 — 상태 변경은 의미 있는 도메인 메서드(`occupy`, `release` 등)로 한다
- [ ] 연관 Entity는 cascade/orphanRemoval에 의존하지 않고, 각 도메인의 전용 Repository로 명시적으로
      저장/삭제한다 (ADR-0002)

## Common Code

- [ ] 상태/분류 코드는 `common_code` 테이블을 참조하며, 코드 형식은 `{GROUP}_{VALUE}`를 따른다
      (예: `TEMPERATURE_TYPE_AMBIENT`) (ADR-0004)
- [ ] 애플리케이션 레벨에서는 Java enum으로 정의하고, DB에는 코드 문자열로 저장한다

## DTO

- [ ] Request/Response DTO는 모두 Java `record`로 정의한다

## R2DBC / 리액티브

- [ ] R2DBC 체인 안에서 blocking I/O(동기 JDBC, 동기 HTTP 클라이언트 등)를 호출하지 않는다 (ADR-0002)
- [ ] 불필요하게 전체 엔티티를 로딩하지 않고, 필요한 컬럼만 DTO Projection으로 조회한다
- [ ] 루프 안에서 개별 쿼리를 반복하지 않고 `IN` 절 또는 `flatMap` 병렬화로 처리한다
- [ ] 트랜잭션 경계는 Service 레이어의 `@Transactional`로 명시한다 (조회 전용은
      `@Transactional(readOnly = true)`)
- [ ] 에러 전파가 필요한 지점에 `onErrorMap`/`onErrorResume`을 명시한다

## Controller / 레이어 경계

- [ ] Controller는 요청/응답 처리만 담당하고 비즈니스 로직을 포함하지 않는다
- [ ] 의존성 주입은 생성자 주입만 사용한다 (필드 주입 금지)

## Warehouse 도메인 구조

- [ ] `Warehouse → Zone → Location` 계층 구조를 따른다
- [ ] `WorkArea`는 Zone과 분리된, `Warehouse`에 직속된 온도 무관 공용 작업 구역으로 취급한다
- [ ] Zone code는 `A`~`E`이며 D Zone만 `COLD`, 나머지는 `AMBIENT`다
- [ ] Location code 형식은 `A-01`을 따른다
- [ ] `usedCapacity`/`maxCapacity`는 적재/반출 즉시 갱신하고, DB `CHECK` 제약으로 초과를 이중 방지한다

## 테스트 레이어 분리

- [ ] `*EntityTest` — Spring Context 없이 순수 도메인 로직/불변식만 검증
- [ ] `*ServiceTest` — `@ExtendWith(MockitoExtension::class)`로 Repository를 mock
- [ ] `*ControllerTest` — `@WebFluxTest` 슬라이스 + `WebTestClient`
- [ ] `*RepositoryTest` — `@DataR2dbcTest` 슬라이스, 실제 쿼리 검증
- [ ] 위 레이어 간 책임이 겹치지 않는다 (예: ServiceTest에서 실제 DB를 쓰지 않는다)
- [ ] 테스트 픽스처는 `fixture/` 패키지의 `XxxTestBuilder`(Entity), `XxxDtoBuilder`(DTO)를 사용한다

## Kotlin 스타일 (ADR-0007, ADR-0006)

이 섹션은 저장소 전체(신규 도메인뿐 아니라 ADR-0007로 포팅된 Warehouse/common_code/global 포함)에
적용된다.

- [ ] 클래스 프로퍼티, 함수 파라미터, 함수 반환 타입은 명시한다 — 메서드 본문 내부 지역 변수는 타입
      추론을 허용한다
- [ ] Entity는 `data class`로 선언하지 않는다 — `copy()`가 `create()` 팩토리의 invariant 검증을
      우회하기 때문. 일반 `class` + `companion object.create()` + 식별자 기반 `equals`/`hashCode`
      오버라이드를 사용한다
- [ ] DTO(Java record 대응)는 `data class`로 선언한다
- [ ] `!!` 연산자를 사용하지 않는다
- [ ] `domain/model`의 내부 구현 세부사항은 `internal`/`private`로 가시성을 명시적으로 좁힌다
- [ ] 확장 함수는 어댑터 계층의 매핑 용도로만 사용하고, 비즈니스 로직을 확장 함수로 domain 밖에
      두지 않는다
- [ ] 신규 Kotlin 도메인은 `Mono`/`Flux` 대신 `suspend fun`/`Flow`를 사용한다 — 기존 Java 코드 호출
      경계에서만 `kotlinx-coroutines-reactor`로 변환한다

## 아키텍처 스타일 (ADR-0001)

- [ ] 도메인 패키지가 `adapter(in/out)` / `application(port, service)` / `domain(model, enums,
      exception, valueobject, service)` 구조를 따른다
- [ ] UseCase 인터페이스(`application/port/in`)를 REST Controller와 이벤트 어댑터가 함께 재사용한다
- [ ] 도메인 모델(`domain/model`)이 영속성 구현(`adapter/out/persistence`)에 의존하지 않는다
