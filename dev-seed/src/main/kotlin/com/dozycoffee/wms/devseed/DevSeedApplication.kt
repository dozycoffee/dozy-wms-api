package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.DozyWmsApiApplication
import org.springframework.boot.SpringApplication
import kotlin.system.exitProcess

/**
 * 개발 DB에 목 데이터를 적재하고 종료하는 일회성 실행 진입점이다. 앱 컨텍스트를 `local` 프로파일로 띄우되 서버는 임의 포트로 열고,
 * 스케줄러와 외부 발행기는 끈다. 이미 데이터가 있으면 [DevSeedRunner]가 건너뛴다.
 */
fun main(args: Array<String>) {
    val application = SpringApplication(DozyWmsApiApplication::class.java)
    application.setAdditionalProfiles("local")
    application.setDefaultProperties(
        mapOf(
            "server.port" to "0",
            "spring.main.banner-mode" to "off",
            "wms.inventory-call.dispatch.enabled" to "false",
            "wms.warehouse-access-event.publisher.enabled" to "false"
        )
    )
    exitProcess(SpringApplication.exit(application.run(*args)))
}
