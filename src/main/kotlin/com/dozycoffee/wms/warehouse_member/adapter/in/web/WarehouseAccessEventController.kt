package com.dozycoffee.wms.warehouse_member.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse_member.adapter.`in`.web.response.RepublishWarehouseAccessResponse
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RepublishWarehouseAccessUseCase
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 소속이 있는 모든 직원의 소속 이벤트를 다시 발행한다. 처음 연동하거나 inventory 사본이 어긋났을 때 쓴다(ADR-0020) */
@RestController
@RequestMapping("/api/warehouse-members")
class WarehouseAccessEventController(
    private val republishWarehouseAccessUseCase: RepublishWarehouseAccessUseCase
) {

    @PostMapping("/republish")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun republish(): RepublishWarehouseAccessResponse {
        return RepublishWarehouseAccessResponse(republishWarehouseAccessUseCase.republishAll())
    }
}
