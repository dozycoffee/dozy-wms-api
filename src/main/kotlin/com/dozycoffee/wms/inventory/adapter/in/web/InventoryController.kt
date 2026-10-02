package com.dozycoffee.wms.inventory.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.inventory.adapter.`in`.web.request.RegisterInventoryRequest
import com.dozycoffee.wms.inventory.adapter.`in`.web.response.InventoryDetailResponse
import com.dozycoffee.wms.inventory.adapter.`in`.web.response.InventoryResponse
import com.dozycoffee.wms.inventory.adapter.`in`.web.response.ZoneInventorySummaryResponse
import com.dozycoffee.wms.inventory.application.port.`in`.GetInventoryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.GetZoneInventorySummaryUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.InventorySortBy
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDefectiveUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.MarkInventoryDisposalScheduledUseCase
import com.dozycoffee.wms.inventory.application.port.`in`.RegisterInventoryUseCase
import com.dozycoffee.wms.inventory.domain.enumeration.QualityStatus
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inventories")
class InventoryController(
    private val registerInventoryUseCase: RegisterInventoryUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getZoneInventorySummaryUseCase: GetZoneInventorySummaryUseCase,
    private val markInventoryDefectiveUseCase: MarkInventoryDefectiveUseCase,
    private val markInventoryDisposalScheduledUseCase: MarkInventoryDisposalScheduledUseCase
) {

    @PostMapping
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@Valid @RequestBody request: RegisterInventoryRequest): InventoryResponse {
        return InventoryResponse.from(registerInventoryUseCase.register(request.toCommand()))
    }

    @GetMapping("/zone-summary")
    @PreAuthorize(WmsAuthorize.READ)
    fun getZoneSummary(
        @RequestParam(required = false) warehouseIds: List<Long>?
    ): Flow<ZoneInventorySummaryResponse> {
        return getZoneInventorySummaryUseCase.getAll(warehouseIds).map { ZoneInventorySummaryResponse.from(it) }
    }

    @GetMapping("/{inventoryId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable inventoryId: Long): InventoryDetailResponse {
        return InventoryDetailResponse.from(getInventoryUseCase.getDetailById(inventoryId))
    }

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    fun getAll(
        @RequestParam(required = false) locationId: Long?,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(required = false) qualityStatus: QualityStatus?,
        @RequestParam(required = false) sortBy: InventorySortBy?,
        @RequestParam(required = false) warehouseIds: List<Long>?
    ): Flow<InventoryResponse> {
        return getInventoryUseCase.getAll(locationId, productId, qualityStatus, sortBy, warehouseIds)
            .map { InventoryResponse.from(it) }
    }

    @PatchMapping("/{inventoryId}/mark-defective")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun markDefective(@PathVariable inventoryId: Long): InventoryResponse {
        return InventoryResponse.from(markInventoryDefectiveUseCase.markDefective(inventoryId))
    }

    @PatchMapping("/{inventoryId}/mark-disposal-scheduled")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun markDisposalScheduled(@PathVariable inventoryId: Long): InventoryResponse {
        return InventoryResponse.from(markInventoryDisposalScheduledUseCase.markDisposalScheduled(inventoryId))
    }
}
