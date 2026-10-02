package com.dozycoffee.wms.warehouse_member.adapter.`in`.web

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.warehouse_member.adapter.`in`.web.request.AssignWarehouseMemberRequest
import com.dozycoffee.wms.warehouse_member.application.port.`in`.AssignWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.GetWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.RemoveWarehouseMemberUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.WarehouseMemberResult
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberNotFoundException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.UUID

@WithDozyPrincipal(roles = ["wms:warehouse_admin"])
@WebFluxTest(WarehouseMemberController::class)
class WarehouseMemberControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var assignWarehouseMemberUseCase: AssignWarehouseMemberUseCase

    @MockitoBean
    private lateinit var removeWarehouseMemberUseCase: RemoveWarehouseMemberUseCase

    @MockitoBean
    private lateinit var getWarehouseMemberUseCase: GetWarehouseMemberUseCase

    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

    @Test
    fun `배정하면 201과 배정 정보를 반환한다`() {
        runBlocking {
            whenever(assignWarehouseMemberUseCase.assign(1L, principalId))
                .thenReturn(WarehouseMemberResult(10L, 1L, principalId))
        }

        webTestClient.post().uri("/api/warehouses/1/members")
            .bodyValue(AssignWarehouseMemberRequest(principalId))
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.warehouseMemberId").isEqualTo(10)
            .jsonPath("$.principalId").isEqualTo(principalId.toString())
    }

    @Test
    fun `사용자가 비어있으면 400을 반환한다`() {
        webTestClient.post().uri("/api/warehouses/1/members")
            .bodyValue(AssignWarehouseMemberRequest(null))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `창고의 배정 목록을 반환한다`() {
        whenever(getWarehouseMemberUseCase.getAllByWarehouse(1L))
            .thenReturn(flowOf(WarehouseMemberResult(10L, 1L, principalId)))

        webTestClient.get().uri("/api/warehouses/1/members")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].principalId").isEqualTo(principalId.toString())
    }

    @Test
    fun `해제하면 204를 반환한다`() {
        webTestClient.delete().uri("/api/warehouses/1/members/{principalId}", principalId)
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    fun `배정되지 않은 사용자를 해제하면 404를 반환한다`() {
        runBlocking {
            doThrow(WarehouseMemberNotFoundException()).whenever(removeWarehouseMemberUseCase)
                .remove(eq(1L), any())
        }

        webTestClient.delete().uri("/api/warehouses/1/members/{principalId}", principalId)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    @WithDozyPrincipal(roles = ["wms:inbound_manager"])
    fun `warehouse_admin이 아니면 배정 관리는 403이다`() {
        webTestClient.get().uri("/api/warehouses/1/members").exchange().expectStatus().isForbidden
        webTestClient.delete().uri("/api/warehouses/1/members/{principalId}", principalId)
            .exchange().expectStatus().isForbidden
    }
}
