package com.dozycoffee.wms.global.security

/**
 * Auth에 `wms:{code}`로 등록하는 role 코드. 등록 후 code는 바꿀 수 없다 (ADR-0025).
 * [WAREHOUSE_MANAGER]는 배정된 창고의 입고·출고·반품·폐기·실사를 하는 창고 관리자다.
 * [ADMIN]은 창고·Zone 마스터, 직원-창고 배정, 승인, 운영 API를 맡는 WMS 운영 관리자이며 콘솔의 업무 모드나 대시보드가 없다.
 * [VIEWER]는 창고 마스터(목록·이름)만 읽는다. 다른 서비스 화면에서 창고 이름을 보여주려는 용도라 업무 데이터는 보지 못한다.
 * [SERVICE]는 다른 서비스(inventory)가 system client로 호출할 때 쓰는 role이며 그 서비스용 API에만 허용한다.
 */
enum class WmsRole(val code: String) {
    WAREHOUSE_MANAGER("warehouse_manager"),
    VIEWER("viewer"),
    ADMIN("admin"),
    SERVICE("service")
}

/** `@PreAuthorize`에 쓰는 SpEL 식. 어노테이션 인자는 컴파일 타임 상수여야 해서 문자열로 둔다 */
object WmsAuthorize {
    /** 업무 데이터 조회. [WmsRole.VIEWER]는 창고 마스터만 읽을 수 있어 포함하지 않는다 */
    const val READ: String = "hasAnyRole('warehouse_manager','admin')"

    /** 창고 마스터(창고 목록·단건) 조회 */
    const val MASTER_READ: String = "hasAnyRole('warehouse_manager','viewer','admin')"
    const val INBOUND: String = "hasAnyRole('warehouse_manager','admin')"
    const val OUTBOUND: String = "hasAnyRole('warehouse_manager','admin')"
    const val RETURN: String = "hasAnyRole('warehouse_manager','admin')"
    const val DISPOSAL: String = "hasAnyRole('warehouse_manager','admin')"
    const val STOCK_AUDIT: String = "hasAnyRole('warehouse_manager','admin')"
    const val ADMIN: String = "hasRole('admin')"
    const val OUTBOUND_OR_SERVICE: String = "hasAnyRole('warehouse_manager','admin','service')"
    const val SERVICE: String = "hasAnyRole('service','admin')"
}
