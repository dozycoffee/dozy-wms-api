package com.dozycoffee.wms.global.event

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

/**
 * MSA 공통 이벤트 형식(envelope)이다. 값이 `null`인 필드는 JSON에서 생략한다. [data]는 이벤트 유형마다 정해진 본문이며,
 * `null`을 생략하려면 본문 클래스에도 `@JsonInclude(NON_NULL)`을 붙인다.
 * 메타데이터는 값에만 있고 Kafka 헤더는 쓰지 않는다(ADR-0022). [traceparent]는 선택 필드이며 요청 안에서 만든 이벤트에만 싣고, 없으면 보내지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EventEnvelope<T : Any>(
    val eventId: String,
    val eventType: String,
    val eventVersion: Int,
    val occurredAt: OffsetDateTime,
    val source: String,
    val traceparent: String? = null,
    val data: T
) {
    companion object {
        const val SOURCE_WMS: String = "wms"
    }
}
