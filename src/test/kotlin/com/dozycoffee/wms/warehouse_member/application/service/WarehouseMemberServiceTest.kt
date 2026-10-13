package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.warehouse.application.port.`in`.GetWarehouseUseCase
import com.dozycoffee.wms.warehouse.application.port.`in`.result.WarehouseResult
import com.dozycoffee.wms.warehouse.domain.exception.WarehouseNotFoundException
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseAccessEventRepository
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberNotFoundException
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import com.dozycoffee.wms.support.duplicateKeyViolation
import com.dozycoffee.wms.warehouse_member.domain.exception.WarehouseMemberAlreadyAssignedException
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
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WarehouseMemberServiceTest {

    @Mock
    private lateinit var warehouseMemberRepository: WarehouseMemberRepository

    @Mock
    private lateinit var warehouseAccessEventRepository: WarehouseAccessEventRepository

    @Mock
    private lateinit var getWarehouseUseCase: GetWarehouseUseCase

    private lateinit var service: WarehouseMemberService

    private val now: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)

    @BeforeEach
    fun setUp() {
        val zone: ZoneId = ZoneId.of("Asia/Seoul")
        service = WarehouseMemberService(
            warehouseMemberRepository,
            warehouseAccessEventRepository,
            getWarehouseUseCase,
            Clock.fixed(now.atZone(zone).toInstant(), zone)
        )
    }

    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

    private fun warehouseResult(): WarehouseResult = org.mockito.kotlin.mock()

    @Nested
    inner class 배정 {

        @Test
        fun `배정이 없으면 새로 저장한다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(warehouseResult())
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)
            whenever(warehouseMemberRepository.save(any()))
                .thenReturn(WarehouseMember.reconstitute(10L, 1L, principalId))

            val result = service.assign(1L, principalId)

            assertThat(result.warehouseMemberId).isEqualTo(10L)
            assertThat(result.principalId).isEqualTo(principalId)
            verify(warehouseMemberRepository).save(any())
        }

        @Test
        fun `새로 배정하면 그 직원의 소속 변경을 발행 대기에 기록한다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(warehouseResult())
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)
            whenever(warehouseMemberRepository.save(any()))
                .thenReturn(WarehouseMember.reconstitute(10L, 1L, principalId))

            service.assign(1L, principalId)

            verify(warehouseAccessEventRepository).markChanged(eq(principalId), any(), eq(now), eq(now))
        }

        @Test
        fun `이미 배정돼 있으면 저장하지 않고 기존 배정을 반환한다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(warehouseResult())
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId))
                .thenReturn(WarehouseMember.reconstitute(10L, 1L, principalId))

            val result = service.assign(1L, principalId)

            assertThat(result.warehouseMemberId).isEqualTo(10L)
            verify(warehouseMemberRepository, never()).save(any())
            verify(warehouseAccessEventRepository, never()).markChanged(any(), any(), any(), any())
        }

        @Test
        fun `동시에 같은 배정이 저장돼 유니크 위반이 나면 이미 배정됨 예외를 던진다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenReturn(warehouseResult())
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)
            whenever(warehouseMemberRepository.save(any())).thenThrow(duplicateKeyViolation())

            assertThatThrownBy { runBlocking { service.assign(1L, principalId) } }
                .isInstanceOf(WarehouseMemberAlreadyAssignedException::class.java)
            verify(warehouseAccessEventRepository, never()).markChanged(any(), any(), any(), any())
        }

        @Test
        fun `존재하지 않는 창고에는 배정할 수 없다`() = runTest {
            whenever(getWarehouseUseCase.getById(1L)).thenThrow(WarehouseNotFoundException())

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
            verify(warehouseAccessEventRepository).markChanged(eq(principalId), any(), eq(now), eq(now))
        }

        @Test
        fun `배정되지 않은 사용자를 해제하면 예외를 던진다`() = runTest {
            whenever(warehouseMemberRepository.findByWarehouseIdAndPrincipalId(1L, principalId)).thenReturn(null)

            assertThatThrownBy { runBlocking { service.remove(1L, principalId) } }
                .isInstanceOf(WarehouseMemberNotFoundException::class.java)
            verify(warehouseAccessEventRepository, never()).markChanged(any(), any(), any(), any())
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
