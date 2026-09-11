# ADR-0002: WebFlux + R2DBC 논블로킹 스택 채택

## 상태
Accepted

## 배경 (Context)
WMS API는 배치 스캔(유통기한 모니터링), 다중 Location 분산 적재처럼 동시에 여러 하위 작업을 처리해야
하는 경로가 있다. 팀 목표에도 "논블로킹 리액티브 아키텍처 설계 경험"이 명시돼 있다.

고려한 대안:
- **Spring MVC + JPA(JDBC)**: 팀 경험이 더 많고 cascade/orphanRemoval 등 편의 기능을 쓸 수 있지만,
  블로킹 I/O 기반이라 동시 처리 경로에서 스레드 풀 고갈 리스크가 있고 학습 목표와도 맞지 않는다.
- **WebFlux + JPA 혼용**: WebFlux 위에서 블로킹 JDBC를 호출하면 이벤트 루프 스레드가 막혀 리액티브
  스택의 이점이 사라진다.

## 결정 (Decision)
Web 계층은 Spring WebFlux, DB 접근은 Spring Data R2DBC로 통일한다. R2DBC가 JPA의 cascade /
orphanRemoval을 지원하지 않으므로, 연관 엔티티(Zone→Location, Warehouse→Zone/WorkArea 등)는 각 도메인의
전용 Repository를 통해 애플리케이션 레벨에서 명시적으로 저장/삭제한다.

## 결과 (Consequences)
- 얻는 것: 요청 경로 전체가 논블로킹으로 일관되며, 배치 스캔처럼 다건을 다룰 때 `Flux`/`Mono.zip` 등으로
  병렬화가 자연스럽다.
- 감수하는 것: cascade가 없으므로 연관 엔티티 저장 순서와 삭제 순서를 서비스 코드에서 직접 관리해야 하고,
  누락 시 고아 데이터가 남을 수 있다 — 관련 서비스 코드는 이 누락 여부를 리뷰 시 반드시 확인한다.
  블로킹 라이브러리(JDBC 드라이버, 동기 HTTP 클라이언트 등)를 R2DBC 체인 안에서 호출하지 않도록 주의가
  필요하다.
- 감사 포인트: `docs/architecture-checklist.md`의 "R2DBC 체인에 blocking 코드 없음" 항목이 이 결정을
  근거로 한다.
