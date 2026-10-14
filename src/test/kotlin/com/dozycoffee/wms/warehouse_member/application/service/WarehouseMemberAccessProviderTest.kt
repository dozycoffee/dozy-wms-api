package com.dozycoffee.wms.warehouse_member.application.service

import com.dozycoffee.wms.global.security.Actor
import com.dozycoffee.wms.global.security.AllWarehouses
import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.OnlyWarehouses
import com.dozycoffee.wms.global.security.SystemActor
import com.dozycoffee.wms.global.security.UserActor
import com.dozycoffee.wms.warehouse_member.application.port.out.WarehouseMemberRepository
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class WarehouseMemberAccessProviderTest {

    @Mock
    private lateinit var warehouseMemberRepository: WarehouseMemberRepository

    private val principalId: UUID = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

    private fun providerFor(actor: Actor): WarehouseMemberAccessProvider =
        WarehouseMemberAccessProvider(
            object : CurrentActorProvider {
                override suspend fun get(): Actor = actor
            },
            warehouseMemberRepository
        )

    @Test
    fun `시스템 작업은 모든 창고에 접근한다`() = runTest {
        assertThat(providerFor(SystemActor).current()).isEqualTo(AllWarehouses)
        verify(warehouseMemberRepository, never()).findWarehouseIdsByPrincipalId(org.mockito.kotlin.any())
    }

    @Test
    fun `admin은 배정과 무관하게 모든 창고에 접근한다`() = runTest {
        val actor = UserActor(principalId, setOf("admin"))

        assertThat(providerFor(actor).current()).isEqualTo(AllWarehouses)
        verify(warehouseMemberRepository, never()).findWarehouseIdsByPrincipalId(org.mockito.kotlin.any())
    }

    @Test
    fun `서비스 간 호출 role은 배정과 무관하게 모든 창고에 접근한다`() = runTest {
        val actor = UserActor(principalId, setOf("service"))

        assertThat(providerFor(actor).current()).isEqualTo(AllWarehouses)
        verify(warehouseMemberRepository, never()).findWarehouseIdsByPrincipalId(org.mockito.kotlin.any())
    }

    @Test
    fun `viewer만 가진 사용자는 창고 마스터 조회 전용이라 모든 창고에 접근한다`() = runTest {
        val actor = UserActor(principalId, setOf("viewer"))

        assertThat(providerFor(actor).current()).isEqualTo(AllWarehouses)
        verify(warehouseMemberRepository, never()).findWarehouseIdsByPrincipalId(org.mockito.kotlin.any())
    }

    @Test
    fun `viewer와 창고 관리자를 함께 가지면 배정된 창고로 제한한다`() = runTest {
        whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(2L))

        val access = providerFor(UserActor(principalId, setOf("viewer", "warehouse_manager"))).current()

        assertThat(access).isEqualTo(OnlyWarehouses(setOf(2L)))
    }

    @Test
    fun `일반 담당자는 배정된 창고만 접근한다`() = runTest {
        whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(setOf(1L, 3L))

        val access = providerFor(UserActor(principalId, setOf("warehouse_manager"))).current()

        assertThat(access).isEqualTo(OnlyWarehouses(setOf(1L, 3L)))
    }

    @Test
    fun `배정이 없는 담당자는 어떤 창고에도 접근하지 못한다`() = runTest {
        whenever(warehouseMemberRepository.findWarehouseIdsByPrincipalId(principalId)).thenReturn(emptySet())

        val access = providerFor(UserActor(principalId, setOf("warehouse_manager"))).current()

        assertThat(access.canAccess(1L)).isFalse()
    }
}
