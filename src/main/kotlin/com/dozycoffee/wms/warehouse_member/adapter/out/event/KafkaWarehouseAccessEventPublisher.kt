package com.dozycoffee.wms.warehouse_member.adapter.out.event

import com.dozycoffee.wms.global.event.EventEnvelope
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventPublisher
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.future.await
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * `WAREHOUSE_ACCESS_CHANGED`를 공통 envelope으로 Kafka에 보낸다(ADR-0020, ADR-0022). 메시지 키는 `principalId`라 같은 직원의 이벤트는 순서가 지켜지고,
 * 메타데이터는 값에만 있고 Kafka 헤더는 쓰지 않는다. 브로커 확인까지 기다린다.
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
            ProducerRecord(topic, null, principalId.toString(), jsonMapper.writeValueAsString(envelope))
        kafkaTemplate.send(record).await()
    }

    /** inventory 수신부가 읽는 `data` 본문이다. 필드 이름을 바꾸지 않는다(inventory events.md) */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class WarehouseAccessChangedData(
        val principalId: String,
        val warehouseIds: List<Long>
    )

    companion object {
        const val EVENT_TYPE: String = "WAREHOUSE_ACCESS_CHANGED"
        const val EVENT_VERSION: Int = 1
    }
}
