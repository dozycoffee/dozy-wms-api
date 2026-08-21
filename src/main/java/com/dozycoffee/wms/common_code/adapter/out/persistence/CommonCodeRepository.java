package com.dozycoffee.wms.common_code.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CommonCodeRepository extends ReactiveCrudRepository<CommonCodeEntity, String> {

    Flux<CommonCodeEntity> findByGroupCodeAndActiveTrue(String groupCode);
}
