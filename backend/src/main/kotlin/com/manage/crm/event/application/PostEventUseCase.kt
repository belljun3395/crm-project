package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.dto.PostEventUseCaseOut
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.event.service.CampaignDashboardEvent
import com.manage.crm.event.service.CampaignDashboardService
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.support.out
import com.manage.crm.user.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import java.time.LocalDateTime

enum class SaveEventMessage(val message: String) {
    EVENT_SAVE_SUCCESS("Event saved successfully"),
    EVENT_SAVE_WITH_CAMPAIGN("Event saved with campaign"),
    EVENT_SAVE_BUT_NOT_CAMPAIGN("Event saved but not in campaign"),
    PROPERTIES_MISMATCH("Campaign properties and Event properties mismatch")
}

data class SavedEvent(
    val id: Long,
    val message: String
) {
    constructor(id: Long, message: SaveEventMessage) : this(id, message.message)
}

@Service
class PostEventUseCase(
    private val eventRepository: EventRepository,
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val campaignCacheManager: CampaignCacheManager,
    private val userRepository: UserRepository,
    private val campaignDashboardService: CampaignDashboardService
) {
    val log = KotlinLogging.logger {}

    suspend fun execute(useCaseIn: PostEventUseCaseIn): PostEventUseCaseOut {
        val eventName = useCaseIn.name
        val externalId = useCaseIn.externalId
        val properties = useCaseIn.properties
        val campaignName = useCaseIn.campaignName

        val userId = userRepository.findByExternalId(externalId)?.id
            ?: throw NotFoundByException("User", "externalId", externalId)

        val savedEvent = getSavedEvent(eventName, userId, properties, campaignName)

        return out {
            PostEventUseCaseOut(savedEvent.id, savedEvent.message)
        }
    }

    private suspend fun getSavedEvent(eventName: String, userId: Long, properties: List<PostEventPropertyDto>, campaignName: String?): SavedEvent {
        return supervisorScope {
            val eventDeferred = async(Dispatchers.IO) {
                saveEvent(eventName, userId, properties)
            }
            val campaignDeferred = async(Dispatchers.IO) {
                findCampaign(campaignName)
            }

            val event = eventDeferred.await()
            val eventId = requireNotNull(event.id)
            val campaign = try {
                campaignDeferred.await()
            } catch (e: NotFoundByException) {
                log.warn { "Campaign not found: ${e.message}" }
                return@supervisorScope SavedEvent(eventId, SaveEventMessage.EVENT_SAVE_BUT_NOT_CAMPAIGN)
            }

            campaign
                ?.let {
                    val eventProperties = requireNotNull(event.properties) { "Event properties cannot be null" }
                    if (!it.allMatchPropertyKeys(eventProperties.getKeys())) {
                        log.warn { "Properties mismatch between campaign and event. Campaign: ${it.properties.getKeys()}, Event: ${eventProperties.getKeys()}" }
                        return@let SavedEvent(eventId, SaveEventMessage.PROPERTIES_MISMATCH)
                    }
                    setCampaignEvent(campaign, event)
                    SavedEvent(eventId, SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN)
                } ?: SavedEvent(eventId, SaveEventMessage.EVENT_SAVE_SUCCESS)
        }
    }

    private suspend fun saveEvent(
        eventName: String,
        userId: Long,
        properties: List<PostEventPropertyDto>
    ): Event {
        return eventRepository.save(
            Event.new(
                name = eventName,
                userId = userId,
                properties = Properties(
                    properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
        )
    }

    private suspend fun findCampaign(campaignName: String?): Campaign? {
        return campaignName?.let { name ->
            campaignCacheManager.loadAndSaveIfMiss(Campaign.UNIQUE_FIELDS.NAME, name) {
                campaignRepository.findCampaignByName(name) ?: throw NotFoundByException("Campaign", "name", name)
            }
        }
    }

    private suspend fun setCampaignEvent(campaign: Campaign, savedEvent: Event) {
        campaignEventsRepository.save(
            CampaignEvents.new(
                campaignId = campaign.id!!,
                eventId = savedEvent.id!!
            )
        )

        try {
            val dashboardEvent = CampaignDashboardEvent(
                campaignId = campaign.id!!,
                eventId = savedEvent.id!!,
                userId = savedEvent.userId,
                eventName = savedEvent.name,
                timestamp = savedEvent.createdAt ?: LocalDateTime.now()
            )
            campaignDashboardService.publishCampaignEvent(dashboardEvent)
            log.debug { "Published campaign event to dashboard stream: campaignId=${campaign.id}, eventId=${savedEvent.id}" }
        } catch (e: Exception) {
            log.error(e) { "Failed to publish event to dashboard stream: campaignId=${campaign.id}, eventId=${savedEvent.id}" }
            // Don't fail the entire operation if stream publishing fails
        }
    }
}
