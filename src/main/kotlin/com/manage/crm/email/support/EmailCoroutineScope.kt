package com.manage.crm.email.support

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

object EmailCoroutineScope {
    private val sharedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun eventListenerScope(context: CoroutineContext? = null, supervisorJob: CompletableJob? = null): CoroutineScope {
        if (context == null && supervisorJob == null) {
            return sharedScope
        }
        val ctx = context ?: Dispatchers.IO
        val job = supervisorJob ?: SupervisorJob()
        return CoroutineScope(ctx + job)
    }
}
