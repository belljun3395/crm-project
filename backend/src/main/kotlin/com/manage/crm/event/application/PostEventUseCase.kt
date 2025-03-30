package com.manage.crm.event.application

import arrow.fx.coroutines.parZip
import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.dto.PostEventUseCaseOut
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.out
import com.manage.crm.user.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import org.springframework.stereotype.Service

enum class SaveEventMessage(val message: String) {
    EVENT_SAVE_SUCCESS("Event saved successfully"),
    EVENT_SAVE_WITH_CAMPAIGN("Event saved with campaign"),
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
    private val userRepository: UserRepository
) {

    suspend fun execute(useCaseIn: PostEventUseCaseIn): PostEventUseCaseOut {
        val eventName = useCaseIn.name
        val externalId = useCaseIn.externalId
        val properties = useCaseIn.properties
        val campaignName = useCaseIn.campaignName

        val userId = userRepository.findByExternalId(externalId)?.id
            ?: throw IllegalArgumentException("User not found by externalId: $externalId")

        val savedEvent = parZip(
            Dispatchers.IO,
            { saveEvent(eventName, userId, properties) },
            { findCampaign(campaignName) }
        ) { event, campaign ->
            campaign?.let {
                if (!it.allMatchPropertyKeys(event.properties!!.getKeys())) {
                    return@parZip SavedEvent(event.id!!, SaveEventMessage.PROPERTIES_MISMATCH)
                }
                setCampaignEvent(campaign, event)
                SavedEvent(event.id!!, SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN)
            } ?: SavedEvent(event.id!!, SaveEventMessage.EVENT_SAVE_SUCCESS)
        }

        return out { PostEventUseCaseOut(savedEvent.id, savedEvent.message) }
    }

    private suspend fun saveEvent(
        eventName: String,
        userId: Long,
        properties: List<PostEventPropertyDto>
    ): Event {
        return eventRepository.save(
            Event(
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
        return campaignName?.let {
            campaignRepository.findCampaignByName(it)
                ?: throw IllegalArgumentException("Campaign not found: $it")
        }
    }

    private suspend fun setCampaignEvent(campaign: Campaign, savedEvent: Event) {
        campaignEventsRepository.save(
            CampaignEvents(
                campaignId = campaign.id!!,
                eventId = savedEvent.id!!
            )
        )
    }
}
