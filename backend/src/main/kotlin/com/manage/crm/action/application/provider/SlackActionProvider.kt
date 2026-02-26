package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchOut
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class SlackActionProvider(
    webClientBuilder: WebClient.Builder
) : WebhookBasedActionProvider(webClientBuilder) {
    override val channel: ActionChannel = ActionChannel.SLACK

    override suspend fun dispatch(request: ActionProviderRequest): ActionDispatchOut {
        return postJson(
            destination = request.destination,
            payload = mapOf("text" to request.body)
        )
    }
}
