package com.dozycoffee.wms.product.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DuplicateProductCodeException : ApplicationException(ProductErrorCode.DUPLICATE_PRODUCT_CODE)
