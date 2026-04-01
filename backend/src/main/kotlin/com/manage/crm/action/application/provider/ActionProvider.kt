package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchOut

data class ActionProviderRequest(
    val destination: String,
    val subject: String?,
    val body: String,
    val variables: Map<String, String>,
)

interface ActionProvider {
    val channel: ActionChannel

    suspend fun dispatch(request: ActionProviderRequest): ActionDispatchOut
}
