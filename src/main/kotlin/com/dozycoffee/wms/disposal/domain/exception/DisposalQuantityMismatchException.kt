package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DisposalQuantityMismatchException : ApplicationException(DisposalItemErrorCode.QUANTITY_MISMATCH)
