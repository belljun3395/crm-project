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
import com.manage.crm.event.domain.vo.EventProperties
import com.manage.crm.event.domain.vo.EventProperty
import com.manage.crm.event.service.CampaignDashboardEvent
import com.manage.crm.event.service.CampaignDashboardService
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.support.out
import com.manage.crm.user.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

enum class SaveEventMessage(val message: String) {
    EVENT_SAVE_SUCCESS("Event saved successfully"),
    EVENT_SAVE_WITH_CAMPAIGN("Event saved with campaign"),
    EVENT_SAVE_BUT_NOT_CAMPAIGN("Event saved but not in campaign"),
    PROPERTIES_MISMATCH("Campaign properties and Event properties mismatch")
}

/**
 * UC-EVENT-001
 * Records a user event and optionally links it to a campaign.
 *
 * Input: event name, user externalId, property list, and optional campaign name.
 * Success: persists event and returns event id with result message.
 * Failure: throws when user is not found by externalId.
 * Side effects: may create campaign-event relation and publish dashboard stream event.
 */
@Service
class PostEventUseCase(
    private val eventRepository: EventRepository,
    private val campaignRepository: CampaignRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
    private val campaignCacheManager: CampaignCacheManager,
    private val userRepository: UserRepository,
    private val segmentTargetingService: SegmentTargetingService,
    private val campaignDashboardService: CampaignDashboardService
) {
    val log = KotlinLogging.logger {}

    suspend fun execute(useCaseIn: PostEventUseCaseIn): PostEventUseCaseOut {
        val eventName = useCaseIn.name
        val externalId = useCaseIn.externalId
        val segmentId = useCaseIn.segmentId
        val properties = useCaseIn.properties
        val campaignName = useCaseIn.campaignName

        val targetUserIds = resolveTargetUserIds(externalId, segmentId)
        val savedEvents = targetUserIds.map { userId ->
            saveEvent(eventName, userId, properties)
        }
        val firstSavedEvent = savedEvents.firstOrNull()
            ?: throw NotFoundByException("User", "segmentId", segmentId ?: -1L)
        val firstSavedEventId = requireNotNull(firstSavedEvent.id)

        val campaign = try {
            findCampaign(campaignName)
        } catch (e: NotFoundByException) {
            log.warn { "Campaign not found: ${e.message}" }
            return out {
                PostEventUseCaseOut(
                    firstSavedEventId,
                    if (segmentId != null) {
                        "Event saved for segment users (${savedEvents.size}) but not in campaign"
                    } else {
                        SaveEventMessage.EVENT_SAVE_BUT_NOT_CAMPAIGN.message
                    }
                )
            }
        }

        if (campaign != null) {
            val eventProperties = requireNotNull(firstSavedEvent.properties) { "Event properties cannot be null" }
            if (!campaign.allMatchPropertyKeys(eventProperties.getKeys())) {
                log.warn { "Properties mismatch between campaign and event. Campaign: ${campaign.properties.getKeys()}, Event: ${eventProperties.getKeys()}" }
                return out {
                    PostEventUseCaseOut(firstSavedEventId, SaveEventMessage.PROPERTIES_MISMATCH.message)
                }
            }

            savedEvents.forEach { savedEvent ->
                setCampaignEvent(campaign, savedEvent)
            }
        }

        val message = when {
            segmentId != null && campaign != null -> "Event saved with campaign for segment users (${savedEvents.size})"
            segmentId != null -> "Event saved for segment users (${savedEvents.size})"
            campaign != null -> SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN.message
            else -> SaveEventMessage.EVENT_SAVE_SUCCESS.message
        }

        return out {
            PostEventUseCaseOut(firstSavedEventId, message)
        }
    }

    private suspend fun resolveTargetUserIds(externalId: String, segmentId: Long?): List<Long> {
        if (segmentId != null) {
            return segmentTargetingService.resolveUserIds(segmentId)
        }

        val userId = userRepository.findByExternalId(externalId)?.id
            ?: throw NotFoundByException("User", "externalId", externalId)
        return listOf(userId)
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
                properties = EventProperties(
                    properties.map {
                        EventProperty(
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
