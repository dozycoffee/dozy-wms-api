package com.dozycoffee.wms.inventory_call.adapter.out.persistence

import com.dozycoffee.wms.global.common.BaseEntity
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.inventory_call.domain.model.InventoryCall
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("inventory_call")
class InventoryCallEntity private constructor() : BaseEntity() {

    @Id
    var inventoryCallId: Long? = null
        private set

    var callType: String? = null
        private set

    var warehouseId: Long? = null
        private set

    var idempotencyKey: String? = null
        private set

    var requestPath: String? = null
        private set

    var requestBody: String? = null
        private set

    var status: String? = null
        private set

    var attemptCount: Int? = null
        private set

    var nextAttemptAt: LocalDateTime? = null
        private set

    var lastError: String? = null
        private set

    var responseBody: String? = null
        private set

    var sentAt: LocalDateTime? = null
        private set

    var deadAt: LocalDateTime? = null
        private set

    fun toDomain(): InventoryCall {
        return InventoryCall.reconstitute(
            requireNotNull(inventoryCallId),
            InventoryCallType.valueOf(requireNotNull(callType)),
            requireNotNull(warehouseId),
            requireNotNull(idempotencyKey),
            requireNotNull(requestPath),
            requireNotNull(requestBody),
            InventoryCallStatus.valueOf(requireNotNull(status)),
            requireNotNull(attemptCount),
            requireNotNull(nextAttemptAt),
            lastError,
            responseBody,
            sentAt,
            deadAt
        )
    }

    companion object {
        fun from(domain: InventoryCall): InventoryCallEntity {
            val entity = InventoryCallEntity()
            entity.inventoryCallId = domain.inventoryCallId
            entity.callType = domain.callType.name
            entity.warehouseId = domain.warehouseId
            entity.idempotencyKey = domain.idempotencyKey
            entity.requestPath = domain.requestPath
            entity.requestBody = domain.requestBody
            entity.status = domain.status.name
            entity.attemptCount = domain.attemptCount
            entity.nextAttemptAt = domain.nextAttemptAt
            entity.lastError = domain.lastError
            entity.responseBody = domain.responseBody
            entity.sentAt = domain.sentAt
            entity.deadAt = domain.deadAt
            return entity
        }
    }
}
