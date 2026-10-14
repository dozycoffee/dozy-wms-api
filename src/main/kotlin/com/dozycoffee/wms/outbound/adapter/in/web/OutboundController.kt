package com.dozycoffee.wms.outbound.adapter.`in`.web

import com.dozycoffee.wms.global.security.WmsAuthorize
import com.dozycoffee.wms.outbound.adapter.`in`.web.request.ReportPickingShortageRequest
import com.dozycoffee.wms.outbound.adapter.`in`.web.response.OutboundAllocationResponse
import com.dozycoffee.wms.outbound.adapter.`in`.web.response.OutboundResponse
import com.dozycoffee.wms.outbound.application.port.`in`.ApproveOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.CompleteOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.GetOutboundUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.ReportPickingShortageUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.RequestOutboundCancelUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundInspectingUseCase
import com.dozycoffee.wms.outbound.application.port.`in`.StartOutboundPickingUseCase
import com.dozycoffee.wms.outbound.domain.enumeration.OutboundStatus
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/outbounds")
class OutboundController(
    private val startOutboundPickingUseCase: StartOutboundPickingUseCase,
    private val startOutboundInspectingUseCase: StartOutboundInspectingUseCase,
    private val completeOutboundUseCase: CompleteOutboundUseCase,
    private val reportPickingShortageUseCase: ReportPickingShortageUseCase,
    private val requestOutboundCancelUseCase: RequestOutboundCancelUseCase,
    private val approveOutboundCancelUseCase: ApproveOutboundCancelUseCase,
    private val getOutboundUseCase: GetOutboundUseCase
) {

    @GetMapping("/{outboundId}")
    @PreAuthorize(WmsAuthorize.READ)
    suspend fun getById(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(getOutboundUseCase.getById(outboundId))
    }

    @GetMapping
    @PreAuthorize(WmsAuthorize.READ)
    fun getAll(@RequestParam(required = false) status: OutboundStatus?): Flow<OutboundResponse> {
        return getOutboundUseCase.getAll(status).map { OutboundResponse.from(it) }
    }

    @PatchMapping("/{outboundId}/picking")
    @PreAuthorize(WmsAuthorize.OUTBOUND)
    suspend fun startPicking(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(startOutboundPickingUseCase.startPicking(outboundId))
    }

    @PatchMapping("/{outboundId}/inspecting")
    @PreAuthorize(WmsAuthorize.OUTBOUND)
    suspend fun startInspecting(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(startOutboundInspectingUseCase.startInspecting(outboundId))
    }

    /** 피킹할 수 없는 Lot의 부족 수량을 보고해 inventory에서 다른 Lot으로 재할당받는다. 재할당 뒤의 전체 할당을 돌려준다 */
    @PostMapping("/{outboundId}/reallocation")
    @PreAuthorize(WmsAuthorize.OUTBOUND)
    suspend fun reportPickingShortage(
        @PathVariable outboundId: Long,
        @Valid @RequestBody request: ReportPickingShortageRequest
    ): List<OutboundAllocationResponse> {
        return reportPickingShortageUseCase.report(request.toCommand(outboundId)).map { OutboundAllocationResponse.from(it) }
    }

    /** OMS 또는 창고 담당자의 취소 요청. 피킹 전이면 바로 취소하고, 이후에는 관리자 승인을 기다린다 */
    @PostMapping("/{outboundId}/cancel-requests")
    @PreAuthorize(WmsAuthorize.OUTBOUND_OR_SERVICE)
    suspend fun requestCancel(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(requestOutboundCancelUseCase.requestCancel(outboundId))
    }

    /** 관리자가 상품이 원위치된 것을 확인하고 취소를 확정한다 */
    @PostMapping("/{outboundId}/cancel-approval")
    @PreAuthorize(WmsAuthorize.ADMIN)
    suspend fun approveCancel(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(approveOutboundCancelUseCase.approveCancel(outboundId))
    }

    @PatchMapping("/{outboundId}/complete")
    @PreAuthorize(WmsAuthorize.OUTBOUND)
    suspend fun complete(@PathVariable outboundId: Long): OutboundResponse {
        return OutboundResponse.from(completeOutboundUseCase.complete(outboundId))
    }
}
