# ADR-0026: Kafka 토픽은 인프라가 선언하고 서비스는 만들지 않는다

## 상태
Accepted. ADR-0020의 "토픽을 압축으로 만든다"와 ADR-0021 결정 5의 "inventory가 토픽을 압축으로 만든다"에서 **누가 만드는가**를 대체한다(압축이 필요하다는 요구는 그대로다).

## 배경 (Context)
WMS는 `dozy.wms.warehouse-access`를 `NewTopic` 빈으로 직접 만들었다(파티션 3, 복제 1, 압축). 한편 ADR-0021은 `dozy.inventory.product`의 압축을 inventory가 만들어 주는 것으로 가정했지만, inventory에는 토픽을 만드는 코드가 없다. 결과적으로 토픽을 만드는 주체가 서비스마다 다르고, 한 곳은 아무도 만들지 않는다.

토픽 설정은 서비스 하나의 내부 사항이 아니다.

- 파티션 수는 줄일 수 없고 같은 키의 순서 보장이 파티션 수에 묶여 있다.
- 보존 기간과 압축 여부는 구독자에게 영향을 준다. 예를 들어 압축이 아니면 새 환경의 WMS는 오래된 상품 이벤트를 읽지 못한다.
- 실패한 메시지를 보내는 `{토픽}.dlt`도 토픽이라, 자동 생성이 꺼진 브로커에서는 미리 있어야 한다.

대안:

| 대안 | 평가 |
|---|---|
| 브로커 자동 생성에 기댄다 | 파티션과 보존이 기본값이 되고 오타 토픽도 조용히 생긴다. 압축이 보장되지 않는다 |
| 각 서비스가 `NewTopic`으로 만든다 | 정의가 코드 옆에 있지만 서비스마다 주체가 갈리고, 환경마다 설정이 어긋나기 쉽다. 서비스에 토픽 관리 권한이 필요하다 |
| **인프라가 토픽을 선언하고 만든다(채택)** | 설정이 한곳에 모이고 리뷰된다. 서비스는 발행과 구독만 한다 |

## 결정 (Decision)
1. **토픽은 `dozy-infra`의 `topics/topics.yaml`에 선언하고 인프라가 만든다.** WMS는 토픽을 만들지 않는다. `WarehouseAccessTopicConfig`를 삭제한다.
2. 토픽 설정의 기준은 서비스의 이벤트 계약(ADR, `docs`)이다. WMS가 요구하는 설정은 다음과 같다.
   - `dozy.wms.warehouse-access`(WMS 발행): 압축, 키 `principalId`. 파티션 3, 복제 1은 기존 기본값이다.
   - `dozy.inventory.product`(WMS 구독): 압축. 키는 상품 ID다.
   - `dozy.inventory.reservation`, `dozy.inventory.lot`(WMS 구독): 압축 여부는 이 ADR에서 정하지 않는다. WMS가 압축에 의존한다고 문서에 적은 토픽은 위 둘뿐이다.
   - `{위 구독 토픽}.dlt`: 오류 처리기가 파티션을 지정하지 않고 보낸다. 인프라가 미리 만든다.
3. `wms.warehouse-access-event.topic.name`은 발행기가 쓰므로 남기고, `topic.partitions`·`topic.replicas` 설정은 삭제한다.

## 결과 (Consequences)
- 토픽을 만드는 빈이 없어 `KafkaAdmin`이 기동 때 토픽 생성을 시도하지 않는다. 브로커에 닿지 못할 때 기동이 늦어지던 문제(`scripts/image-smoke-test.sh`의 대기 시간 주석)가 줄어들 것으로 보지만 측정하지는 않았다.
- 로컬 개발의 Kafka(compose)는 자동 생성이 켜져 있어 토픽이 기본 설정(압축 아님)으로 생긴다. 로컬에서는 압축이 필요한 경우가 없으므로 허용한다. 압축에 의존하는 동작을 로컬에서 확인해야 하면 직접 만든다.
- 인프라가 토픽을 만들어 주지 않은 환경에서는 `dozy.wms.warehouse-access`가 자동 생성에 의존한다. 운영에서는 인프라가 먼저 만든다.
- 서비스가 토픽을 만드는 `@Configuration`을 다시 추가하지 못하게 테스트로 막는다(`NoTopicCreationTest`).
