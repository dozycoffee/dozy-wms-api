package com.dozycoffee.wms.warehouse_member.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class WarehouseMemberAlreadyAssignedException : ApplicationException(WarehouseMemberErrorCode.ALREADY_ASSIGNED)
