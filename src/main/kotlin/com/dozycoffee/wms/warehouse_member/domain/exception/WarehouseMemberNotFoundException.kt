package com.dozycoffee.wms.warehouse_member.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class WarehouseMemberNotFoundException : ApplicationException(WarehouseMemberErrorCode.WAREHOUSE_MEMBER_NOT_FOUND)
