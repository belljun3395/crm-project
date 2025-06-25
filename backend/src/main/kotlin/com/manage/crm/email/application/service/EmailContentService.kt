package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplatePropertiesModel
import com.manage.crm.email.domain.support.VariablesSupport
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
        notificationProperties: NotificationEmailTemplatePropertiesModel,
        campaignId: Long?
    ): Content {
        return if (notificationProperties.isNoVariables()) {
            NonContent()
        } else {
            val attributes = user.userAttributes
            val variables = notificationProperties.variables
            val userAttributeVariables = variables.getVariables(false)
                .associate { key ->
                    VariablesSupport.doAssociate(objectMapper, key, attributes, variables)
                }.let {
                    VariablesContent(it)
                }
            return campaignId?.let { campaignId ->
                campaignEventsRepository.findTopByEventId(campaignId)?.let { it ->
                    eventRepository.findById(it.eventId)?.let {
                        val variables = mutableMapOf<String, String>()
                        val keys = it.properties.getKeys()
                        for (key in keys) {
                            val value = it.properties.getValue(key)
                            variables[key] = value
                        }
                        VariablesContent(variables)
                    }
                }
            }
                ?.let { userAttributeVariables.merge(it) }
                ?: userAttributeVariables
        }
    }
}
