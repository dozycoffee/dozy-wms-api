package com.dozycoffee.wms.common_code.domain.model

class CommonCode private constructor(
    val code: String,
    val groupCode: String,
    val name: String,
    val sortOrder: Int,
    val active: Boolean
) {
    companion object {
        fun create(code: String, groupCode: String, name: String, sortOrder: Int): CommonCode {
            return CommonCode(code, groupCode, name, sortOrder, true)
        }

        fun reconstruct(code: String, groupCode: String, name: String, sortOrder: Int, active: Boolean): CommonCode {
            return CommonCode(code, groupCode, name, sortOrder, active)
        }
    }
}
