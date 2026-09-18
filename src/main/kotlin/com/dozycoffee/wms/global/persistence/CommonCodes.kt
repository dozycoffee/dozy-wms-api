package com.dozycoffee.wms.global.persistence

object CommonCodes {

    fun <E : Enum<E>> toCode(groupCode: String, value: E): String = "${groupCode}_${value.name}"

    /**
     * code는 "{groupCode}_{value.name}" 형태인데, value.name 자체에 언더스코어가 포함될 수 있어
     * (예: STOCK_AUDIT_STATUS_IN_PROGRESS) 마지막 '_' 기준으로 자르면 잘못 파싱된다. enum 상수 이름과
     * code의 suffix를 직접 비교해 가장 길게 일치하는 상수를 찾는다.
     */
    fun <E : Enum<E>> fromCode(type: Class<E>, code: String): E {
        return type.enumConstants
            .filter { code.endsWith(it.name) }
            .maxByOrNull { it.name.length }
            ?: throw IllegalArgumentException("$code 에 해당하는 ${type.simpleName} 상수를 찾을 수 없습니다.")
    }
}
