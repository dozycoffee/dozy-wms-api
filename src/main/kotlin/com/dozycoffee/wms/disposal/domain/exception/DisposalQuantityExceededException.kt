package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DisposalQuantityExceededException : ApplicationException(DisposalItemErrorCode.QUANTITY_EXCEEDED)
