package com.dozycoffee.wms.warehouse.application.port.`in`.command

data class OccupyWorkAreaCommand(val workAreaId: Long, val amount: Int)
