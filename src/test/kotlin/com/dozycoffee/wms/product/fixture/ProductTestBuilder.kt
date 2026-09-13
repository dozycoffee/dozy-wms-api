package com.dozycoffee.wms.product.fixture

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.model.Product

class ProductTestBuilder {

    private var productId: Long? = null
    private var productCode: String? = "PRD-0001"
    private var productName: String? = "콜롬비아 원두"
    private var category: ProductCategory? = ProductCategory.BEAN
    private var unit: String? = "KG"
    private var shelfLifeDays: Int? = 365
    private var productStatus: ProductStatus = ProductStatus.ACTIVE

    companion object {
        fun product(): ProductTestBuilder = ProductTestBuilder()
    }

    fun productId(productId: Long?): ProductTestBuilder {
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

    fun build(): Product {
        val id: Long? = productId
        if (id != null) {
            return Product.reconstitute(
                productId = id,
                productCode = requireNotNull(productCode) { "productCode는 재구성 시 필수입니다." },
                productName = requireNotNull(productName) { "productName은 재구성 시 필수입니다." },
                category = requireNotNull(category) { "category는 재구성 시 필수입니다." },
                unit = requireNotNull(unit) { "unit은 재구성 시 필수입니다." },
                shelfLifeDays = shelfLifeDays,
                productStatus = productStatus
            )
        }
        return Product.create(
            productCode = productCode,
            productName = productName,
            category = category,
            unit = unit,
            shelfLifeDays = shelfLifeDays
        )
    }
}
