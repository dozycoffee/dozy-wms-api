package com.dozycoffee.wms.global.config

import org.apache.kafka.clients.admin.NewTopic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.kafka.core.KafkaAdmin

/** 토픽은 인프라가 만든다(ADR-0026). 서비스 코드가 토픽을 만드는 빈을 다시 두지 못하게 막는다 */
class NoTopicCreationTest {

    @Test
    fun `서비스는 토픽을 만드는 빈을 등록하지 않는다`() {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean = true
        }
        scanner.addIncludeFilter(AnnotationTypeFilter(Configuration::class.java))

        val topicBeans: List<String> = scanner.findCandidateComponents("com.dozycoffee.wms")
            .mapNotNull(BeanDefinition::getBeanClassName)
            .map { Class.forName(it) }
            .flatMap { type ->
                type.declaredMethods
                    .filter { it.isAnnotationPresent(Bean::class.java) }
                    .filter { it.returnType == NewTopic::class.java || it.returnType == KafkaAdmin.NewTopics::class.java }
                    .map { "${type.simpleName}.${it.name}" }
            }

        assertThat(topicBeans).isEmpty()
    }
}
