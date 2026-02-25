package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchOut
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class DiscordActionProvider(
    webClientBuilder: WebClient.Builder
) : WebhookBasedActionProvider(webClientBuilder) {
    override val channel: ActionChannel = ActionChannel.DISCORD

    override suspend fun dispatch(request: ActionProviderRequest): ActionDispatchOut {
        return postJson(
            destination = request.destination,
            payload = mapOf("content" to request.body)
        )
    }
}
