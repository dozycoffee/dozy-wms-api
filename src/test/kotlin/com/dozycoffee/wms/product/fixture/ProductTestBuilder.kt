package com.dozycoffee.wms.product.fixture

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

class ProductTestBuilder {

    private var productId: Long = nextProductId.incrementAndGet()
    private var productCode: String? = "PRD-0001"
    private var productName: String? = "콜롬비아 원두"
    private var category: ProductCategory? = ProductCategory.BEAN
    private var unit: String? = "KG"
    private var shelfLifeDays: Int? = 365
    private var productStatus: ProductStatus = ProductStatus.ACTIVE
    private var sourceOccurredAt: LocalDateTime? = null

    companion object {
        /** 상품 ID는 inventory가 정한 값이므로 테스트도 저장소가 채번하지 않고 빌더가 유일한 값을 부여한다 */
        private val nextProductId: AtomicLong = AtomicLong(1000)

        fun product(): ProductTestBuilder = ProductTestBuilder()
    }

    fun productId(productId: Long): ProductTestBuilder {
        this.productId = productId
        return this
    }

    fun productCode(productCode: String?): ProductTestBuilder {
        this.productCode = productCode
        return this
    }

    fun productName(productName: String?): ProductTestBuilder {
        this.productName = productName
        return this
    }

    fun category(category: ProductCategory?): ProductTestBuilder {
        this.category = category
        return this
    }

    fun unit(unit: String?): ProductTestBuilder {
        this.unit = unit
        return this
    }

    fun shelfLifeDays(shelfLifeDays: Int?): ProductTestBuilder {
        this.shelfLifeDays = shelfLifeDays
        return this
    }

    fun productStatus(productStatus: ProductStatus): ProductTestBuilder {
        this.productStatus = productStatus
        return this
    }

    fun sourceOccurredAt(sourceOccurredAt: LocalDateTime?): ProductTestBuilder {
        this.sourceOccurredAt = sourceOccurredAt
        return this
    }

    fun build(): Product {
        return Product.of(
            productId = productId,
            productCode = productCode,
            productName = productName,
            category = requireNotNull(category) { "category는 필수입니다." },
            unit = unit,
            shelfLifeDays = shelfLifeDays,
            productStatus = productStatus,
            sourceOccurredAt = sourceOccurredAt
        )
    }
}
