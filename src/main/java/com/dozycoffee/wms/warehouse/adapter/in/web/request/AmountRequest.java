package com.dozycoffee.wms.warehouse.adapter.in.web.request;

import jakarta.validation.constraints.Positive;

/** WorkArea/Location의 점유(occupy)·반출(release) 요청에서 공통으로 사용하는 수량 요청 DTO */
public record AmountRequest(
        @Positive int amount
) {
}
