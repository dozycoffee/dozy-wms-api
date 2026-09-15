package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DisposalItemNotFoundException : ApplicationException(DisposalItemErrorCode.DISPOSAL_ITEM_NOT_FOUND)
