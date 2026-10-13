# ADR-0022: 이벤트 메타데이터를 Kafka 헤더 없이 값(JSON)에만 담는다

## 상태
Accepted. ADR-0020 결정 1의 Kafka 헤더 부분을 대체한다. 나머지(토픽, 메시지 키, envelope 구조, `eventId` 고정 규칙, 발행 방식)는 ADR-0020을 그대로 따른다.

## 배경 (Context)
MSA 공통 이벤트 형식이 바뀌었다(inventory ADR-0039). ADR-0038(공통 envelope) 이후 `eventId`, `eventType`, `eventVersion`, `source`가 값과 Kafka 헤더에 같은 값으로 중복되고
`aggregate-type`, `aggregate-id`, `traceparent`는 헤더에만 있었다. 같은 정보를 두 곳에 싣고 어긋나지 않게 유지하는 비용이 있고, 헤더를 읽는 구독자는 없었다.
WMS의 `WAREHOUSE_ACCESS_CHANGED`(ADR-0020)는 헤더 6종(`event-id`, `event-type`, `event-version`, `source`, `aggregate-type`, `aggregate-id`)을 싣고 있었다.

## 결정 (Decision)
1. **메타데이터는 값(JSON)에만 둔다. Kafka 헤더는 쓰지 않는다.** 헤더 6종을 모두 제거한다. 구독자는 값의 `eventId`로 중복을 거른다.
2. **`aggregate-type`/`aggregate-id`는 보내지 않는다.** 집계는 토픽 이름(`dozy.wms.warehouse-access`)과 `eventType` 접두사로, 집계 ID는 `data.principalId`로 알 수 있다.
3. **`traceparent`는 값 최상위의 선택 필드다**(W3C Trace Context). `EventEnvelope`가 필드를 지원하고 값이 없으면 직렬화에서 생략한다. WMS에는 분산 추적 체계가 없어(자체 `X-Trace-Id`만 있음)
   당분간 보내지 않는다. 요청의 추적 컨텍스트를 발행 대기 행에 저장해 싣는 일은 WMS가 추적 체계를 도입할 때 정한다.
4. **메시지 키와 토픽, `data`는 그대로다.** `eventVersion`을 올리지 않는다(`data`와 키가 같고 헤더는 계약에 없었던 부가 정보다).
5. **소비 쪽은 바뀌지 않는다.** 상품 이벤트 소비자(`ProductEventParser`, ADR-0021)는 이미 값만 읽고 모르는 필드(`traceparent`)를 무시한다.

## 결과 (Consequences)
- 얻는 것: 한 정보를 한 곳에만 싣는다. 구독자가 값 하나로 메타데이터와 본문을 모두 얻고, 메시지를 복사하거나 로그로 남겨도 정보가 빠지지 않는다. inventory와 같은 형식이 된다.
- 감수하는 것: 값을 파싱하지 않는 일반 Kafka 도구는 `eventId`·`eventType`으로 필터링할 수 없다. WMS 이벤트에는 당분간 `traceparent`가 없어 서비스 간 추적이 이어지지 않는다.
