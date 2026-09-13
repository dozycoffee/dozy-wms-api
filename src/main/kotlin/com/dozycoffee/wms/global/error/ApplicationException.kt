package com.dozycoffee.wms.global.error

open class ApplicationException protected constructor(errorCode: ErrorCode) : BusinessException(errorCode)
