package com.dozycoffee.wms.warehouse_member.adapter.out.event

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/**
 * 직원 창고 소속 토픽을 로그 압축으로 만든다. 키가 `principalId`라 직원마다 최신 상태만 보관되어, inventory의 새 컨슈머 그룹이 처음부터 읽어도
 * 사본이 채워진다(ADR-0020). 운영에서 인프라가 토픽을 미리 만들었다면 이미 있는 토픽은 그대로 둔다. 발행기를 끄면(테스트) 만들지 않는다.
 */
@Configuration
@ConditionalOnProperty(prefix = "wms.warehouse-access-event.publisher", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class WarehouseAccessTopicConfig {

    @Bean
    fun warehouseAccessTopic(
        @Value("\${wms.warehouse-access-event.topic.name:dozy.wms.warehouse-access}") name: String,
        @Value("\${wms.warehouse-access-event.topic.partitions:3}") partitions: Int,
        @Value("\${wms.warehouse-access-event.topic.replicas:1}") replicas: Int
    ): NewTopic {
        return TopicBuilder.name(name)
            .partitions(partitions)
            .replicas(replicas)
            .config("cleanup.policy", "compact")
            .build()
    }
}
