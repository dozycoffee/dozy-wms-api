# ADR-0007: 전체 코드베이스 Kotlin 마이그레이션

## 상태
Accepted (ADR-0005를 대체함)

## 배경 (Context)
Product 도메인의 `ProductErrorCode`를 구현하던 중, Java로 남아있던 `ErrorCode` 인터페이스(메서드 기반:
`getCode()`/`getMessage()`/`getErrorType()`)를 Kotlin에서 `override val` 프로퍼티로 구현할 수 없다는
제약에 부딪혔다. Java 인터페이스의 getter 메서드는 Kotlin이 프로퍼티로 오버라이드하는 것을 허용하지
않고(`fun`으로만 구현 가능), 이는 `java.util.Map.Entry`처럼 Kotlin 컴파일러가 특별 매핑해둔 극소수 JDK
내장 타입에만 예외적으로 적용되는 기능이었다.

이 마찰을 계기로, ADR-0005가 검토했던 대안 중 하나였던 "기존 Java 코드까지 전체 Kotlin으로 재작성"을
다시 논의했다. ADR-0005 당시에는 "이미 테스트로 검증된 Warehouse 도메인을 다시 포팅하며 회귀를 검증하는
비용 대비 얻는 이득이 낮다"는 이유로 기각했었다.

고려한 대안:
- **현재 방식 유지 (Java Warehouse + Kotlin 신규 도메인 혼용)**: 회귀 위험은 없지만, `ErrorCode`처럼
  Java 인터페이스에 걸려 있는 한 언어 경계 마찰이 신규 도메인에서 반복적으로 발생한다.
- **`global` 패키지만 Kotlin으로 전환**: 마찰의 근본 원인(`ErrorCode` 등 공유 인프라)만 해소하고
  Warehouse 도메인 로직(용량 계산, occupy/release 등 회귀 위험이 큰 부분)은 건드리지 않는 절충안.
- **전체 마이그레이션**: 채택. 사용자가 언어 혼용 자체를 없애기로 결정.

## 결정 (Decision)
`src/main/java`, `src/test/java` 전체(130개 파일: `global`, `common_code`, `warehouse` 도메인의
model/enumeration/exception/valueobject/application/adapter 전 계층과 대응 테스트)를 Kotlin으로
포팅했다. 이번 마이그레이션은 **언어만 전환하고 동작은 1:1로 보존**하는 것을 원칙으로 했다:

- **비동기 스타일은 그대로 Reactor `Mono`/`Flux`를 유지한다.** ADR-0006(신규 Kotlin 도메인은
  Coroutines 사용)은 범위를 바꾸지 않는다 — Warehouse가 Kotlin으로 바뀌었다고 해서 Coroutines로 다시
  쓰는 것은 "언어 마이그레이션"과 "비동기 패러다임 전환"이라는 서로 다른 두 개의 큰 변경을 한 번에
  섞는 것이라 리스크를 키운다고 판단해 별도 논의로 미뤘다.
- `ErrorCode`를 Kotlin 프로퍼티 기반 인터페이스(`val code`/`val message`/`val errorType`)로 재정의하고,
  모든 `*ErrorCode` enum이 `override val`로 구현하도록 통일했다 — 애초에 이 마이그레이션을 촉발한 문제를
  해결하는 지점이다.
- Lombok(`@Getter`, `@RequiredArgsConstructor`, `@AllArgsConstructor` 등) 의존성을 완전히 제거했다.
  Kotlin의 `data class`, 주 생성자 프로퍼티, `private set`이 동일한 역할을 대체한다.
- `application.port.in`, `adapter.in.web`처럼 Kotlin 예약어(`in`)와 충돌하는 패키지 세그먼트는
  백틱(`` `in` ``)으로 이스케이프했다 — 디렉터리 구조와 아키텍처 컨벤션(`docs/architecture-checklist.md`
  의 hexagonal 구조)은 그대로 유지한다.
- Mockito의 `any()`/`ArgumentCaptor.capture()`가 `null`을 반환해 Kotlin의 non-null 파라미터 타입과
  충돌하는 문제 때문에 `testImplementation 'org.mockito.kotlin:mockito-kotlin:5.4.0'`을 추가했다.

## 결과 (Consequences)
- 얻는 것: 코드베이스 전체가 한 언어로 통일되어 `override val`, `data class`, null-safety 등 Kotlin
  관용구를 어디서나 일관되게 쓸 수 있다. Lombok 제거로 빌드 의존성이 단순해졌다.
- 감수하는 것: 이번 마이그레이션 세션 환경에는 로컬 MySQL이 없어 `@DataR2dbcTest` 슬라이스(영속성
  어댑터 4종)와 `DozyWmsApiApplicationTests.contextLoads()`는 실제 DB 대비 검증하지 못했다. 다만 이
  10건은 마이그레이션 이전에도 동일한 이유(DB 미기동)로 실패하던 항목이라 회귀 여부를 판단할 기준선은
  있다 — Docker MySQL을 띄운 뒤 `./gradlew test`로 반드시 재확인이 필요하다. 그 외 127개 테스트는
  마이그레이션 전후 동일하게 통과한다.
- `docs/business-rules-checklist.md`의 `.java` 파일 경로 링크를 `.kt`로 갱신했다. `CLAUDE.md`의 프로젝트
  개요/Kotlin 컨벤션 섹션도 "Product부터 Kotlin, 기존은 Java 유지"라는 전제가 더 이상 사실이 아니므로
  함께 갱신했다.
- 감사 포인트: 코드베이스에 `.java` 소스 파일이 남아있으면(신규로 추가되었든 마이그레이션에서
  누락되었든) 이 결정 위반이다. Warehouse의 Reactor 스타일을 Coroutines로 바꾸는 것은 이 ADR의 범위가
  아니며, 필요하면 별도 ADR로 다룬다.
