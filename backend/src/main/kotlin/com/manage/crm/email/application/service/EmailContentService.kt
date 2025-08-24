package com.manage.crm.email.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.email.application.dto.Content
import com.manage.crm.email.application.dto.NonContent
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.domain.model.NotificationEmailTemplateVariablesModel
import com.manage.crm.email.domain.support.VariablesSupport
import com.manage.crm.user.domain.User
import org.springframework.stereotype.Service

@Service
class EmailContentService(
    private val objectMapper: ObjectMapper
) {
    /**
     *  - 알림 프로퍼티와 사용자 정보를 기반으로 이메일 콘텐츠를 생성합니다.
     *      - 만약 프로퍼티에 변수가 없다면 NonContent를 반환합니다.
     *      - 변수가 있다면 사용자 속성에서 해당 변수를 추출하여 VariablesContent를 반환합니다.
     */
    fun genUserEmailContent(user: User, notificationVariables: NotificationEmailTemplateVariablesModel): Content {
        return if (notificationVariables.isNoVariables()) {
            NonContent()
        } else {
            val attributes = user.userAttributes
            val variables = notificationVariables.variables
            variables.getVariables(false)
                .associate { key ->
                    VariablesSupport.doAssociate(objectMapper, key, attributes, variables)
                }.let {
                    VariablesContent(it)
                }
        }
    }
}
