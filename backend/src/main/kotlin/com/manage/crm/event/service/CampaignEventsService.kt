package com.manage.crm.event.service

import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.EventRepository
import org.springframework.stereotype.Service

@Service
class CampaignEventsService(
    private val campaignEventsRepository: CampaignEventsRepository,
    private val eventsRepository: EventRepository
) {
    suspend fun findAllEventsByCampaignIdAndUserId(campaignId: Long): List<Event> {
        val eventIds = campaignEventsRepository.findAllByCampaignId(campaignId).map { it.eventId }
        return eventsRepository.findAllByIdIn(eventIds)
    }

    suspend fun findAllEventsByCampaignIdAndUserId(campaignId: Long, userId: Long): List<Event> {
        val eventIds = campaignEventsRepository.findAllByCampaignIdAndUserId(campaignId, userId).map { it.eventId }
        return eventsRepository.findAllByIdIn(eventIds)
    }
}
