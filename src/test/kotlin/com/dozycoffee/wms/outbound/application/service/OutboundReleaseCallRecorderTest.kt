package com.dozycoffee.wms.outbound.application.service

import com.dozycoffee.wms.inventory_call.application.port.`in`.RecordInventoryCallUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.command.RecordInventoryCallCommand
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallType
import com.dozycoffee.wms.outbound.fixture.OutboundTestBuilder.Companion.outbound
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class OutboundReleaseCallRecorderTest {

    @Mock
    private lateinit var recordInventoryCallUseCase: RecordInventoryCallUseCase

    @Test
    fun `예약 해제 호출을 출고 ID 기반 멱등 키로 기록한다`() = runTest {
        val outbound = outbound().outboundId(3L).warehouseId(1L).reservationId(7L).build()

        OutboundReleaseCallRecorder(recordInventoryCallUseCase).record(outbound)

        val captor = argumentCaptor<RecordInventoryCallCommand>()
        verify(recordInventoryCallUseCase).record(captor.capture())
        val command = captor.firstValue
        assertThat(command.callType).isEqualTo(InventoryCallType.RELEASE)
        assertThat(command.warehouseId).isEqualTo(1L)
        assertThat(command.idempotencyKey).isEqualTo("wms-release-3")
        assertThat(command.requestPath).isEqualTo("/api/v1/reservations/7/release")
    }

    @Test
    fun `예약이 없는 이전 출고는 기록하지 않는다`() = runTest {
        OutboundReleaseCallRecorder(recordInventoryCallUseCase).record(outbound().outboundId(3L).reservationId(null).build())

        verify(recordInventoryCallUseCase, never()).record(any())
    }
}
