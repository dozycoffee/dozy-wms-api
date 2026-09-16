package com.dozycoffee.wms.return_request.adapter.`in`.web

import com.dozycoffee.wms.return_request.adapter.`in`.web.request.InspectReturnItemRequest
import com.dozycoffee.wms.return_request.adapter.`in`.web.response.ReturnItemResponse
import com.dozycoffee.wms.return_request.application.port.`in`.GetReturnItemUseCase
import com.dozycoffee.wms.return_request.application.port.`in`.InspectReturnItemUseCase
import jakarta.validation.Valid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/return-items")
class ReturnItemController(
    private val inspectReturnItemUseCase: InspectReturnItemUseCase,
    private val getReturnItemUseCase: GetReturnItemUseCase
) {

    @GetMapping
    fun getAllByReturnRequest(@RequestParam returnRequestId: Long): Flow<ReturnItemResponse> {
        return getReturnItemUseCase.getAllByReturnRequest(returnRequestId).map { ReturnItemResponse.from(it) }
    }

    @PatchMapping("/{returnItemId}/inspect")
    suspend fun inspect(
        @PathVariable returnItemId: Long,
        @Valid @RequestBody request: InspectReturnItemRequest
    ): ReturnItemResponse {
        return ReturnItemResponse.from(inspectReturnItemUseCase.inspect(request.toCommand(returnItemId)))
    }
}
