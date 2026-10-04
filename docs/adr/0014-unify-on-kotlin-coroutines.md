# ADR-0014: 비동기 스타일을 Kotlin Coroutines로 통일

## 상태
Accepted (ADR-0006, ADR-0007의 "Warehouse/global은 Reactor 유지" 부분을 대체함)

## 배경 (Context)
ADR-0006은 신규 도메인을 Coroutines로, ADR-0007은 Warehouse를 언어만 Kotlin으로 옮기고 Reactor 스타일을
유지하기로 했다. 그 결과 `warehouse` 4개 하위 도메인(Warehouse/Zone/WorkArea/Location)만 `Mono`/`Flux`였고,
이를 호출하는 inbound·outbound·return_request·disposal·stock_audit·warehouse_member·devseed 서비스마다
`awaitSingle()`/`collectList().awaitSingle()` 브릿지가 흩어져 있었다. `warehouse`가 모든 도메인의 공통 의존이라
브릿지 비용이 계속 늘었고, 두 패러다임을 모두 이해해야 하는 부담이 남았다.

고려한 대안:
- **현상 유지**: 회귀 위험은 없지만 브릿지와 이중 패러다임 부담이 지속된다.
- **Reactor로 통일**: 코드베이스 대부분을 재작성해야 하고 Kotlin 제어 흐름과 맞지 않는다(ADR-0006 기각 사유 유효).
- **Coroutines로 통일**: 채택. 변환 대상이 `warehouse` 약 2k LOC와 테스트에 한정된다.

## 결정 (Decision)
`warehouse`의 Port/Service/Persistence/Controller를 `suspend fun`/`Flow`로 전환하고 호출부의 브릿지를 제거한다.

- Repository는 `CoroutineCrudRepository`, 단건 조회는 `T?`, 목록은 `Flow<T>`를 쓴다. 미존재는 서비스에서
  `?: throw XxxNotFoundException()`으로 변환한다.
- 서비스의 `@Transactional`은 그대로 둔다. 트랜잭션 안에서 `launch`/`async`로 새 코루틴을 띄우지 않는다
  (트랜잭션 컨텍스트가 공유되지 않을 수 있다).
- 프레임워크가 Reactor 타입을 요구하는 지점만 예외로 남긴다: `SecurityConfig`의
  `Converter<Jwt, Mono<AbstractAuthenticationToken>>`, `ReactiveSecurityContextHolder`/Reactor Context 접근
  (`awaitSingleOrNull`), `ReactiveAuditorAware`(`mono {}`). `DatabaseClient`는 Spring의 Kotlin 확장
  (`awaitRowsUpdated`/`awaitOne`/`awaitOneOrNull`/`flow`)으로 쓰므로 예외가 아니다.
- 테스트는 `runTest`와 mockito-kotlin을 쓰고, `reactor-test` 의존성을 제거한다.

## 결과 (Consequences)
- 얻는 것: 단일 비동기 스타일, 도메인 간 브릿지 호출 제거, `Mono`/`Flux` 체이닝 제거.
- 감수하는 것: 위 프레임워크 경계에는 Reactor가 의도적으로 남는다. Coroutines의 취소는 협력적이라
  `catch (e: Exception)`이 `CancellationException`을 삼키지 않도록 주의해야 한다.
- 검증: 서비스 단위 테스트는 Repository를 mock으로 대체하므로 트랜잭션 의미는 `DevSeedRunnerTest`
  (한 트랜잭션에서 전체 UseCase 흐름 재생)와 `*PersistenceAdapterTest`가 실 MySQL로 검증한다.
- 감사 포인트: `warehouse`·`global`에 `Mono`/`Flux`를 반환하는 UseCase/Repository가 남아 있지 않은지,
  프레임워크 경계 외 지점에 `awaitSingle()` 브릿지가 다시 생기지 않았는지 확인한다.
