package com.dozycoffee.wms.support

import com.dozycoffee.wms.global.security.Actor
import com.dozycoffee.wms.global.security.CurrentActorProvider
import com.dozycoffee.wms.global.security.SystemActor

/** 요청 컨텍스트가 없는 영속성 테스트에서 감사 주체를 시스템으로 고정한다 */
class SystemActorProvider : CurrentActorProvider {
    override suspend fun get(): Actor = SystemActor
}
