package com.manage.crm.user.support

import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import com.manage.crm.user.event.NewUserEvent
import com.manage.crm.user.event.RefreshTotalUsersCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class UserEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate
) {
    val log = KotlinLogging.logger {}

    suspend fun publish(event: NewUserEvent) {
        transactionSynchronizationTemplate.afterCompletion(
            Dispatchers.Default + MDCContext(),
            "publish event: $event"
        ) {
            applicationEventPublisher.publishEvent(event)
        }
    }

    suspend fun publish(event: RefreshTotalUsersCommand) {
        applicationEventPublisher.publishEvent(event)
    }
}
