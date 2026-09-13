package com.dozycoffee.wms.warehouse.application.port.`in`.command

data class OccupyLocationCommand(val locationId: Long, val amount: Int)
