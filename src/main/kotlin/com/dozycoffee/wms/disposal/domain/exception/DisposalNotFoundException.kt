package com.dozycoffee.wms.disposal.domain.exception

import com.dozycoffee.wms.global.error.ApplicationException

class DisposalNotFoundException : ApplicationException(DisposalErrorCode.DISPOSAL_NOT_FOUND)
