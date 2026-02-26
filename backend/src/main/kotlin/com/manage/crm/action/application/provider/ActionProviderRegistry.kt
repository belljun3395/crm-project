package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionChannel
import org.springframework.stereotype.Component

@Component
class ActionProviderRegistry(
    providers: List<ActionProvider>
) {
    private val providerByChannel = providers.associateBy { it.channel }

    fun get(channel: ActionChannel): ActionProvider {
        return providerByChannel[channel]
            ?: throw IllegalArgumentException("No action provider registered for channel: $channel")
    }
}
