package com.manage.crm.email.application

import com.manage.crm.email.application.dto.PostTemplateUseCaseIn
import com.manage.crm.email.application.dto.PostTemplateUseCaseOut
import com.manage.crm.email.application.service.EmailTemplateRepositoryEventProcessor
import com.manage.crm.email.application.service.HtmlService
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.support.stringListToVariables
import com.manage.crm.email.exception.VariablesNotMatchException
import com.manage.crm.support.exception.DuplicateByException
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UC-EMAIL-001
 * Creates or updates an email template with validated variables.
 *
 * Input: template id (optional), name, subject, body, version (optional), and variable list.
 * Success: persists template and returns id/name/version.
 * Failure: throws on duplicate template name, missing template id, or variable mismatch.
 * Side effects: normalizes template body through HTML formatting before persistence.
 */
@Service
class PostTemplateUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateSaveRepository: EmailTemplateRepositoryEventProcessor,
    private val htmlService: HtmlService
) {

    @Transactional
    suspend fun execute(useCaseIn: PostTemplateUseCaseIn): PostTemplateUseCaseOut {
        val id: Long? = useCaseIn.id
        val templateName = useCaseIn.templateName
        val subject: String? = useCaseIn.subject
        val version: Float? = useCaseIn.version
        val body = htmlService.prettyPrintHtml(useCaseIn.body)
        val variables = run {
            val bodyVariables = htmlService.extractVariables(body)
                .sorted().stringListToVariables()

            val variables = useCaseIn.variables
                .filterNot { it.isBlank() }
                .filterNot { it.isEmpty() }
                .sorted().stringListToVariables()

            if (bodyVariables.getVariables() != variables.getVariables()) {
                throw VariablesNotMatchException(bodyVariables.getDisplayVariables(), variables.getDisplayVariables())
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

        modifiedOrNewTemplate = emailTemplateSaveRepository.save(modifiedOrNewTemplate)

        return out {
            PostTemplateUseCaseOut(
                id = modifiedOrNewTemplate.id!!,
                templateName = modifiedOrNewTemplate.templateName,
                version = modifiedOrNewTemplate.version.value
            )
        }
    }

    private suspend fun getEmailTemplate(id: Long?, templateName: String): EmailTemplate? {
        return when {
            id != null -> {
                emailTemplateRepository.findById(id) ?: throw NotFoundByIdException("EmailTemplate", id)
            }

            emailTemplateRepository.findByTemplateName(templateName) != null -> {
                throw DuplicateByException("EmailTemplate", "templateName", templateName)
            }

            else -> null
        }
    }
}
