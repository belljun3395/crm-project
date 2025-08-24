package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.support.VariablesSupport
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.user.domain.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

@Service
class EmailContentService(
    private val objectMapper: ObjectMapper,
    private val campaignEventsService: CampaignEventsService
) {
    private val log = KotlinLogging.logger {}

    /**
     *  - 알림 프로퍼티와 사용자 정보를 기반으로 이메일 콘텐츠를 생성합니다.
     *      - 만약 프로퍼티에 변수가 없다면 NonContent를 반환합니다.
     *      - 변수가 있다면 사용자 속성에서 해당 변수를 추출하여 VariablesContent를 반환합니다.
     */
    suspend fun genUserEmailContent(
        user: User,
        notificationVariables: NotificationEmailTemplateVariablesModel,
        campaignId: Long?
    ): Content {
        return if (notificationVariables.isNoVariables()) {
            NonContent()
        } else {
            try {
                // Start with user attribute variables
                val userAttributes = user.userAttributes
                val variables = notificationVariables.variables
                val templateKeys = variables.getVariables(false).toSet()

                val userAttributeMap = templateKeys.associateWith { key ->
                    VariablesSupport.doAssociate(objectMapper, key, userAttributes, variables).second
                }

                // Merge with campaign event variables (event variables take priority)
                val finalVariables = campaignId?.let { cId ->
                    val eventVariables = getCampaignEventVariables(cId, templateKeys)
                    userAttributeMap + eventVariables // Event variables override user variables
                } ?: userAttributeMap

                VariablesContent(finalVariables)
            } catch (e: Exception) {
                log.error(e) { "Failed to generate email content for user ${user.id}, campaignId: $campaignId" }
                NonContent()
            }
        }
    }

    suspend fun getCampaignEventVariables(campaignId: Long, allowedKeys: Set<String>): Map<String, String> {
        try {
            val events = campaignEventsService.findAllEventsByCampaignId(campaignId)
            if (events.isEmpty()) return emptyMap()

            val eventVariables = mutableMapOf<String, String>()

            // Process events deterministically (sorted by event ID for consistency)
            events.sortedBy { it.id }.forEach { event ->
                val keys = event.properties.getKeys()
                for (key in keys) {
                    // Only include variables that are whitelisted by template
                    if (allowedKeys.contains(key)) {
                        val value = event.properties.getValue(key)
                        eventVariables[key] = value
                    }
                }
            }

            log.debug { "Retrieved ${eventVariables.size} event variables for campaignId: $campaignId" }
            return eventVariables
        } catch (e: Exception) {
            log.error(e) { "Failed to get campaign event variables for campaignId: $campaignId" }
            return emptyMap()
        }
    }
}
