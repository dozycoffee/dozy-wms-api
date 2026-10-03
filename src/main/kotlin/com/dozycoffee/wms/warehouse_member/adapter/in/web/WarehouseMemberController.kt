package com.dozycoffee.wms.warehouse_member.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse_member.adapter.`in`.web.request.AssignWarehouseMemberRequest
import com.dozycoffee.wms.warehouse_member.adapter.`in`.web.response.WarehouseMemberResponse
import com.dozycoffee.wms.warehouse_member.application.port.`in`.AssignWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.GetWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RemoveWarehouseMemberUseCase
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/warehouses/{warehouseId}/members")
class WarehouseMemberController(
    private val assignWarehouseMemberUseCase: AssignWarehouseMemberUseCase,
    private val removeWarehouseMemberUseCase: RemoveWarehouseMemberUseCase,
    private val getWarehouseMemberUseCase: GetWarehouseMemberUseCase
) {

    @PostMapping
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun assign(
        @PathVariable warehouseId: Long,
        @Valid @RequestBody request: AssignWarehouseMemberRequest
    ): WarehouseMemberResponse {
        return WarehouseMemberResponse.from(
            assignWarehouseMemberUseCase.assign(warehouseId, requireNotNull(request.principalId))
        )
    }

    @GetMapping
    @PreAuthorize(WmsAuthorize.ADMIN)
    fun getAll(@PathVariable warehouseId: Long): Flow<WarehouseMemberResponse> {
        return getWarehouseMemberUseCase.getAllByWarehouse(warehouseId).map { WarehouseMemberResponse.from(it) }
    }

    @DeleteMapping("/{principalId}")
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun remove(@PathVariable warehouseId: Long, @PathVariable principalId: UUID) {
        removeWarehouseMemberUseCase.remove(warehouseId, principalId)
    }
}
