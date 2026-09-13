package com.dozycoffee.wms.global.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponseDto> {
        logger.warn("Business exception: code={}, message={}", e.errorCode.code, e.message)
        val errorCode: ErrorCode = e.errorCode
        val status: HttpStatus = ErrorTypeHttpStatusMapper.resolve(errorCode.errorType)
        return ResponseEntity.status(status).body(ErrorResponseDto.of(errorCode))
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(e: WebExchangeBindException): ResponseEntity<ErrorResponseDto> {
        val message: String = e.bindingResult.fieldErrors.joinToString(", ") { fe ->
            "${fe.field}: ${fe.defaultMessage}"
        }
        return ResponseEntity.badRequest()
            .body(ErrorResponseDto(CommonErrorCode.INVALID_INPUT.code, message, LocalDateTime.now()))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponseDto> {
        logger.error("Unhandled exception", e)
        return ResponseEntity.internalServerError().body(ErrorResponseDto.of(CommonErrorCode.INTERNAL_SERVER_ERROR))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
