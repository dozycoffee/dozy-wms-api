package com.dozycoffee.wms.global.event

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

/**
 * MSA 공통 이벤트 형식(envelope)이다. 값이 `null`인 필드는 JSON에서 생략한다. [data]는 이벤트 유형마다 정해진 본문이며,
 * `null`을 생략하려면 본문 클래스에도 `@JsonInclude(NON_NULL)`을 붙인다.
 * 같은 값이 Kafka 헤더([EventHeaders])에도 실린다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventEnvelope<T : Any>(
    val eventId: String,
    val eventType: String,
    val eventVersion: Int,
    val occurredAt: OffsetDateTime,
    val source: String,
    val data: T
) {
    companion object {
        const val SOURCE_WMS: String = "wms"
    }
}

object EventHeaders {
    const val EVENT_ID: String = "event-id"
    const val EVENT_TYPE: String = "event-type"
    const val EVENT_VERSION: String = "event-version"
    const val SOURCE: String = "source"
    const val AGGREGATE_TYPE: String = "aggregate-type"
    const val AGGREGATE_ID: String = "aggregate-id"
    const val TRACEPARENT: String = "traceparent"
}
