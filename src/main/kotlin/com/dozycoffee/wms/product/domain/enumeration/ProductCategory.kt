package com.dozycoffee.wms.product.domain.enumeration

import com.dozycoffee.wms.warehouse.domain.enumeration.ZoneCode

enum class ProductCategory(val zoneCode: ZoneCode) {
    BEAN(ZoneCode.A),
    SYRUP(ZoneCode.B),
    POWDER(ZoneCode.C),
    DAIRY(ZoneCode.D),
    SUPPLY(ZoneCode.E),
    MD(ZoneCode.F)
}
