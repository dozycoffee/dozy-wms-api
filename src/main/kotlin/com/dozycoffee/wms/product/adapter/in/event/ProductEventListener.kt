package com.dozycoffee.wms.product.adapter.`in`.event

import com.dozycoffee.wms.product.application.port.`in`.ApplyProductSnapshotUseCase
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * inventory의 상품 이벤트를 받아 상품 사본을 교체한다(ADR-0021). 한 건씩 처리하고 실패하면 예외를 던져 컨테이너의 오류 처리기가
 * 다시 시도한 뒤 `{토픽}.dlt`로 보낸다(`KafkaConsumerConfig`). 교체는 멱등이고 오래된 이벤트는 무시하므로 중복 전달에 안전하다.
 * 요청 밖 작업이라 감사 컬럼에는 `system`이 기록된다. 리스너 스레드에서 실행되므로 코루틴 경계에서 완료를 기다린다.
 * `wms.product-event.listener.enabled=false`이면 만들어지지 않는다(테스트에서 끈다).
 */
@Component
@ConditionalOnProperty(prefix = "wms.product-event.listener", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ProductEventListener(
    private val productEventParser: ProductEventParser,
    private val applyProductSnapshotUseCase: ApplyProductSnapshotUseCase
) {

    @KafkaListener(
        id = "product-event-listener",
        idIsGroup = false,
        topics = ["\${wms.product-event.listener.topic:dozy.inventory.product}"]
    )
    fun onMessage(record: ConsumerRecord<String, String>) {
        val command = productEventParser.parse(record.value())
        if (command == null) {
            log.warn("상품 사본과 무관한 이벤트 유형이라 무시했다: key={}, offset={}", record.key(), record.offset())
            return
        }
        val applied: Boolean = runBlocking { applyProductSnapshotUseCase.apply(command) }
        if (applied) {
            log.info("상품 사본을 교체했다: productId={}, status={}", command.productId, command.productStatus)
        } else {
            log.info("이미 반영한 이벤트와 같거나 오래돼 무시했다: productId={}, occurredAt={}", command.productId, command.occurredAt)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(ProductEventListener::class.java)
    }
}
