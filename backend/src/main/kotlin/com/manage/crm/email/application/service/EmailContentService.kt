package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.support.VariablesSupport
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.user.domain.User
import org.springframework.stereotype.Service

@Service
class EmailContentService(
    private val objectMapper: ObjectMapper,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventRepository: EventRepository
) {
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
            // Start with user attribute variables
            val userAttributes = user.userAttributes
            val variables = notificationVariables.variables
            val userAttributeMap = variables.getVariables(false)
                .associate { key ->
                    VariablesSupport.doAssociate(objectMapper, key, userAttributes, variables)
                }

            // Merge with campaign event variables (event variables take priority)
            val finalVariables = campaignId?.let { cId ->
                val eventVariables = getCampaignEventVariables(cId)
                // Create whitelisted event variables based on template variables
                val allowedEventVariables = filterEventVariablesByWhitelist(eventVariables, variables)
                userAttributeMap + allowedEventVariables // Event variables override user variables
            } ?: userAttributeMap
            
            VariablesContent(finalVariables)
        }
    }

    private fun filterEventVariablesByWhitelist(eventVariables: Map<String, String>, templateVariables: Variables): Map<String, String> {
        val allowedKeys = templateVariables.getVariables(false).map { key ->
            // Extract the actual key from the template variable format
            when {
                key.startsWith("attribute_") -> key.substringAfter("attribute_")
                key.startsWith("custom_") -> key.substringAfter("custom_").substringBefore("_")
                else -> key
            }
        }.toSet()
        
        return eventVariables.filterKeys { key ->
            allowedKeys.contains(key)
        }
    }

    suspend fun getCampaignEventVariables(campaignId: Long): Map<String, String> {
        val campaignEvents = campaignEventsRepository.findAllByCampaignId(campaignId)
        if (campaignEvents.isEmpty()) return emptyMap()
        
        val eventIds = campaignEvents.map { it.eventId }
        val events = eventRepository.findAllByIdIn(eventIds)
        if (events.isEmpty()) return emptyMap()
        
        val eventVariables = mutableMapOf<String, String>()
        events.forEach { event ->
            val keys = event.properties.getKeys()
            for (key in keys) {
                val value = event.properties.getValue(key)
                // Event variables override user variables for the same key
                eventVariables[key] = value
            }
        }
        return eventVariables
    }
}
