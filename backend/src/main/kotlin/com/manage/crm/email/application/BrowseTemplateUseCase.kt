package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseTemplateUseCaseIn
import com.manage.crm.email.application.dto.BrowseTemplateUseCaseOut
import com.manage.crm.email.application.dto.TemplateDto
import com.manage.crm.email.application.dto.TemplateHistoryDto
import com.manage.crm.email.application.dto.TemplateWithHistoryDto
import com.manage.crm.email.domain.repository.EmailTemplateHistoryRepository
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

/**
 *  - `templateWithHistories`: `withHistory`에 따라 `Template`과 `History`를 조회 후 조합한 결과
 */
@Service
class BrowseTemplateUseCase(
    private val emailTemplateRepository: EmailTemplateRepository,
    private val emailTemplateHistoryRepository: EmailTemplateHistoryRepository
) {
    suspend fun execute(useCaseIn: BrowseTemplateUseCaseIn): BrowseTemplateUseCaseOut {
        val withHistory = useCaseIn.withHistory

        val templates = emailTemplateRepository.findAll().toList()
        val histories = takeIf { withHistory }
            ?.let {
                templates
                    .map { it.id!! }
                    .let { emailTemplateHistoryRepository.findAllByTemplateIdInOrderByVersionDesc(it) }
                    .groupBy { it.templateId }
            }

        val templateWithHistories = templates
            .map {
                val history = histories?.get(it.id)
                it to history
            }

        return out {
            templateWithHistories
                .map {
                    val template = it.first
                    val templateHistories = it.second
                    TemplateWithHistoryDto(
                        template =
                        TemplateDto(
                            id = template.id!!,
                            templateName = template.templateName!!,
                            subject = template.subject!!,
                            body = template.body!!,
                            variables = template.variables.getVariables(),
                            version = template.version!!.value,
                            createdAt = template.createdAt.toString()
                        ),
                        histories =
                        templateHistories?.map { history ->
                            TemplateHistoryDto(
                                id = history.id!!,
                                templateId = history.templateId!!,
                                subject = history.subject!!,
                                body = history.body!!,
                                variables = history.variables.getVariables(),
                                version = history.version!!.value,
                                createdAt = history.createdAt.toString()
                            )
                        } ?: emptyList()
                    )
                }
                .let {
                    BrowseTemplateUseCaseOut(it)
                }
        }
    }
}
