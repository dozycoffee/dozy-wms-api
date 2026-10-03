package com.dozycoffee.wms.global.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ActorTest {

    @Test
    fun `사용자의 감사 이름은 principalId 문자열이다`() {
        val principalId = UUID.fromString("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")

        assertThat(UserActor(principalId, setOf("inbound_manager")).auditName)
            .isEqualTo("0199a3c4-7b2e-7c1a-9f3d-2b6e8a1c4d5f")
    }

    @Test
    fun `시스템 작업의 감사 이름은 system이다`() {
        assertThat(SystemActor.auditName).isEqualTo("system")
    }
}
