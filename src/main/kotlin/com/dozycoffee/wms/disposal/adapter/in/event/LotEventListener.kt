package com.dozycoffee.wms.disposal.adapter.`in`.event

import com.dozycoffee.wms.disposal.application.port.`in`.RegisterExpiredLotDisposalUseCase
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * inventory의 Lot 이벤트에서 경과(`LOT_EXPIRED`)를 받아 폐기 신청을 만든다(ADR-0024). 한 건씩 처리하고 실패하면 예외를 던져
 * 컨테이너의 오류 처리기가 다시 시도한 뒤 `{토픽}.dlt`로 보낸다(`KafkaConsumerConfig`). 이벤트 ID로 중복을 거르므로 중복 전달에 안전하다.
 * 요청 밖 작업이라 감사 컬럼에는 `system`이 기록된다. `wms.lot-event.listener.enabled=false`이면 만들어지지 않는다(테스트에서 끈다).
 */
@Component
@ConditionalOnProperty(prefix = "wms.lot-event.listener", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class LotEventListener(
    private val lotEventParser: LotEventParser,
    private val registerExpiredLotDisposalUseCase: RegisterExpiredLotDisposalUseCase
) {

    @KafkaListener(
        id = "lot-event-listener",
        idIsGroup = false,
        topics = ["\${wms.lot-event.listener.topic:dozy.inventory.lot}"]
    )
    fun onMessage(record: ConsumerRecord<String, String>) {
        val command = lotEventParser.parse(record.value())
        if (command == null) {
            log.debug("폐기와 무관한 Lot 이벤트라 무시했다: key={}, offset={}", record.key(), record.offset())
            return
        }
        val created: Int = runBlocking { registerExpiredLotDisposalUseCase.register(command) }
        log.info("Lot 경과 이벤트로 폐기 신청 {}건을 만들었다: eventId={}, lotNumber={}", created, command.eventId, command.lotNumber)
    }

    private companion object {
        val log = LoggerFactory.getLogger(LotEventListener::class.java)
    }
}
