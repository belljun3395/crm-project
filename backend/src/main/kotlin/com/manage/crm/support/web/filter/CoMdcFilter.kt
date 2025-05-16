package com.manage.crm.support.web.filter

import com.manage.crm.support.coroutine.MdcContinuationInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

@Component
class CoMdcFilter : CoWebFilter() {
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        withContext(MdcContinuationInterceptor(Dispatchers.Unconfined)) {
            chain.filter(exchange)
        }
    }
}
