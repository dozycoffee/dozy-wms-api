package com.dozycoffee.wms.warehouse_member.adapter.`in`.web.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AssignWarehouseMemberRequest(
    @field:NotNull val principalId: UUID?
)
