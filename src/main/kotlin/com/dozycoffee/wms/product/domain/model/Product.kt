package com.dozycoffee.wms.product.domain.model

import com.dozycoffee.wms.global.common.SoftDeletableEntity
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductErrorCode

class Product private constructor(
    val productId: Long?,
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?,
    productStatus: ProductStatus
) : SoftDeletableEntity() {

    var productStatus: ProductStatus = productStatus
        private set

    companion object {

        fun create(
            productCode: String?,
            productName: String?,
            category: ProductCategory?,
            unit: String?,
            shelfLifeDays: Int?
        ): Product {
            val validProductCode: String = validateProductCode(productCode)
            val validProductName: String = validateProductName(productName)
            val validCategory: ProductCategory = validateCategory(category)
            val validUnit: String = validateUnit(unit)
            validateShelfLifeDays(shelfLifeDays)
            return Product(
                productId = null,
                productCode = validProductCode,
                productName = validProductName,
                category = validCategory,
                unit = validUnit,
                shelfLifeDays = shelfLifeDays,
                productStatus = ProductStatus.ACTIVE
            )
        }

        fun reconstitute(
            productId: Long,
            productCode: String,
            productName: String,
            category: ProductCategory,
            unit: String,
            shelfLifeDays: Int?,
            productStatus: ProductStatus
        ): Product {
            return Product(
                productId = productId,
                productCode = productCode,
                productName = productName,
                category = category,
                unit = unit,
                shelfLifeDays = shelfLifeDays,
                productStatus = productStatus
            )
        }

        private fun validateProductCode(productCode: String?): String {
            if (productCode.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_CODE)
            }
            return productCode
        }

        private fun validateProductName(productName: String?): String {
            if (productName.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_NAME)
            }
            return productName
        }

        private fun validateCategory(category: ProductCategory?): ProductCategory {
            if (category == null) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_CATEGORY)
            }
            return category
        }

        private fun validateUnit(unit: String?): String {
            if (unit.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_UNIT)
            }
            return unit
        }

        private fun validateShelfLifeDays(shelfLifeDays: Int?) {
            if (shelfLifeDays != null && shelfLifeDays < 0) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_SHELF_LIFE_DAYS)
            }
        }
    }

    fun activate() {
        productStatus = ProductStatus.ACTIVE
    }

    fun deactivate() {
        productStatus = ProductStatus.INACTIVE
    }

    fun delete(actor: String) {
        softDelete(actor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Product) return false
        val id: Long? = productId
        return id != null && id == other.productId
    }

    override fun hashCode(): Int {
        return productId?.hashCode() ?: System.identityHashCode(this)
    }
}
