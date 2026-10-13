package com.dozycoffee.wms.support

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.testcontainers.mysql.MySQLContainer

class MySqlTestContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val mysql: MySQLContainer = container
        TestPropertyValues.of(
            "spring.r2dbc.url=r2dbc:mysql://${mysql.host}:${mysql.firstMappedPort}/$DATABASE_NAME?useSSL=false&serverTimezone=Asia/Seoul",
            "spring.r2dbc.username=root",
            "spring.r2dbc.password=${mysql.password}",
            "spring.flyway.url=${mysql.jdbcUrl}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul",
            "spring.flyway.user=root",
            "spring.flyway.password=${mysql.password}",
        ).applyTo(applicationContext.environment)

        // 외부 연결이 필요한 백그라운드 작업은 테스트에서 기본으로 끈다. 개별 테스트가 @DynamicPropertySource나 properties로 다시 켤 수 있도록
        // 테스트가 넣는 속성보다 낮고 application.yaml보다 높은 우선순위에 둔다.
        applicationContext.environment.propertySources.addBefore(
            "systemProperties",
            MapPropertySource(
                "testDefaults",
                mapOf(
                    "wms.inventory-call.dispatch.enabled" to "false",
                    "wms.warehouse-access-event.publisher.enabled" to "false",
                    "wms.product-event.listener.enabled" to "false"
                )
            )
        )
    }

    private companion object {
        const val DATABASE_NAME: String = "dozy_wms"

        // 테스트 JVM당 1회만 기동하고 JVM 종료 시 Ryuk가 정리한다
        val container: MySQLContainer by lazy {
            MySQLContainer("mysql:8.0")
                .withDatabaseName(DATABASE_NAME)
                .withEnv("TZ", "Asia/Seoul")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
                .also { it.start() }
        }
    }
}
