package com.manage.crm.support.coroutine

import com.manage.crm.support.mdc.MDC_KEY_TRACE_ID
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import kotlin.coroutines.CoroutineContext

suspend fun <T> withMDCContext(
    context: CoroutineContext = Dispatchers.IO,
    block: suspend () -> T
): T {
    return withContext(context + MDCContext()) { block() }
}

fun mdcCoroutineScope(
    context: CoroutineContext = Dispatchers.IO,
    traceId: String = MDC.getCopyOfContextMap()?.get(MDC_KEY_TRACE_ID) ?: ""
): CoroutineScope {
    val contextMap = MDC.getCopyOfContextMap() ?: emptyMap<String?, String?>()
        .toMutableMap()
        .apply {
            put(
                MDC_KEY_TRACE_ID,
                traceId
            )
        }
    return CoroutineScope(context + MDCContext(contextMap))
}

fun eventListenerCoroutineScope(
    context: CoroutineContext = Dispatchers.IO,
    traceId: String = MDC.getCopyOfContextMap()?.get(MDC_KEY_TRACE_ID) ?: ""
): CoroutineScope {
    return CoroutineScope(mdcCoroutineScope(context, traceId).coroutineContext + SupervisorJob())
}
