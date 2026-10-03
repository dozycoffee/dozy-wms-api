package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberNotFoundException
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WarehouseMemberServiceTest {

    @Mock
    private lateinit var warehouseMemberRepository: WarehouseMemberRepository

    @Mock
    private lateinit var getWarehouseUseCase: GetWarehouseUseCase

    @InjectMocks
    private lateinit var service: WarehouseMemberService

    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

    private fun warehouseResult(): WarehouseResult = org.mockito.kotlin.mock()

    @Nested
    inner class 배정 {

        @Test
        fun `배정이 없으면 새로 저장한다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(Mono.just(warehouseResult()))
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)
            whenever(warehouseMemberRepository.save(any()))
                .thenReturn(WarehouseMember.reconstitute(10L, 1L, principalId))

            val result = service.assign(1L, principalId)

            assertThat(result.warehouseMemberId).isEqualTo(10L)
            assertThat(result.principalId).isEqualTo(principalId)
            verify(warehouseMemberRepository).save(any())
        }

        @Test
        fun `이미 배정돼 있으면 저장하지 않고 기존 배정을 반환한다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(Mono.just(warehouseResult()))
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId))
                .thenReturn(WarehouseMember.reconstitute(10L, 1L, principalId))

            val result = service.assign(1L, principalId)

            assertThat(result.warehouseMemberId).isEqualTo(10L)
            verify(warehouseMemberRepository, never()).save(any())
        }

        @Test
        fun `존재하지 않는 창고에는 배정할 수 없다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(Mono.error(WarehouseNotFoundException()))

            assertThatThrownBy { runBlocking { service.assign(1L, principalId) } }
                .isInstanceOf(WarehouseNotFoundException::class.java)
            verify(warehouseMemberRepository, never()).save(any())
        }
    }

    @Nested
    inner class 해제 {

        @Test
        fun `배정을 삭제한다`() = runTest {
            val member = WarehouseMember.reconstitute(10L, 1L, principalId)
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(member)

            service.remove(1L, principalId)

            verify(warehouseMemberRepository).delete(member)
        }

        @Test
        fun `배정되지 않은 사용자를 해제하면 예외를 던진다`() = runTest {
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)

            assertThatThrownBy { runBlocking { service.remove(1L, principalId) } }
                .isInstanceOf(WarehouseMemberNotFoundException::class.java)
        }
    }

    @Test
    fun `창고의 배정 목록을 조회한다`() = runTest {
        whenever(warehouseMemberRepository.findAllByWarehouseId(1L))
            .thenReturn(flowOf(WarehouseMember.reconstitute(10L, 1L, principalId)))

        val results = service.getAllByWarehouse(1L).toList()

        assertThat(results).hasSize(1)
        assertThat(results[0].principalId).isEqualTo(principalId)
    }
}
