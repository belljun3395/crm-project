package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.support.VariablesSupport
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.service.CampaignEventsService
import com.manage.crm.user.domain.User
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.collections.emptyMap

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
     *      - 캠페인 ID가 제공되면 해당 캠페인 이벤트의 프로퍼티도 포함하여 최종 변수를 구성합니다.
     */
    suspend fun genUserEmailContent(user: User, notificationVariables: NotificationEmailTemplateVariablesModel, campaignId: Long?): Content? {
        return if (notificationVariables.isNoVariables()) {
            NonContent()
        } else {
            try {
                val userAttributes = user.userAttributes
                val userAttributeVariables = VariablesSupport.associateUserAttribute(
                    userAttributes,
                    notificationVariables.getUserVariables(),
                    objectMapper
                )
                val eventVariables = campaignId?.let { cId ->
                    return@let getCampaignEventProperties(cId, user.id!!)?.let { it ->
                        val campaignVariables = notificationVariables.variables
                        VariablesSupport.associateCampaignEventProperty(it, campaignVariables)
                    }
                } ?: emptyMap()

                val finalVariables = userAttributeVariables + eventVariables
                VariablesContent(finalVariables)
            } catch (e: Exception) {
                log.error(e) { "Failed to generate email content for user ${user.id}, campaignId: $campaignId" }
                return null
            }
        }
    }

    suspend fun getCampaignEventProperties(campaignId: Long, userId: Long): Properties? {
        try {
            val events = campaignEventsService.findAllEventsByCampaignIdAndUserId(campaignId, userId)
            return events.sortedBy { it.id }.firstOrNull()?.properties
        } catch (e: Exception) {
            log.error(e) { "Failed to get campaign event variables for campaignId: $campaignId" }
            return null
        }
    }
}
