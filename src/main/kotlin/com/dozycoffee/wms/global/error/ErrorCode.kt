package com.dozycoffee.wms.global.error

interface ErrorCode {
    val code: String
    val message: String
    val errorType: ErrorType
}
