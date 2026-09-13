package com.dozycoffee.wms.global.error

abstract class BusinessException protected constructor(
    val errorCode: ErrorCode
) : RuntimeException(errorCode.message)
