# ADR-0005: 신규 도메인부터 Kotlin 도입

## 상태
Accepted

## 배경 (Context)
Warehouse 도메인까지는 Java 21로 구현했다. Product부터 시작하는 나머지 도메인(Inventory, Inbound,
Outbound, Disposal, ReturnRequest 등)을 구현하는 시점에, 앞으로의 신규 구현 언어를 Kotlin으로 전환하기로
했다.

고려한 대안:
- **Java 유지**: 팀 관성은 유지되지만, null-safety·`data class`·확장 함수 등으로 얻을 수 있는
  보일러플레이트 감소(Lombok 의존 제거 포함)를 얻지 못한다.
- **기존 Java 코드까지 전체 Kotlin으로 재작성**: Warehouse 도메인은 이미 테스트로 검증된 상태라, 이를
  다시 포팅하며 회귀를 검증하는 비용 대비 얻는 이득이 낮다.
- **신규 도메인만 Kotlin, 기존 Java 코드는 유지 (혼용)**: 채택.

## 결정 (Decision)
Product 이후의 신규 도메인은 Kotlin으로 구현한다. 기존 `warehouse`/`common_code`/`global` 패키지는
Java로 유지하고 마이그레이션하지 않는다. 두 언어는 같은 Gradle 모듈 안에서 공존하며 JVM 바이트코드
레벨로 상호운용한다. Kotlin 코드의 세부 컨벤션(타입 명시, `data class` 사용 범위, null 안전성, 가시성,
확장 함수 범위)은 `CLAUDE.md`의 "Kotlin Coding Conventions" 섹션을 따른다.

## 결과 (Consequences)
- 얻는 것: null-safety, `data class`, 확장 함수 등으로 신규 도메인의 보일러플레이트가 줄어든다. 이미
  검증된 Warehouse 도메인을 재작성하는 리스크를 지지 않는다.
- 감수하는 것: 한 저장소에 두 언어가 공존해 온보딩 비용이 늘고, Java↔Kotlin 경계(특히 nullable
  파라미터 처리)를 매번 신경써야 한다. `build.gradle`에 Kotlin 플러그인(`kotlin-jvm`,
  `kotlin-spring`)과 관련 설정을 첫 Kotlin 코드 작성 전에 추가해야 한다.
- 감사 포인트: 신규 파일이 Java/Kotlin 중 어느 쪽으로 작성됐는지, 해당 도메인이 신규(Product 이후)인지
  확인해 이 결정을 어겼는지 점검한다. `docs/architecture-checklist.md`의 "Kotlin 스타일" 섹션이 이
  결정을 근거로 한다.
