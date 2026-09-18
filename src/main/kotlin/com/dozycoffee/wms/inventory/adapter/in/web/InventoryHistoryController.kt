package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.inventory.adapter.`in`.web.response.InventoryHistoryResponse
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryHistoryUseCase
import com.dozycoffee.wms.inventory.domain.enumeration.InventoryHistoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/inventory-histories")
class InventoryHistoryController(
    private val getInventoryHistoryUseCase: GetInventoryHistoryUseCase
) {

    @GetMapping
    fun getAll(
        @RequestParam(required = false) inventoryId: Long?,
        @RequestParam(required = false) historyType: InventoryHistoryType?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): Flow<InventoryHistoryResponse> {
        return getInventoryHistoryUseCase.getAll(inventoryId, historyType, from, to)
            .map { InventoryHistoryResponse.from(it) }
    }
}
