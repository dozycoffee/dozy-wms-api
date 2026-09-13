package com.dozycoffee.wms.warehouse.adapter.out.persistence

internal object CommonCodes {

    fun <E : Enum<E>> toCode(groupCode: String, value: E): String = "${groupCode}_${value.name}"

    fun <E : Enum<E>> fromCode(type: Class<E>, code: String): E {
        val value = code.substring(code.lastIndexOf('_') + 1)
        return java.lang.Enum.valueOf(type, value)
    }
}
