package com.dozycoffee.wms.global.error

import java.time.LocalDateTime

data class ErrorResponseDto(
    val errorCode: String,
    val message: String,
    val timestamp: LocalDateTime
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponseDto {
            return ErrorResponseDto(errorCode.code, errorCode.message, LocalDateTime.now())
        }
    }
}
