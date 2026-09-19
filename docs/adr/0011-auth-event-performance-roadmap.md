# ADR-0011: 인증·이벤트·성능 확장을 위한 로드맵 방향

## 상태
Accepted

## 배경 (Context)

프로젝트 현재 구현 상황을 점검하는 과정에서, 앞으로 진행할 세 가지 확장 방향이 확인됐다.

- **인증/인가**: 별도 MSA 인증 서비스가 `.well-known` JWKS 엔드포인트로 공개키를 제공하는 OAuth2
  Resource Server 패턴을 도입할 예정이나, 그 인증 서비스 자체가 아직 구현되지 않았다. 현재는
  [ADR-0010](0010-current-access-scope-provider-port.md)으로 `CurrentAccessScopeProvider` 포트와
  `MockAccessScopeProvider`만 만들어둔 상태다.
- **이벤트 기반 전환**: 도메인 간 결합을 점진적으로 이벤트 방식 + 아웃박스 패턴으로 전환할 계획이다.
  현재는 `InboundService`→`RegisterDisposalUseCase`, `ReturnRequestService`→
  `RegisterDisposalUseCase`, `ExpirationMonitoringService`→`Inventory` 등 도메인 간 호출이 전부
  동기 직접 호출이다. 다만 `Allocation`의 `idempotency_key`([ADR-0008](0008-inventory-allocation-entity.md))가
  이미 "향후 이벤트 기반 비동기 처리"를 염두에 두고 설계돼 있어, 방향 자체는 낯설지 않다.
- **성능**: 테스트를 이어가면서 필요한 지점에 인덱스를 추가하고 Redis 캐시 도입을 검토할 계획이다.

세 방향 모두 선행 조건(인증 서비스 존재, 브로커 선택, 실측 트래픽 병목)이 아직 갖춰지지 않았다. "선행
조건이 없어도 지금 해도 안전한 작업"과 "선행 조건이 갖춰질 때까지 보류할 작업"을 구분해두지 않으면,
조급하게 만든 코드를 선행 조건이 정해진 뒤 다시 뜯어고치게 될 위험이 있다.

## 결정 (Decision)

### 1. 인증/인가 (JWKS 기반 Resource Server)

**지금 진행**
- `CurrentAccessScopeProvider` 소비 지점 교체: `R2dbcConfig.auditorAware()`, `ProductService.DELETED_BY_SYSTEM`,
  `InventoryService.DISPOSAL_ACTOR`의 `"system"` 하드코딩을 인터페이스 호출로 바꾼다. Mock이라도
  인터페이스를 통하게 해두면 실제 어댑터로 교체할 때 소비 지점 코드가 바뀌지 않는다.
- `InventoryController`/`ZoneInventorySummaryService`의 `warehouseIds` 조회 필터와
  `AccessScope.warehouseIds`의 교집합 검증 로직 추가 — 현재는 클라이언트가 보낸 값을 그대로 신뢰하는
  "조회 편의" 기능일 뿐 실제 인가 범위로 좁혀주지 않는다. Mock 단계에서도 로직 자체는 검증 가능하다.

**보류 (실제 인증 서비스의 JWKS 엔드포인트가 준비된 뒤)**
- `spring-boot-starter-oauth2-resource-server` 의존성 추가, `jwk-set-uri` 설정
- Spring Profile 인프라 도입(local/dev/prod) — Mock 어댑터가 prod에 조용히 남지 않도록 게이팅
- JWT 검증 실패(401)를 처리할 `WebExceptionHandler`

**설계 메모 (지금 코드화하지 않고 기록만 남김)**
`R2dbcConfig.auditorAware()`는 전역 Reactor Bean(저장 시마다 호출)인데
`CurrentAccessScopeProvider.get()`은 `suspend fun`이다. 지금 Mock은 요청과 무관하게 고정값을
반환해서 이 문제가 드러나지 않지만, 실제 JWT 인증이 붙으면 "현재 요청의" principal을 R2DBC auditing
콜백 시점에 가져와야 한다. WebFlux에서 요청별 보안 컨텍스트는 `ThreadLocal`이 아니라 **Reactor
Context**(`ReactiveSecurityContextHolder`)에 실리므로, 실제 어댑터는 Reactor Context에서 값을
꺼내야 하고 `auditorAware()`(Mono 반환, CLAUDE.md 컨벤션상 Reactor 쪽 어댑터 메서드)와 coroutine
소비 지점(`suspend fun`) 양쪽에 동일한 Context가 전파되도록 맞춰야 한다. 지금 미리 구현하면 실제
JWKS 연동 시점에 다시 뜯어고칠 가능성이 높아 코드화하지 않고, 실제 어댑터 구현 시점에 이 메모를
출발점으로 삼는다.

### 2. 이벤트 기반 + 아웃박스 전환

3단계 점진 전환으로 가되, **지금은 1단계만 진행**한다.

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 브로커/아웃박스 없이 Spring `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`로 도메인 간 직접 UseCase 호출(Inbound/ReturnRequest → Disposal, ExpirationMonitoringService → Inventory)을 이벤트 발행/구독으로 전환해 결합도만 먼저 낮춘다 | 지금 진행 |
| 2 | 서비스 분리를 준비할 때 `outbox_event` 테이블(`aggregate_type`/`aggregate_id`, `event_type`, `payload`, `published_at`)을 도입하고, 기존 `ExpirationMonitoringScheduler`와 같은 `@Scheduled` 폴링 패턴으로 미발행 건을 릴레이한다 | 보류 |
| 3 | 실제 메시지 브로커(Kafka 등) 연동, 서비스 분리와 병행 | 보류 |

1단계만 먼저 하는 이유: 브로커 선택이나 이벤트 페이로드 스키마처럼 되돌리기 어려운 결정 없이 결합도만
먼저 풀 수 있고, 나중에 "이벤트로 소통하는 경계"가 코드에 이미 드러나 있어야 2단계(아웃박스)로 넘어가기
쉽다. 아웃박스 테이블을 지금부터 만드는 것은 이벤트 스키마/브로커가 미정인 상태에서의 과설계로 본다.

### 3. 성능 (인덱스 / Redis 캐시)

**인덱스** — 실제 쿼리 패턴을 근거로 우선순위를 정한다(MySQL은 FK 컬럼에 단일 컬럼 인덱스를 자동
생성하므로, 그것만으로 부족한 복합 조건 조회가 대상).
- `inventory(product_id, quality_status[, deleted_at])` — **최우선**.
  [OutboundService.pickFifo()](../../src/main/kotlin/com/dozycoffee/wms/outbound/application/service/OutboundService.kt)가
  출고 피킹마다 이 조합(`findAllActive` 쿼리, `product_id` + `quality_status` + `deleted_at`)으로
  조회하는데, FK 단일 인덱스로는 두 조건을 동시에 타지 못한다.
- `lot(expiration_date)` — [ExpirationMonitoringService.scan()](../../src/main/kotlin/com/dozycoffee/wms/inventory/application/service/ExpirationMonitoringService.kt)의
  일일 배치 스캔(`findAllByLotStatusNotAndExpirationDateLessThanEqual`)이 사용. `lot_status`는 FK라
  단일 인덱스가 있지만 `!=` 조건이라 효율이 낮고 `expiration_date`엔 인덱스 자체가 없다. 배치라 지연
  허용도는 있지만 Lot 테이블이 커지면 풀스캔 비용이 늘어난다.
- `inventory_history(inventory_id, created_at)`은 V18 마이그레이션에서 이미 처리됨 — 추가 작업 없음.
- Zone/Location 쪽은 창고 규모(Zone 5개, Location 약 15개)가 작아 추가 인덱스 이득이 낮아 낮은
  우선순위로 둔다.

**Redis 캐싱 정책** — "읽기 많고 쓰기 드문" 데이터만 후보로 삼는다.
- 캐싱 후보: `Product` 마스터, `common_code`(사실상 상수), `Warehouse`/`Zone`/`WorkArea`의 정적
  구조(이름/코드/`maxCapacity`) — 변경 빈도가 낮다.
- **캐싱 금지**: `Location.usedCapacity`, `Inventory` 수량류(`quantity`/`allocatedQuantity`) —
  입출고마다 바뀌는 값이라 캐싱 시 무효화 비용이 이득을 상쇄하고, WMS에서 재고 수량 stale은 치명적
  버그로 직결된다. `zone-summary` 같은 집계 API도 실시간성이 핵심이라 캐싱보다 위 인덱스 튜닝으로
  쿼리 자체를 빠르게 하는 쪽을 우선한다.

## 결과 (Consequences)

- **인증**: 지금 진행하는 두 작업(소비 지점 교체, `warehouseIds` 교집합 검증)은 Mock 상태에서도
  완결되므로 즉시 착수 가능하다. 다만 실제 JWKS 어댑터 구현 시점에 위 "설계 메모"(Reactor Context
  propagation)를 재검토해야 하며, 이를 놓치면 `auditorAware()`가 실제 요청과 무관한 값을 계속 기록하는
  버그가 생길 수 있다.
- **이벤트 전환**: 1단계(`ApplicationEventPublisher`) 도입 시 `@TransactionalEventListener(AFTER_COMMIT)`
  특성상 커밋 이후 시점 검증이 필요해, 기존 `*ServiceTest`(Mockito, 트랜잭션 없음) 레이어만으로는
  커버되지 않는다 — 새로운 통합 테스트 레이어가 필요해질 것이다. 2/3단계로 넘어가는 시점은 실제 서비스
  분리 계획이 구체화된 뒤 별도 ADR로 재논의한다.
- **성능**: 인덱스 추가는 마이그레이션으로 즉시 가능하며 리스크가 낮다. Redis는 캐싱 대상을 정적
  데이터로 한정하지 않으면 재고 정합성 사고로 이어질 수 있음을 감사 포인트로 남긴다.
- **감사 포인트**: 인증 서비스의 JWKS 엔드포인트가 실제로 준비되는 시점, 그리고 서비스 분리가
  구체화되는 시점에 이 ADR의 "보류" 항목들이 그대로 유효한지 재검토한다.
