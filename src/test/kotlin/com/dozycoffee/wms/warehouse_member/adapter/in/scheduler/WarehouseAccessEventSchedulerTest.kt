package com.dozycoffee.wms.warehouse_member.adapter.`in`.scheduler

import com.dozycoffee.wms.warehouse_member.application.port.`in`.PublishWarehouseAccessEventsUseCase
import com.dozycoffee.wms.warehouse_member.application.port.`in`.result.PublishWarehouseAccessEventsResult
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
class WarehouseAccessEventSchedulerTest {

    @Mock
    private lateinit var publishWarehouseAccessEventsUseCase: PublishWarehouseAccessEventsUseCase

    @InjectMocks
    private lateinit var scheduler: WarehouseAccessEventScheduler

    @Test
    fun `처리한 건이 없을 때까지 배치를 반복한다`() = runTest {
        whenever(publishWarehouseAccessEventsUseCase.publishBatch()).thenReturn(
            PublishWarehouseAccessEventsResult(2, 0),
            PublishWarehouseAccessEventsResult(1, 1),
            PublishWarehouseAccessEventsResult(0, 0)
        )

        scheduler.publish()

        verify(publishWarehouseAccessEventsUseCase, times(3)).publishBatch()
    }

    @Test
    fun `처리할 건이 계속 나와도 한 번의 실행에서 반복 횟수를 제한한다`() = runTest {
        whenever(publishWarehouseAccessEventsUseCase.publishBatch()).thenReturn(PublishWarehouseAccessEventsResult(1, 0))

        scheduler.publish()

        verify(publishWarehouseAccessEventsUseCase, times(50)).publishBatch()
    }
}
