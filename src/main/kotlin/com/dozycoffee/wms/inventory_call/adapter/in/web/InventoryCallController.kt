package com.dozycoffee.wms.inventory_call.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.inventory_call.adapter.`in`.web.response.InventoryCallResponse
import com.dozycoffee.wms.inventory_call.adapter.`in`.web.response.InventoryCallSummaryResponse
import com.dozycoffee.wms.inventory_call.application.port.`in`.DiscardInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.GetInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.RequeueInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** inventory 호출 아웃박스의 운영 API. 격리(DEAD)된 호출을 확인하고 재전송하거나 버린다(ADR-0019) */
@RestController
@RequestMapping("/api/inventory-calls")
@PreAuthorize(WmsAuthorize.ADMIN)
class InventoryCallController(
    private val getInventoryCallUseCase: GetInventoryCallUseCase,
    private val requeueInventoryCallUseCase: RequeueInventoryCallUseCase,
    private val discardInventoryCallUseCase: DiscardInventoryCallUseCase
) {

    @GetMapping
    fun getAll(
        @RequestParam(required = false) status: InventoryCallStatus?,
        @RequestParam(defaultValue = "100") limit: Int
    ): Flow<InventoryCallSummaryResponse> {
        return getInventoryCallUseCase.getAll(status, limit.coerceIn(1, MAX_LIMIT)).map { InventoryCallSummaryResponse.from(it) }
    }

    @GetMapping("/{inventoryCallId}")
    suspend fun getById(@PathVariable inventoryCallId: Long): InventoryCallResponse {
        return InventoryCallResponse.from(getInventoryCallUseCase.getById(inventoryCallId))
    }

    @PostMapping("/{inventoryCallId}/requeue")
    suspend fun requeue(@PathVariable inventoryCallId: Long): InventoryCallResponse {
        return InventoryCallResponse.from(requeueInventoryCallUseCase.requeue(inventoryCallId))
    }

    @PostMapping("/{inventoryCallId}/discard")
    suspend fun discard(@PathVariable inventoryCallId: Long): InventoryCallResponse {
        return InventoryCallResponse.from(discardInventoryCallUseCase.discard(inventoryCallId))
    }

    companion object {
        private const val MAX_LIMIT = 500
    }
}
