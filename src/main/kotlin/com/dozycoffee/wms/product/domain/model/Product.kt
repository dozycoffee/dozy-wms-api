package com.dozycoffee.wms.product.domain.model

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductErrorCode
import java.time.LocalDateTime

/**
 * inventory 서비스가 소유한 상품 마스터의 로컬 사본이다(ADR-0021). WMS는 상품을 만들거나 바꾸지 않고, inventory가 보낸 변경 후 스냅샷으로
 * 통째로 교체한다. [sourceOccurredAt]은 마지막으로 반영한 이벤트의 발생 시각이다.
 */
class Product private constructor(
    val productId: Long,
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?,
    val productStatus: ProductStatus,
    val sourceOccurredAt: LocalDateTime?
) : BaseEntity() {

    /** 이미 반영한 이벤트와 같거나 더 오래된 이벤트인지 본다. 중복 전달과 순서가 바뀐 전달을 걸러 낸다 */
    fun isStale(occurredAt: LocalDateTime): Boolean {
        val applied: LocalDateTime = sourceOccurredAt ?: return false
        return !occurredAt.isAfter(applied)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Product) return false
        return productId == other.productId
    }

    override fun hashCode(): Int = productId.hashCode()

    companion object {

        fun of(
            productId: Long,
            productCode: String?,
            productName: String?,
            category: ProductCategory,
            unit: String?,
            shelfLifeDays: Int?,
            productStatus: ProductStatus,
            sourceOccurredAt: LocalDateTime?
        ): Product {
            if (productId <= 0) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_ID)
            }
            if (productCode.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_CODE)
            }
            if (productName.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_NAME)
            }
            if (unit.isNullOrBlank()) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_UNIT)
            }
            if (shelfLifeDays != null && shelfLifeDays < 0) {
                throw InvalidDomainValueException(ProductErrorCode.INVALID_SHELF_LIFE_DAYS)
            }
            return Product(productId, productCode, productName, category, unit, shelfLifeDays, productStatus, sourceOccurredAt)
        }

        fun reconstitute(
            productId: Long,
            productCode: String,
            productName: String,
            category: ProductCategory,
            unit: String,
            shelfLifeDays: Int?,
            productStatus: ProductStatus,
            sourceOccurredAt: LocalDateTime?
        ): Product {
            return Product(productId, productCode, productName, category, unit, shelfLifeDays, productStatus, sourceOccurredAt)
        }
    }
}
