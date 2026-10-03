package com.dozycoffee.wms.warehouse.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.AmountRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.request.RegisterWorkAreaRequest
import com.dozycoffee.wms.warehouse.adapter.`in`.web.response.WorkAreaResponse
import com.dozycoffee.wms.warehouse.application.port.`in`.GetWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.OccupyWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.RegisterWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.ReleaseWorkAreaUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.command.OccupyWorkAreaCommand
import com.dozycoffee.wms.warehouse.application.port.`in`.command.ReleaseWorkAreaCommand
import com.dozycoffee.wms.warehouse.domain.enumeration.AreaCode
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class WorkAreaController(
    private val registerWorkAreaUseCase: RegisterWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase
) {

    @PostMapping("/api/warehouses/{warehouseId}/work-areas")
    @PreAuthorize(WmsAuthorize.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(
        @PathVariable warehouseId: Long,
        @Valid @RequestBody request: RegisterWorkAreaRequest
    ): WorkAreaResponse {
        return WorkAreaResponse.from(registerWorkAreaUseCase.register(request.toCommand(warehouseId)))
    }

    @GetMapping("/api/work-areas/{workAreaId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable workAreaId: Long): WorkAreaResponse {
        return WorkAreaResponse.from(getWorkAreaUseCase.getById(workAreaId))
    }

    @GetMapping("/api/warehouses/{warehouseId}/work-areas/{areaCode}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getByWarehouseIdAndAreaCode(
        @PathVariable warehouseId: Long,
        @PathVariable areaCode: AreaCode
    ): WorkAreaResponse {
        return WorkAreaResponse.from(getWorkAreaUseCase.getByWarehouseIdAndAreaCode(warehouseId, areaCode))
    }

    @PatchMapping("/api/work-areas/{workAreaId}/occupy")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun occupy(@PathVariable workAreaId: Long, @Valid @RequestBody request: AmountRequest): WorkAreaResponse {
        return WorkAreaResponse.from(occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workAreaId, request.amount)))
    }

    @PatchMapping("/api/work-areas/{workAreaId}/release")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun release(@PathVariable workAreaId: Long, @Valid @RequestBody request: AmountRequest): WorkAreaResponse {
        return WorkAreaResponse.from(releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workAreaId, request.amount)))
    }
}
