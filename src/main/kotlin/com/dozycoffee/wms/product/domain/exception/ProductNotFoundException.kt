package com.dozycoffee.wms.product.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class ProductNotFoundException : ApplicationException(ProductErrorCode.PRODUCT_NOT_FOUND)
