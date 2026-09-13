package com.dozycoffee.wms.product.adapter.`in`.web.request

import com.dozycoffee.wms.product.application.port.`in`.command.RegisterProductCommand
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class RegisterProductRequest(
    @field:NotBlank val productCode: String?,
    @field:NotBlank val productName: String?,
    @field:NotNull val category: ProductCategory?,
    @field:NotBlank val unit: String?,
    @field:Min(0) val shelfLifeDays: Int?
) {
    fun toCommand(): RegisterProductCommand {
        return RegisterProductCommand(
            requireNotNull(productCode),
            requireNotNull(productName),
            requireNotNull(category),
            requireNotNull(unit),
            shelfLifeDays
        )
    }
}
