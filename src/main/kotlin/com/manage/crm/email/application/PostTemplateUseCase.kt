package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.service.HtmlService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.EmailTemplateHistory
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import com.manage.crm.support.out
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 *  - `modifiedOrNewTemplate`:  요청에 따라 수정한 템플릿이거나 새롭게 생성한 템플릿
 */
@Service
class PostTemplateUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository,
    private val htmlService: HtmlService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    @Transactional
    suspend fun execute(useCaseIn: PostTemplateUseCaseIn): PostTemplateUseCaseOut {
        val id: Long? = useCaseIn.id
        val templateName = useCaseIn.templateName
        val subject: String? = useCaseIn.subject
        val version: Float? = useCaseIn.version
        val body = htmlService.prettyPrintHtml(useCaseIn.body)
        val variables = run {
            val bodyVariables = htmlService.extractVariables(body).sorted().let { Variables(it) }
            val variables = useCaseIn.variables
                .filterNot { it.isBlank() }
                .filterNot { it.isEmpty() }
                .sorted()
                .let { Variables(it) }

            if (bodyVariables.getVariables(false) != variables.getVariables(false)) {
                throw IllegalArgumentException("Variables do not match: \n${bodyVariables.getVariables(false)} != ${variables.getVariables(false)}")
            }
            return@run variables
        }

        val persistedTemplate: EmailTemplate? = getEmailTemplate(id, templateName)
        var modifiedOrNewTemplate =
            persistedTemplate
                ?.modify()
                ?.modifySubject(subject)
                ?.modifyBody(body, variables)
                ?.updateVersion(version)
                ?.done()
                ?: run {
                    EmailTemplate.new(
                        templateName = templateName,
                        subject = subject!!,
                        body = body,
                        variables = variables
                    )
                }
        val modifiedOrNewTemplateEvents = modifiedOrNewTemplate.domainEvents

        modifiedOrNewTemplate = modifiedOrNewTemplate.let { template ->
            if (template.isNewTemplate()) {
                emailTemplateRepository.save(template).let {
                    emailTemplateHistoryRepository.save(
                        EmailTemplateHistory(
                            templateId = it.id!!,
                            subject = it.subject,
                            body = it.body,
                            variables = it.variables,
                            version = it.version
                        )
                    )
                    it
                }
            } else {
                emailTemplateRepository.save(template)
            }
        }

        applicationEventPublisher.publishEvent(modifiedOrNewTemplateEvents)

        return out {
            PostTemplateUseCaseOut(
                id = modifiedOrNewTemplate.id!!,
                templateName = modifiedOrNewTemplate.templateName!!,
                version = modifiedOrNewTemplate.version
            )
        }
    }

    private suspend fun getEmailTemplate(id: Long?, templateName: String): EmailTemplate? {
        return when {
            id != null -> {
                emailTemplateRepository.findById(id) ?: throw IllegalArgumentException("Email Template not found by id: $id")
            }

            emailTemplateRepository.findByTemplateName(templateName) != null -> {
                throw IllegalArgumentException("Duplicate template name: $templateName")
            }

            else -> null
        }
    }
}
