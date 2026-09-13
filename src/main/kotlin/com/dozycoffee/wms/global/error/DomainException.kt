package com.dozycoffee.wms.global.error

open class DomainException protected constructor(errorCode: ErrorCode) : BusinessException(errorCode)
