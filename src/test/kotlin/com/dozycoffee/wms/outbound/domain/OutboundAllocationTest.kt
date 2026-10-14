package com.dozycoffee.wms.outbound.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.outbound.domain.model.OutboundAllocation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OutboundAllocationTest {

    private fun allocation(quantity: Int = 5): OutboundAllocation =
        OutboundAllocation.create(1L, 55L, "LOT-1", LocalDate.of(2027, 1, 1), quantity)

    @Test
    fun `생성하면 피킹 수량이 비어 있다`() {
        val allocation = allocation()

        assertThat(allocation.quantity).isEqualTo(5)
        assertThat(allocation.pickedQuantity).isNull()
    }

    @Test
    fun `필수 값이 없거나 수량이 0 이하이면 예외를 던진다`() {
        assertThatThrownBy { OutboundAllocation.create(null, 55L, "LOT-1", null, 5) }.isInstanceOf(InvalidDomainValueException::class.java)
        assertThatThrownBy { OutboundAllocation.create(1L, null, "LOT-1", null, 5) }.isInstanceOf(InvalidDomainValueException::class.java)
        assertThatThrownBy { OutboundAllocation.create(1L, 55L, " ", null, 5) }.isInstanceOf(InvalidDomainValueException::class.java)
        assertThatThrownBy { allocation(0) }.isInstanceOf(InvalidDomainValueException::class.java)
    }

    @Test
    fun `피킹 수량은 0 이상 할당 수량 이하여야 한다`() {
        val allocation = allocation(5)

        allocation.pick(0)
        allocation.pick(5)

        assertThat(allocation.pickedQuantity).isEqualTo(5)
        assertThatThrownBy { allocation.pick(6) }.isInstanceOf(InvalidDomainValueException::class.java)
        assertThatThrownBy { allocation.pick(-1) }.isInstanceOf(InvalidDomainValueException::class.java)
    }

    @Test
    fun `재할당하면 수량이 바뀌고 피킹 수량이 비워진다`() {
        val allocation = allocation(5)
        allocation.pick(5)

        allocation.reallocate(3)

        assertThat(allocation.quantity).isEqualTo(3)
        assertThat(allocation.pickedQuantity).isNull()
        assertThatThrownBy { allocation.reallocate(0) }.isInstanceOf(InvalidDomainValueException::class.java)
    }
}
