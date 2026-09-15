package com.dozycoffee.wms.warehouse.adapter.`in`.web

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class WorkAreaController(
    private val registerWorkAreaUseCase: RegisterWorkAreaUseCase,
    private val occupyWorkAreaUseCase: OccupyWorkAreaUseCase,
    private val releaseWorkAreaUseCase: ReleaseWorkAreaUseCase,
    private val getWorkAreaUseCase: GetWorkAreaUseCase
) {

    @PostMapping("/api/warehouses/{warehouseId}/work-areas")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @PathVariable warehouseId: Long,
        @Valid @RequestBody request: RegisterWorkAreaRequest
    ): Mono<WorkAreaResponse> {
        return registerWorkAreaUseCase.register(request.toCommand(warehouseId)).map { WorkAreaResponse.from(it) }
    }

    @GetMapping("/api/work-areas/{workAreaId}")
    fun getById(@PathVariable workAreaId: Long): Mono<WorkAreaResponse> {
        return getWorkAreaUseCase.getById(workAreaId).map { WorkAreaResponse.from(it) }
    }

    @GetMapping("/api/warehouses/{warehouseId}/work-areas/{areaCode}")
    fun getByWarehouseIdAndAreaCode(
        @PathVariable warehouseId: Long,
        @PathVariable areaCode: AreaCode
    ): Mono<WorkAreaResponse> {
        return getWorkAreaUseCase.getByWarehouseIdAndAreaCode(warehouseId, areaCode).map { WorkAreaResponse.from(it) }
    }

    @PatchMapping("/api/work-areas/{workAreaId}/occupy")
    fun occupy(@PathVariable workAreaId: Long, @Valid @RequestBody request: AmountRequest): Mono<WorkAreaResponse> {
        return occupyWorkAreaUseCase.occupy(OccupyWorkAreaCommand(workAreaId, request.amount))
            .map { WorkAreaResponse.from(it) }
    }

    @PatchMapping("/api/work-areas/{workAreaId}/release")
    fun release(@PathVariable workAreaId: Long, @Valid @RequestBody request: AmountRequest): Mono<WorkAreaResponse> {
        return releaseWorkAreaUseCase.release(ReleaseWorkAreaCommand(workAreaId, request.amount))
            .map { WorkAreaResponse.from(it) }
    }
}
