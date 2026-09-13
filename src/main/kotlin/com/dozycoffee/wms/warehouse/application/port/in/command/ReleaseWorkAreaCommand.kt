package com.dozycoffee.wms.warehouse.application.port.`in`.command

data class ReleaseWorkAreaCommand(val workAreaId: Long, val amount: Int)
