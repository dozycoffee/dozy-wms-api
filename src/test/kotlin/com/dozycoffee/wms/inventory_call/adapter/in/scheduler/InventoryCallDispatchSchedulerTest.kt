package com.dozycoffee.wms.inventory_call.adapter.`in`.scheduler

import com.dozycoffee.wms.inventory_call.application.port.`in`.DispatchInventoryCallsUseCase
import com.dozycoffee.wms.inventory_call.application.port.`in`.result.DispatchInventoryCallsResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class InventoryCallDispatchSchedulerTest {

    @Mock
    private lateinit var dispatchInventoryCallsUseCase: DispatchInventoryCallsUseCase

    @InjectMocks
    private lateinit var scheduler: InventoryCallDispatchScheduler

    @Test
    fun `처리한 호출이 없을 때까지 배치를 반복한다`() = runTest {
        whenever(dispatchInventoryCallsUseCase.dispatchBatch()).thenReturn(
            DispatchInventoryCallsResult(2, 0, 0),
            DispatchInventoryCallsResult(1, 1, 0),
            DispatchInventoryCallsResult(0, 0, 0)
        )

        scheduler.dispatch()

        verify(dispatchInventoryCallsUseCase, times(3)).dispatchBatch()
    }

    @Test
    fun `처리할 호출이 계속 나와도 한 번의 실행에서 반복 횟수를 제한한다`() = runTest {
        whenever(dispatchInventoryCallsUseCase.dispatchBatch()).thenReturn(DispatchInventoryCallsResult(1, 0, 0))

        scheduler.dispatch()

        verify(dispatchInventoryCallsUseCase, times(50)).dispatchBatch()
    }
}
