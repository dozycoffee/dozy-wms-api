# ADR-0010: 인증/인가를 위한 CurrentAccessScopeProvider 포트 도입

## 상태
Accepted

## 배경 (Context)

이 프로젝트는 인증/인가가 전혀 없다 — Spring Security/JWT 의존성이 없고, Spring Profile 자체가
정의되어 있지 않다(`application.yaml`은 단일 설정 파일이며 `@Profile` 사용 이력도 없다). 팀은 향후
별도 MSA 인증 서비스가 `.well-known` JWKS 엔드포인트로 공개키를 제공하는 OAuth2 Resource Server
패턴을 붙일 계획이지만, 그 서비스는 아직 준비되지 않았다.

이 공백 때문에 이미 두 곳에서 "현재 사용자"를 문자열 `"system"`으로 하드코딩하고 있다
(`R2dbcConfig.auditorAware()`, `ProductService.DELETED_BY_SYSTEM`). 또한 재고/Zone 조회 API의
`warehouseIds` 필터는 클라이언트가 보낸 값을 그대로 신뢰하는 "조회 편의" 기능일 뿐, 실제 인가 범위로
좁혀주지 않는다.

인증 서비스가 준비되기 전까지 이 문제들을 방치할지, 아니면 나중에 실제 구현으로 교체 가능한 추상화를
먼저 만들어둘지 결정이 필요했다.

고려한 대안:

- **아무것도 만들지 않고 각 소비 지점에서 계속 `"system"`을 하드코딩**: 지금 당장은 구현 비용이
  0이지만, 나중에 실제 인증이 붙을 때 하드코딩된 지점을 전부 찾아 고쳐야 하고, 그 사이에 새로 추가되는
  코드도 같은 하드코딩을 반복하기 쉽다.
- **인터페이스 없이 지금 바로 실제 JWT/JWKS 연동을 구현**: 인증 서비스 자체가 아직 없어 애초에
  불가능하다.
- **포트(인터페이스) + mock 어댑터를 먼저 도입**: 이 저장소가 이미 전역적으로 쓰는 Hexagonal
  패턴(인터페이스는 `application`/소비 지점, 구현은 `adapter`)을 그대로 확장한다. Controller/Service는
  인터페이스에만 의존하므로, 나중에 실제 JWT/JWKS 어댑터로 교체해도 호출부가 바뀌지 않는다.

## 결정 (Decision)

`CurrentAccessScopeProvider` 인터페이스와 `MockAccessScopeProvider` 구현체를 도입한다.

- **패키지 위치**: `global/security/`에 평탄하게 둔다(`port`/`adapter` 하위 구조를 만들지 않는다).
  `global`은 이미 `config`/`common`/`error`/`persistence`로 평탄하게 구성되어 있고, 지금 만드는 파일도
  3개(`AccessScope`, `CurrentAccessScopeProvider`, `MockAccessScopeProvider`)뿐이라 4단 구조는
  과설계다. 나중에 JWT 파서·JWKS 리졸버·캐시 등이 붙어 파일이 늘어나면 그때 도메인식 `port`/`adapter`
  구조로 승격한다.
- **Bean 등록**: `@Profile`/`@ConditionalOnProperty` 같은 게이팅 없이 무조건 `@Component`로 등록한다.
  Profile 인프라 자체가 없는 프로젝트에 mock 하나 때문에 새 설정 체계를 들이는 건 과하다. "mock이
  프로덕션에 조용히 남는" 위험은 자동 게이팅 대신 (a) `Mock` 접두어가 붙은 클래스명, (b) 이 ADR의
  명문화, (c) 코드리뷰로 관리한다.
- **`AccessScope` 기본값은 하드코딩**: `userId = "system"`, `warehouseIds = emptyList()`(= "제한
  없음", 기존 `warehouseIds` 조회 필터의 null/빈 리스트 의미론과 일치). `@Value` 등으로 설정 가능하게
  만들지 않는다 — mock 단계에서 쓸모없는 설정 표면만 늘어난다.
- **시그니처는 `suspend fun get(): AccessScope` 하나만 둔다.** 이 저장소에서 Warehouse(Reactor
  스타일) 코드가 Coroutine 코드를 호출하는 역방향 사례는 실제로 0건이다(항상 Coroutine 도메인이
  `.collectList().awaitSingle()`로 Warehouse의 Reactor UseCase를 소비하는 단방향). `Mono` 반환
  버전을 지금 미리 만들어두는 건 확인되지 않은 선제적 설계라 넣지 않는다.
- **`AccessScope`에 `userId`도 포함한다.** `warehouseIds`뿐 아니라 `userId: String`도 넣는다 —
  `"system"`을 하드코딩 중인 두 지점이 이미 구체적으로 식별된 미래 소비자이고, 둘 다 필요한 게 "현재
  행위자 문자열"이므로 지금 필드를 넣어두는 편이 나중에 타입을 다시 설계하는 것보다 저렴하다.

이번 결정은 포트 인터페이스와 mock 어댑터의 골격만 확정한다. 다음은 의도적으로 이번 범위에서 제외하고
후속 작업으로 분리한다:

- `R2dbcConfig.auditorAware()` / `ProductService.DELETED_BY_SYSTEM`을 `CurrentAccessScopeProvider`로
  교체하는 작업
- `warehouseIds` 조회 필터와 `AccessScope.warehouseIds`의 교차(intersection) 검증 로직
- 실제 JWT/JWKS 기반 어댑터 구현, Spring Security/JWT 의존성 추가, Spring Profile 인프라 도입

## 결과 (Consequences)

- 얻는 것: Controller/Service가 인터페이스에만 의존하게 되어, 나중에 실제 JWT/JWKS 어댑터가 준비되면
  `MockAccessScopeProvider`를 새 구현체로 교체하는 것만으로 인증이 연결된다 — 소비 지점 코드는 바뀌지
  않는다.
- 감수하는 것: mock이 프로덕션 게이팅 없이 등록되므로, 이 프로젝트에 실제 인증이 필요한 시점 전까지는
  "누구나 `system` 권한으로 동작"하는 상태가 계속된다. 이는 지금도 이미 그런 상태(인증 자체가 없음)라
  이 결정으로 새로 생기는 위험은 아니지만, `MockAccessScopeProvider`가 실제 어댑터로 교체되지 않은 채
  방치되지 않도록 코드리뷰에서 계속 확인해야 한다.
- 감사 포인트: 실제 JWT/JWKS 어댑터를 구현하는 시점에 이 ADR의 "이번 범위에서 제외" 목록이 모두
  처리됐는지 확인한다.
