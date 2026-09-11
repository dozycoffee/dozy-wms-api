# ADR-0001: 도메인별 Hexagonal(Ports & Adapters) 구조 채택

## 상태
Accepted

## 배경 (Context)
WMS는 입고·출고·반품·폐기·재고·유통기한 모니터링 등 도메인마다 상태 흐름과 비즈니스 규칙이 다르고,
배치 스캔(유통기한 모니터링) 같은 별도 트리거와 REST API 트리거가 같은 도메인 로직을 공유해야 한다.

고려한 대안:
- **Transaction Script / 단일 Service-Controller 구조**: 초기 구현 속도는 빠르지만, 도메인 규칙(예: FIFO
  피킹, capacity 사전 점검)이 Service 여러 곳에 중복되기 쉽고 트리거(REST vs 배치)별로 Service가 갈라지면
  로직이 발산한다.
- **Layered Architecture(Controller-Service-Repository)만 사용**: 팀에 익숙하지만 도메인 모델이
  Repository/영속성 구현에 의존하게 되기 쉽다.

## 결정 (Decision)
각 도메인 패키지를 `adapter(in/out)` / `application(port, service)` / `domain(model, enums, exception,
valueobject, service)`로 분리하는 Hexagonal 구조를 채택한다. 도메인 모델은 순수 POJO로 유지하고, REST
Controller와 이벤트 수신 어댑터는 모두 `adapter/in`을 통해 같은 UseCase(`application/port/in`)를 호출한다.

## 결과 (Consequences)
- 얻는 것: 도메인 규칙이 `domain` 레이어에 한 곳으로 모이고, REST/배치 등 트리거가 늘어나도 UseCase
  재사용이 가능하다. R2DBC 영속성 구현(`adapter/out/persistence`)이 바뀌어도 도메인 로직은 영향받지 않는다.
- 감수하는 것: 파일 수와 레이어 간 매핑 코드(Entity ↔ 도메인 모델)가 늘어난다. 단순 CRUD성 도메인에는
  과할 수 있으므로, 신규 도메인 설계 시 이 구조를 기본값으로 따르되 예외가 필요하면 별도 ADR로 근거를
  남긴다.
- 감사 포인트: `docs/architecture-checklist.md`의 레이어 분리 항목이 이 결정을 근거로 한다.
