package com.dozycoffee.wms.warehouse_member.domain

import com.dozycoffee.wms.global.error.InvalidDomainValueException
import com.dozycoffee.wms.warehouse_member.domain.model.WarehouseMember
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class WarehouseMemberTest {

    @Test
    fun `창고와 사용자가 있으면 배정을 생성한다`() {
        val principalId: UUID = UUID.randomUUID()

        val member = WarehouseMember.create(1L, principalId)

        assertThat(member.warehouseMemberId).isNull()
        assertThat(member.warehouseId).isEqualTo(1L)
        assertThat(member.principalId).isEqualTo(principalId)
    }

    @Test
    fun `창고가 없으면 생성할 수 없다`() {
        assertThatThrownBy { WarehouseMember.create(null, UUID.randomUUID()) }
            .isInstanceOf(InvalidDomainValueException::class.java)
    }

    @Test
    fun `사용자가 없으면 생성할 수 없다`() {
        assertThatThrownBy { WarehouseMember.create(1L, null) }
            .isInstanceOf(InvalidDomainValueException::class.java)
    }

    @Test
    fun `식별자가 같으면 같은 배정이다`() {
        val principalId: UUID = UUID.randomUUID()

        assertThat(WarehouseMember.reconstitute(1L, 1L, principalId))
            .isEqualTo(WarehouseMember.reconstitute(1L, 2L, UUID.randomUUID()))
    }
}
