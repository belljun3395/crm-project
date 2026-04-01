package com.manage.crm.action.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.provider.ActionProviderRegistry
import com.manage.crm.action.application.provider.ActionProviderRequest
import com.manage.crm.action.domain.ActionDispatchHistory
import com.manage.crm.action.domain.repository.ActionDispatchHistoryRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class ActionDispatchService(
    private val actionProviderRegistry: ActionProviderRegistry,
    private val actionDispatchHistoryRepository: ActionDispatchHistoryRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun dispatch(input: ActionDispatchIn): ActionDispatchOut {
        val renderedDestination = VariableTemplateRenderer.render(input.destination, input.variables)
        val renderedSubject = input.subject?.let { VariableTemplateRenderer.render(it, input.variables) }
        val renderedBody = VariableTemplateRenderer.render(input.body, input.variables)

        val output =
            runCatching {
                val provider = actionProviderRegistry.get(input.channel)
                provider.dispatch(
                    ActionProviderRequest(
                        destination = renderedDestination,
                        subject = renderedSubject,
                        body = renderedBody,
                        variables = input.variables,
                    ),
                )
            }.getOrElse { error ->
                ActionDispatchOut(
                    status = ActionDispatchStatus.FAILED,
                    channel = input.channel,
                    destination = renderedDestination,
                    errorCode = "PROVIDER_DISPATCH_ERROR",
                    errorMessage = error.message,
                )
            }

        actionDispatchHistoryRepository.save(
            ActionDispatchHistory.new(
                channel = output.channel.name,
                status = output.status.name,
                destination = output.destination,
                subject = renderedSubject,
                body = renderedBody,
                variablesJson = toVariablesJson(input.variables),
                providerMessageId = output.providerMessageId,
                errorCode = output.errorCode,
                errorMessage = output.errorMessage,
                campaignId = input.campaignId,
                journeyExecutionId = input.journeyExecutionId,
            ),
        )

        return output
    }

    suspend fun browse(
        campaignId: Long?,
        journeyExecutionId: Long?,
    ): List<ActionDispatchHistoryDto> {
        val histories =
            when {
                campaignId != null -> actionDispatchHistoryRepository.findAllByCampaignIdOrderByCreatedAtDesc(campaignId)
                journeyExecutionId != null ->
                    actionDispatchHistoryRepository.findAllByJourneyExecutionIdOrderByCreatedAtDesc(
                        journeyExecutionId,
                    )
                else -> actionDispatchHistoryRepository.findAllByOrderByCreatedAtDesc()
            }

        return histories
            .let { flow ->
                if (campaignId != null && journeyExecutionId != null) {
                    flow.filter { it.journeyExecutionId == journeyExecutionId }
                } else {
                    flow
                }
            }.toList()
            .map { history ->
                ActionDispatchHistoryDto(
                    id = requireNotNull(history.id) { "ActionDispatchHistory id cannot be null" },
                    channel = history.channel,
                    status = history.status,
                    destination = history.destination,
                    subject = history.subject,
                    body = history.body,
                    variables = fromVariablesJson(history.variablesJson),
                    providerMessageId = history.providerMessageId,
                    errorCode = history.errorCode,
                    errorMessage = history.errorMessage,
                    campaignId = history.campaignId,
                    journeyExecutionId = history.journeyExecutionId,
                    createdAt = history.createdAt?.format(formatter) ?: "",
                )
            }
    }

    private fun toVariablesJson(variables: Map<String, String>): String {
        if (variables.isEmpty()) {
            return "{}"
        }
        return objectMapper.writeValueAsString(variables)
    }

    private fun fromVariablesJson(variablesJson: String?): Map<String, String> {
        if (variablesJson.isNullOrBlank()) {
            return emptyMap()
        }

        return runCatching {
            objectMapper.readValue(variablesJson, object : TypeReference<Map<String, String>>() {})
        }.getOrElse {
            emptyMap()
        }
    }
}
