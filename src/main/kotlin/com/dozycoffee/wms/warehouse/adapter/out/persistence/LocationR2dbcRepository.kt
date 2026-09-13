package com.dozycoffee.wms.warehouse.adapter.out.persistence

import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface LocationR2dbcRepository : ReactiveCrudRepository<LocationEntity, Long>
