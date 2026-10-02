package com.dozycoffee.wms.global.security

/**
 * Auth에 `wms:{code}`로 등록하는 role 코드. 등록 후 code는 바꿀 수 없다.
 * 쓰기는 해당 도메인 담당 role과 [WAREHOUSE_ADMIN]이, 조회는 모든 WMS role이 가능하다.
 */
enum class WmsRole(val code: String) {
    INBOUND_MANAGER("inbound_manager"),
    OUTBOUND_MANAGER("outbound_manager"),
    RETURN_MANAGER("return_manager"),
    DISPOSAL_MANAGER("disposal_manager"),
    STOCK_AUDIT_MANAGER("stock_audit_manager"),
    INVENTORY_VIEWER("inventory_viewer"),
    WAREHOUSE_ADMIN("warehouse_admin")
}

/** `@PreAuthorize`에 쓰는 SpEL 식. 어노테이션 인자는 컴파일 타임 상수여야 해서 문자열로 둔다 */
object WmsAuthorize {
    const val READ: String =
        "hasAnyRole('inbound_manager','outbound_manager','return_manager','disposal_manager'," +
            "'stock_audit_manager','inventory_viewer','warehouse_admin')"
    const val INBOUND: String = "hasAnyRole('inbound_manager','warehouse_admin')"
    const val OUTBOUND: String = "hasAnyRole('outbound_manager','warehouse_admin')"
    const val RETURN: String = "hasAnyRole('return_manager','warehouse_admin')"
    const val DISPOSAL: String = "hasAnyRole('disposal_manager','warehouse_admin')"
    const val STOCK_AUDIT: String = "hasAnyRole('stock_audit_manager','warehouse_admin')"
    const val ADMIN: String = "hasRole('warehouse_admin')"
}
