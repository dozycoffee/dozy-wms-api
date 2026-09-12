# ADR-0006: 신규 Kotlin 도메인은 Coroutines 사용

## 상태
Accepted

## 배경 (Context)
ADR-0002는 Reactor(`Mono`/`Flux`) 기반 WebFlux/R2DBC 스택을 채택했다. ADR-0005로 신규 도메인이 Kotlin으로
전환되면서, Kotlin 코드에서도 Reactor를 그대로 쓸지 Kotlin Coroutines(`suspend fun`, `Flow`)로 전환할지
결정이 필요했다.

고려한 대안:
- **Reactor 그대로 사용**: 기존 Java 코드와 스타일이 완전히 동일해 상호운용 이슈가 없다. 다만 Kotlin에서
  `Mono`/`Flux` 체이닝은 장황해지고, Kotlin의 null-safety·일반 제어 흐름(if/try-catch)과 잘 맞물리지
  않는다.
- **Kotlin Coroutines(`suspend fun`, `Flow`) + `kotlinx-coroutines-reactor`**: 채택. Spring
  WebFlux/Data R2DBC가 coroutine 어댑터를 공식 지원하며(`CoroutineCrudRepository` 등), 함수 시그니처가
  `suspend fun foo(): T` 형태로 단순해지고 일반적인 Kotlin 제어 흐름을 그대로 쓸 수 있다.

## 결정 (Decision)
신규 Kotlin 도메인은 Coroutines(`suspend fun`, `Flow<T>`)를 기본으로 사용한다. 기존 Java 코드(Warehouse
등)를 호출하는 경계에서는 `kotlinx-coroutines-reactor`의 `awaitSingle()` / `awaitSingleOrNull()` /
`asFlow()`로 Reactor 타입을 변환한다. 반대로 Java 코드가 Kotlin의 `suspend fun`을 호출해야 하는 경우,
Kotlin 쪽에서 `Mono`/`Flux`를 반환하는 어댑터 메서드를 별도로 노출한다.

## 결과 (Consequences)
- 얻는 것: Kotlin다운 가독성 높은 비동기 코드, Spring 공식 coroutine 지원 활용.
- 감수하는 것: 한 코드베이스 안에 Reactor 스타일(Java)과 Coroutine 스타일(Kotlin)이 공존해 두 패러다임을
  모두 이해해야 한다. Java→Kotlin, Kotlin→Java 각 경계에서 변환 코드가 추가로 필요하다. `build.gradle`에
  `kotlinx-coroutines-core`, `kotlinx-coroutines-reactor` 의존성 추가가 필요하다.
- 감사 포인트: 신규 Kotlin 서비스/UseCase 메서드가 `Mono`/`Flux`를 직접 반환하고 있지 않은지, Java 경계
  변환 지점에 blocking 호출이 섞여 있지 않은지 확인한다.
