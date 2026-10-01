package com.dozycoffee.wms.devseed

import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

internal data class LocationSpec(val zoneCode: ZoneCode, val locationCode: String, val maxCapacity: Int)

internal data class ProductSpec(
    val productCode: String,
    val productName: String,
    val category: ProductCategory,
    val unit: String,
    val shelfLifeDays: Int?
)

internal data class StockSpec(
    val productCode: String,
    val lotNumber: String,
    val expirationOffsetDays: Long?,
    val locationCode: String,
    val quantity: Int,
    val defective: Boolean = false
)

internal class SeedContext(
    val warehouseId: Long,
    val zoneIdByCode: Map<ZoneCode, Long>,
    val locationIdByCode: Map<String, Long>,
    val productIdByCode: Map<String, Long>
)

internal object SeedData {

    const val WAREHOUSE_NAME: String = "DOZY COFFEE 중앙 물류센터"
    const val WAREHOUSE_ADDRESS: String = "경기도 이천시 부발읍 물류단지로 100"

    val LOCATIONS: List<LocationSpec> = listOf(
        LocationSpec(ZoneCode.A, "A-01", 320),
        LocationSpec(ZoneCode.A, "A-02", 270),
        LocationSpec(ZoneCode.A, "A-03", 230),
        LocationSpec(ZoneCode.B, "B-01", 270),
        LocationSpec(ZoneCode.B, "B-02", 280),
        LocationSpec(ZoneCode.C, "C-01", 230),
        LocationSpec(ZoneCode.C, "C-02", 230),
        LocationSpec(ZoneCode.D, "D-01", 180),
        LocationSpec(ZoneCode.D, "D-02", 180),
        LocationSpec(ZoneCode.E, "E-01", 480),
        LocationSpec(ZoneCode.E, "E-02", 450),
        LocationSpec(ZoneCode.E, "E-03", 420),
        LocationSpec(ZoneCode.E, "E-04", 360),
        LocationSpec(ZoneCode.F, "F-01", 230),
        LocationSpec(ZoneCode.F, "F-02", 190)
    )

    val PRODUCTS: List<ProductSpec> = listOf(
        ProductSpec("BEAN-001", "에티오피아 예가체프 1kg", ProductCategory.BEAN, "KG", 180),
        ProductSpec("BEAN-002", "콜롬비아 수프리모 1kg", ProductCategory.BEAN, "KG", 180),
        ProductSpec("BEAN-003", "하우스 블렌드 1kg", ProductCategory.BEAN, "KG", 180),
        ProductSpec("SYR-001", "바닐라 시럽 1L", ProductCategory.SYRUP, "BOTTLE", 365),
        ProductSpec("SYR-002", "헤이즐넛 시럽 1L", ProductCategory.SYRUP, "BOTTLE", 365),
        ProductSpec("SYR-003", "카라멜 시럽 1L", ProductCategory.SYRUP, "BOTTLE", 365),
        ProductSpec("PWD-001", "초코 파우더 1kg", ProductCategory.POWDER, "PACK", 270),
        ProductSpec("PWD-002", "녹차 파우더 1kg", ProductCategory.POWDER, "PACK", 270),
        ProductSpec("PWD-003", "밀크티 파우더 1kg", ProductCategory.POWDER, "PACK", 270),
        ProductSpec("DRY-001", "우유 1L", ProductCategory.DAIRY, "BOX", 14),
        ProductSpec("DRY-002", "휘핑크림 1L", ProductCategory.DAIRY, "BOX", 21),
        ProductSpec("DRY-003", "생크림 1L", ProductCategory.DAIRY, "BOX", 10),
        ProductSpec("SUP-001", "16oz 컵 (1,000입)", ProductCategory.SUPPLY, "BOX", null),
        ProductSpec("SUP-002", "컵 뚜껑 (1,000입)", ProductCategory.SUPPLY, "BOX", null),
        ProductSpec("SUP-003", "포장 박스 (50입)", ProductCategory.SUPPLY, "BOX", null),
        ProductSpec("MD-001", "DOZY 텀블러", ProductCategory.MD, "EA", null),
        ProductSpec("MD-002", "DOZY 머그컵", ProductCategory.MD, "EA", null),
        ProductSpec("MD-003", "원두 선물세트", ProductCategory.MD, "SET", 365)
    )

    const val INACTIVE_PRODUCT_CODE: String = "SYR-900"
    const val DELETED_PRODUCT_CODE: String = "PWD-900"

    val INACTIVE_PRODUCT: ProductSpec =
        ProductSpec(INACTIVE_PRODUCT_CODE, "피치 시럽 1L (단종)", ProductCategory.SYRUP, "BOTTLE", 365)
    val DELETED_PRODUCT: ProductSpec =
        ProductSpec(DELETED_PRODUCT_CODE, "시즌 한정 딸기 파우더 1kg", ProductCategory.POWDER, "PACK", 270)

    const val DEFECTIVE_LOT_NUMBER: String = "SYR003-L0"
    const val EXPIRED_LOT_NUMBER: String = "DRY003-L1"
    const val EXPIRED_HELD_LOT_NUMBER: String = "DRY003-L2"

    val BASELINE_STOCK: List<StockSpec> = listOf(
        StockSpec("BEAN-001", "BEAN001-L1", 20, "A-01", 100),
        StockSpec("BEAN-002", "BEAN002-L1", 120, "A-01", 100),
        StockSpec("BEAN-001", "BEAN001-L2", 90, "A-02", 80),
        StockSpec("BEAN-003", "BEAN003-L1", 150, "A-02", 70),
        StockSpec("BEAN-001", "BEAN001-L3", 150, "A-03", 130),
        StockSpec("BEAN-002", "BEAN002-L2", 170, "A-03", 100),
        StockSpec("SYR-001", "SYR001-L1", 200, "B-01", 70),
        StockSpec("SYR-002", "SYR002-L1", 220, "B-01", 50),
        StockSpec("SYR-003", "SYR003-L1", 240, "B-02", 90),
        StockSpec("SYR-003", DEFECTIVE_LOT_NUMBER, 240, "B-02", 10, defective = true),
        StockSpec("PWD-001", "PWD001-L1", 150, "C-01", 60),
        StockSpec("PWD-002", "PWD002-L1", 160, "C-01", 40),
        StockSpec("PWD-003", "PWD003-L1", 25, "C-02", 60),
        StockSpec("DRY-001", "DRY001-L1", 8, "D-01", 90),
        StockSpec("DRY-002", "DRY002-L1", 12, "D-01", 60),
        StockSpec("DRY-001", "DRY001-L2", 13, "D-02", 110),
        StockSpec("DRY-003", EXPIRED_LOT_NUMBER, -1, "D-02", 40),
        StockSpec("DRY-003", EXPIRED_HELD_LOT_NUMBER, -2, "D-02", 20),
        StockSpec("SUP-001", "SUP001-L1", null, "E-01", 200),
        StockSpec("SUP-002", "SUP002-L1", null, "E-01", 100),
        StockSpec("SUP-003", "SUP003-L1", null, "E-02", 150),
        StockSpec("SUP-001", "SUP001-L2", null, "E-02", 50),
        StockSpec("SUP-002", "SUP002-L2", null, "E-03", 120),
        StockSpec("SUP-003", "SUP003-L2", null, "E-04", 40),
        StockSpec("MD-001", "MD001-L1", null, "F-01", 40),
        StockSpec("MD-003", "MD003-L1", 300, "F-01", 40)
    )
}
