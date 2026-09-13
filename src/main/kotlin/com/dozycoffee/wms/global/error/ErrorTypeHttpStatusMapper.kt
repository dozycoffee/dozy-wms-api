package com.dozycoffee.wms.global.error

import org.springframework.http.HttpStatus

object ErrorTypeHttpStatusMapper {

    fun resolve(errorType: ErrorType): HttpStatus {
        return when (errorType) {
            ErrorType.VALIDATION -> HttpStatus.BAD_REQUEST
            ErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorType.CONFLICT -> HttpStatus.CONFLICT
            ErrorType.INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
