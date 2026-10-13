package com.dozycoffee.wms.warehouse_member.adapter.out.event

import com.dozycoffee.wms.global.event.EventEnvelope
import com.dozycoffee.wms.global.event.EventHeaders
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventPublisher
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.future.await
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * `WAREHOUSE_ACCESS_CHANGED`를 공통 envelope으로 Kafka에 보낸다(ADR-0020). 메시지 키는 `principalId`라 같은 직원의 이벤트는 순서가 지켜지고,
 * 이벤트 ID와 유형 등은 헤더로도 싣는다. 브로커 확인까지 기다린다.
 */
@Component
class KafkaWarehouseAccessEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val jsonMapper: JsonMapper,
    private val clock: Clock,
    @Value("\${wms.warehouse-access-event.topic.name:dozy.wms.warehouse-access}") private val topic: String
) : WarehouseAccessEventPublisher {

    override suspend fun publish(principalId: UUID, eventId: String, occurredAt: LocalDateTime, warehouseIds: List<Long>) {
        val envelope = EventEnvelope(
            eventId = eventId,
            eventType = EVENT_TYPE,
            eventVersion = EVENT_VERSION,
            occurredAt = occurredAt.atZone(clock.zone).toOffsetDateTime(),
            source = EventEnvelope.SOURCE_WMS,
            data = WarehouseAccessChangedData(principalId.toString(), warehouseIds)
        )
        val record: ProducerRecord<String, String> =
            ProducerRecord(topic, null, principalId.toString(), jsonMapper.writeValueAsString(envelope)).apply {
                headers().add(EventHeaders.EVENT_ID, bytes(eventId))
                headers().add(EventHeaders.EVENT_TYPE, bytes(EVENT_TYPE))
                headers().add(EventHeaders.EVENT_VERSION, bytes(EVENT_VERSION.toString()))
                headers().add(EventHeaders.SOURCE, bytes(EventEnvelope.SOURCE_WMS))
                headers().add(EventHeaders.AGGREGATE_TYPE, bytes(AGGREGATE_TYPE))
                headers().add(EventHeaders.AGGREGATE_ID, bytes(principalId.toString()))
            }
        kafkaTemplate.send(record).await()
    }

    private fun bytes(value: String): ByteArray = value.toByteArray(StandardCharsets.UTF_8)

    /** inventory 수신부가 읽는 `data` 본문이다. 필드 이름을 바꾸지 않는다(inventory events.md) */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class WarehouseAccessChangedData(
        val principalId: String,
        val warehouseIds: List<Long>
    )

    companion object {
        const val EVENT_TYPE: String = "WAREHOUSE_ACCESS_CHANGED"
        const val EVENT_VERSION: Int = 1
        const val AGGREGATE_TYPE: String = "WAREHOUSE_ACCESS"
    }
}
