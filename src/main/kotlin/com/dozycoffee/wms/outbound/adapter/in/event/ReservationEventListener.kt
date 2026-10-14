package com.dozycoffee.wms.outbound.adapter.`in`.event

import com.dozycoffee.wms.outbound.application.port.`in`.CreateOutboundFromReservationUseCase
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * inventory의 예약 이벤트에서 확정(`RESERVATION_CONFIRMED`)을 받아 출고를 만든다(ADR-0017 결정 3). 한 건씩 처리하고 실패하면 예외를 던져
 * 컨테이너의 오류 처리기가 다시 시도한 뒤 `{토픽}.dlt`로 보낸다(`KafkaConsumerConfig`). 같은 예약은 한 번만 만들므로 중복 전달에 안전하다.
 * 요청 밖 작업이라 감사 컬럼에는 `system`이 기록된다. 리스너 스레드에서 실행되므로 코루틴 경계에서 완료를 기다린다.
 * `wms.reservation-event.listener.enabled=false`이면 만들어지지 않는다(테스트에서 끈다).
 */
@Component
@ConditionalOnProperty(prefix = "wms.reservation-event.listener", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ReservationEventListener(
    private val reservationEventParser: ReservationEventParser,
    private val createOutboundFromReservationUseCase: CreateOutboundFromReservationUseCase
) {

    @KafkaListener(
        id = "reservation-event-listener",
        idIsGroup = false,
        topics = ["\${wms.reservation-event.listener.topic:dozy.inventory.reservation}"]
    )
    fun onMessage(record: ConsumerRecord<String, String>) {
        val command = reservationEventParser.parse(record.value())
        if (command == null) {
            log.debug("출고와 무관한 예약 이벤트라 무시했다: key={}, offset={}", record.key(), record.offset())
            return
        }
        val created: Boolean = runBlocking { createOutboundFromReservationUseCase.create(command) }
        if (created) {
            log.info("예약 확정으로 출고를 만들었다: warehouseId={}, reservationId={}", command.warehouseId, command.reservationId)
        } else {
            log.info("이미 출고를 만든 예약이라 무시했다: warehouseId={}, reservationId={}", command.warehouseId, command.reservationId)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ReservationEventListener::class.java)
    }
}
