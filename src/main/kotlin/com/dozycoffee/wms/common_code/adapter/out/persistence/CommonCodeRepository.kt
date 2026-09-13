package com.dozycoffee.wms.common_code.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface CommonCodeRepository : ReactiveCrudRepository<CommonCodeEntity, String> {

    fun findByGroupCodeAndActiveTrue(groupCode: String): Flux<CommonCodeEntity>
}
