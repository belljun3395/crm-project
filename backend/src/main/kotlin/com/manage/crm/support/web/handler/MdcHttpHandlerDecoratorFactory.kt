package com.manage.crm.support.web.handler

import com.manage.crm.support.mdc.MDC_KEY_TRACE_ID
import org.slf4j.MDC
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory
import org.springframework.stereotype.Component
import reactor.util.context.Context
import java.util.*

@Component
class MdcHttpHandlerDecoratorFactory : HttpHandlerDecoratorFactory {
    override fun apply(httpHandler: HttpHandler): HttpHandler {
        return HttpHandler { request, response ->
            val uuid = UUID.randomUUID().toString()
            try {
                MDC.put(MDC_KEY_TRACE_ID, uuid)
                httpHandler.handle(request, response)
                    .contextWrite {
                        val originContext = it.stream()
                            .map { ctx -> ctx.key to ctx.value }
                            .toList()
                            .toMap().toMutableMap()
                        val newContext = addContext(originContext, MDC_KEY_TRACE_ID, uuid)
                        Context.of(newContext)
                    }
            } finally {
                MDC.clear()
            }
        }
    }

    private fun addContext(originContext: MutableMap<Any, Any>, key: String, value: String): Map<Any, Any> {
        originContext[key] = value
        return originContext
    }
}
