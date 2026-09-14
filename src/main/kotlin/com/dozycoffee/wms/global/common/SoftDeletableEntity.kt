package com.dozycoffee.wms.global.common

import java.time.LocalDateTime

abstract class SoftDeletableEntity : BaseEntity() {

    var deletedAt: LocalDateTime? = null
        private set

    var deletedBy: String? = null
        private set

    fun isDeleted(): Boolean = deletedAt != null

    protected fun softDelete(actor: String) {
        this.deletedAt = LocalDateTime.now()
        this.deletedBy = actor
    }
}
