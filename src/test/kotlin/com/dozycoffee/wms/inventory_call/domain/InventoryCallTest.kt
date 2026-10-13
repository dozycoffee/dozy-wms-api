package com.dozycoffee.wms.inventory_call.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.inventory_call.domain.enumeration.InventoryCallStatus
import com.dozycoffee.wms.inventory_call.domain.exception.InvalidInventoryCallStateException
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.NOW
import com.dozycoffee.wms.inventory_call.fixture.InventoryCallTestBuilder.Companion.inventoryCall
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class InventoryCallTest {

    @Nested
    inner class 생성 {

        @Test
        fun `생성하면 즉시 전송 가능한 PENDING 상태다`() {
            val call = inventoryCall().build()

            assertThat(call.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(call.attemptCount).isEqualTo(0)
            assertThat(call.nextAttemptAt).isEqualTo(NOW)
            assertThat(call.lastError).isNull()
        }

        @Test
        fun `필수 값이 없으면 예외를 던진다`() {
            assertThatThrownBy { inventoryCall().callType(null).build() }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().warehouseId(null).build() }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().requestBody(" ").build() }.isInstanceOf(InvalidDomainValueException::class.java)
        }

        @Test
        fun `멱등 키는 공백 없는 ASCII 100자 이하여야 한다`() {
            assertThatThrownBy { inventoryCall().idempotencyKey(null).build() }.isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().idempotencyKey("has space").build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().idempotencyKey("한글키").build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().idempotencyKey("k".repeat(101)).build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThat(inventoryCall().idempotencyKey("k".repeat(100)).build().idempotencyKey).hasSize(100)
        }

        @Test
        fun `요청 경로는 슬래시로 시작해야 한다`() {
            assertThatThrownBy { inventoryCall().requestPath("api/v1/x").build() }
                .isInstanceOf(InvalidDomainValueException::class.java)
            assertThatThrownBy { inventoryCall().requestPath(null).build() }.isInstanceOf(InvalidDomainValueException::class.java)
        }
    }

    @Nested
    inner class 상태_전이 {

        @Test
        fun `전송에 성공하면 SENT가 되고 응답과 시각을 남긴다`() {
            val call = inventoryCall().build()

            call.markSent("{\"quantityAfter\":10}", NOW.plusSeconds(1))

            assertThat(call.status).isEqualTo(InventoryCallStatus.SENT)
            assertThat(call.attemptCount).isEqualTo(1)
            assertThat(call.responseBody).isEqualTo("{\"quantityAfter\":10}")
            assertThat(call.sentAt).isEqualTo(NOW.plusSeconds(1))
        }

        @Test
        fun `재시도 대상 실패면 PENDING을 유지하고 다음 시도 시각을 미룬다`() {
            val call = inventoryCall().build()

            call.retryLater("timeout", NOW.plusSeconds(5))

            assertThat(call.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(call.attemptCount).isEqualTo(1)
            assertThat(call.nextAttemptAt).isEqualTo(NOW.plusSeconds(5))
            assertThat(call.lastError).isEqualTo("timeout")
        }

        @Test
        fun `마지막 오류는 500자로 자른다`() {
            val call = inventoryCall().build()

            call.retryLater("e".repeat(600), NOW)

            assertThat(call.lastError).hasSize(500)
        }

        @Test
        fun `다시 보내도 소용없는 실패면 DEAD로 격리한다`() {
            val call = inventoryCall().build()

            call.markDead("HTTP 404", "{\"code\":\"INV_PRODUCT_NOT_FOUND\"}", NOW)

            assertThat(call.status).isEqualTo(InventoryCallStatus.DEAD)
            assertThat(call.deadAt).isEqualTo(NOW)
            assertThat(call.responseBody).contains("INV_PRODUCT_NOT_FOUND")
        }

        @Test
        fun `DEAD를 재전송하면 시도 횟수와 오류를 지우고 PENDING으로 돌아간다`() {
            val call = inventoryCall().build()
            call.markDead("HTTP 404", "body", NOW)

            call.requeue(NOW.plusMinutes(1))

            assertThat(call.status).isEqualTo(InventoryCallStatus.PENDING)
            assertThat(call.attemptCount).isEqualTo(0)
            assertThat(call.nextAttemptAt).isEqualTo(NOW.plusMinutes(1))
            assertThat(call.lastError).isNull()
            assertThat(call.responseBody).isNull()
            assertThat(call.deadAt).isNull()
        }

        @Test
        fun `DEAD를 버리면 DISCARDED가 된다`() {
            val call = inventoryCall().build()
            call.markDead("HTTP 404", null, NOW)

            call.discard()

            assertThat(call.status).isEqualTo(InventoryCallStatus.DISCARDED)
        }

        @Test
        fun `DEAD가 아니면 재전송하거나 버릴 수 없다`() {
            assertThatThrownBy { inventoryCall().build().requeue(NOW) }.isInstanceOf(InvalidInventoryCallStateException::class.java)
            assertThatThrownBy { inventoryCall().build().discard() }.isInstanceOf(InvalidInventoryCallStateException::class.java)
        }

        @Test
        fun `PENDING이 아니면 전송 결과를 반영할 수 없다`() {
            val sent = inventoryCall().build().also { it.markSent(null, NOW) }

            assertThatThrownBy { sent.markSent(null, NOW) }.isInstanceOf(InvalidInventoryCallStateException::class.java)
            assertThatThrownBy { sent.retryLater("x", NOW) }.isInstanceOf(InvalidInventoryCallStateException::class.java)
            assertThatThrownBy { sent.markDead("x", null, NOW) }.isInstanceOf(InvalidInventoryCallStateException::class.java)
        }
    }

    @Test
    fun `같은 요청인지는 종류, 창고, 경로, 본문으로 판단한다`() {
        val call = inventoryCall().build()

        assertThat(call.hasSameRequest(call.callType, call.warehouseId, call.requestPath, call.requestBody)).isTrue()
        assertThat(call.hasSameRequest(call.callType, call.warehouseId, call.requestPath, "{}")).isFalse()
        assertThat(call.hasSameRequest(call.callType, 2L, call.requestPath, call.requestBody)).isFalse()
    }
}
