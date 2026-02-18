package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.support.VariableResolverContext
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.user.domain.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class EmailContentService(
    private val objectMapper: ObjectMapper,
    private val campaignEventsService: CampaignEventsService,
    private val resolverRegistry: VariableResolverRegistry
) {
    private val log = KotlinLogging.logger {}

    /**
     * Generates email content for a single user by resolving all template variables
     * through the [VariableResolverRegistry].
     *
     * - Returns [NonContent] when the template has no variables.
     * - Returns [VariablesContent] with all resolved key-value pairs when variables are present.
     * - Both the new-format key (`user.email`) and the legacy key (`user_email`) are included
     *   in the resolved map so that HTML templates written in either syntax work correctly.
     */
    suspend fun genUserEmailContent(
        user: User,
        notificationVariables: NotificationEmailTemplateVariablesModel,
        campaignId: Long?
    ): Content? {
        if (notificationVariables.isNoVariables()) {
            return NonContent()
        }

        return try {
            val userId = user.id
                ?: run {
                    log.error { "User id is null, cannot generate email content for campaignId: $campaignId" }
                    return null
                }
            val eventProperties: EventProperties? = campaignId?.let { getCampaignEventProperties(it, userId) }

            val context = VariableResolverContext(
                userAttributes = user.userAttributes,
                eventProperties = eventProperties,
                objectMapper = objectMapper
            )

            val resolvedVariables = resolverRegistry.resolveAll(notificationVariables.variables, context)
            VariablesContent(resolvedVariables)
        } catch (e: Exception) {
            log.error(e) { "Failed to generate email content for user ${user.id}, campaignId: $campaignId" }
            null
        }
    }

    suspend fun getCampaignEventProperties(campaignId: Long, userId: Long): EventProperties? {
        return try {
            val events = campaignEventsService.findAllEventsByCampaignIdAndUserId(campaignId, userId)
            events.sortedBy { it.id }.firstOrNull()?.properties
        } catch (e: Exception) {
            log.error(e) { "Failed to get campaign event variables for campaignId: $campaignId" }
            null
        }
    }
}
