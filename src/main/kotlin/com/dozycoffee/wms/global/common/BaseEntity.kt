package com.dozycoffee.wms.global.common

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

abstract class BaseEntity {

    @CreatedDate
    var createdAt: LocalDateTime? = null
        private set

    @CreatedBy
    var createdBy: String? = null
        private set

    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
        private set

    @LastModifiedBy
    var updatedBy: String? = null
        private set

    /**
     * 도메인 모델을 거쳐 재구성된 엔티티는 createdAt/createdBy를 들고 있지 않다.
     * update 시점에 이 값이 비어 있으면 auditing이 채워주지 않아 그대로 NULL로 덮어써지므로,
     * 저장 전 기존 엔티티에서 복사해 보존한다. (updatedAt/updatedBy는 매 저장 시 auditing이 갱신한다)
     */
    open fun copyAuditFieldsFrom(existing: BaseEntity) {
        this.createdAt = existing.createdAt
        this.createdBy = existing.createdBy
    }
}
