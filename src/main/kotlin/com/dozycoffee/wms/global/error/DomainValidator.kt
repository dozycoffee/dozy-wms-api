package com.dozycoffee.wms.global.error

object DomainValidator {

    @JvmStatic
    fun <T : Any> requireNonNull(value: T?, errorCode: ErrorCode): T {
        if (value == null) {
            throw InvalidDomainValueException(errorCode)
        }
        return value
    }
}
