package com.dozycoffee.wms.warehouse.application.port.`in`.command

data class ReleaseLocationCommand(val locationId: Long, val amount: Int)
