package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseTemplateVariableCatalogUseCaseIn
import com.manage.crm.email.application.dto.BrowseTemplateVariableCatalogUseCaseOut
import com.manage.crm.email.application.dto.TemplateVariableCatalogItemDto
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BrowseTemplateVariableCatalogUseCase(
    private val campaignRepository: CampaignRepository
) {

    suspend fun execute(useCaseIn: BrowseTemplateVariableCatalogUseCaseIn): BrowseTemplateVariableCatalogUseCaseOut {
        val campaigns = when (val campaignId = useCaseIn.campaignId) {
            null -> campaignRepository.findAll().toList()
            else -> listOfNotNull(campaignRepository.findById(campaignId))
        }

        val campaignKeys = campaigns
            .flatMap { it.properties.getKeys() }
            .distinct()
            .sorted()

        return out {
            BrowseTemplateVariableCatalogUseCaseOut(
                userVariables = listOf(
                    TemplateVariableCatalogItemDto(
                        key = "user.email",
                        source = "USER",
                        description = "사용자 이메일 (필수)",
                        required = true
                    ),
                    TemplateVariableCatalogItemDto(
                        key = "user.name",
                        source = "USER",
                        description = "사용자 이름 (개인화 권장)",
                        required = false
                    )
                ),
                campaignVariables = campaignKeys.map { key ->
                    TemplateVariableCatalogItemDto(
                        key = "campaign.$key",
                        source = "CAMPAIGN",
                        description = "캠페인 이벤트 속성 키: $key",
                        required = false
                    )
                }
            )
        }
    }
}
