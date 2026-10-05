package com.dozycoffee.wms.support

import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import org.springframework.dao.DataIntegrityViolationException

/** MySQL 유니크 위반(ER_DUP_ENTRY 1062)이 Spring을 거쳐 올라오는 모양을 흉내 낸다 */
fun duplicateKeyViolation(): DataIntegrityViolationException =
    DataIntegrityViolationException(
        "Duplicate entry",
        R2dbcDataIntegrityViolationException("Duplicate entry", "23000", 1062)
    )
