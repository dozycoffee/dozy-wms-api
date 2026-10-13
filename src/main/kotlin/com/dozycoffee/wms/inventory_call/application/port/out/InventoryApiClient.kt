package com.dozycoffee.wms.inventory_call.application.port.out

/** inventory 서비스 호출 결과. 재시도해 볼 만한 실패와 다시 보내도 소용없는 실패를 구분한다(ADR-0019) */
sealed interface InventoryApiResult {
    data class Success(val responseBody: String?) : InventoryApiResult

    data class Retryable(val reason: String) : InventoryApiResult

    data class Permanent(val status: Int?, val responseBody: String?, val reason: String) : InventoryApiResult
}

interface InventoryApiClient {
    /** [path]로 [body]를 POST한다. 어떤 실패도 예외가 아니라 [InventoryApiResult]로 돌려준다 */
    suspend fun post(path: String, idempotencyKey: String, body: String): InventoryApiResult
}
