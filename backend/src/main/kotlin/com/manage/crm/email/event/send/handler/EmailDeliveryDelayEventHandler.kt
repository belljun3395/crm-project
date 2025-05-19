package com.manage.crm.email.event.send.handler

import com.manage.crm.email.event.send.EmailDeliveryDelayEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class EmailDeliveryDelayEventHandler() {
    val log = KotlinLogging.logger {}

    suspend fun handle(event: EmailDeliveryDelayEvent) {
    }
}
