package com.manage.crm.support.coroutine

import com.manage.crm.support.mdc.MDC_KEY_TRACE_ID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.ReactorContext
import org.slf4j.MDC
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

class MdcContinuationInterceptor(private val dispatcher: CoroutineDispatcher) : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        return MdcContinuation(dispatcher.interceptContinuation(continuation))
    }
}

class MdcContinuation<T>(private val continuation: Continuation<T>) : Continuation<T> {
    override val context: CoroutineContext
        get() = continuation.context

    override fun resumeWith(result: Result<T>) {
        continuation.context[ReactorContext]?.context?.get<String>(MDC_KEY_TRACE_ID)?.run {
            MDC.put(MDC_KEY_TRACE_ID, this)
        }
        continuation.resumeWith(result)
    }
}
