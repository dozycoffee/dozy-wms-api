package com.dozycoffee.wms.disposal.adapter.`in`.web

import com.dozycoffee.wms.disposal.adapter.`in`.web.response.DisposalItemResponse
import com.dozycoffee.wms.disposal.application.port.`in`.GetDisposalItemUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/disposal-items")
class DisposalItemController(
    private val getDisposalItemUseCase: GetDisposalItemUseCase
) {

    @GetMapping
    fun getAllByDisposal(@RequestParam disposalId: Long): Flow<DisposalItemResponse> {
        return getDisposalItemUseCase.getAllByDisposal(disposalId).map { DisposalItemResponse.from(it) }
    }
}
