package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class InventoryNotDisposableException : ApplicationException(DisposalItemErrorCode.INVENTORY_NOT_DISPOSABLE)
