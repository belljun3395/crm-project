package com.manage.crm.event.service

import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CampaignEventsService(
    private val campaignRepository: CampaignRepository,
    private val eventRepository: EventRepository,
    private val campaignEventsRepository: CampaignEventsRepository
) {
    suspend fun findAllEventsByCampaignIdAndUserId(campaignId: Long): List<Event> {
        val eventIds = campaignEventsRepository.findEventIdsByCampaignId(campaignId)
        return eventRepository.findAllByIdIn(eventIds)
    }

    suspend fun findAllEventsByCampaignIdAndUserId(campaignId: Long, userId: Long): List<Event> {
        val eventIds = campaignEventsRepository.findEventIdsByCampaignIdAndUserId(campaignId, userId)
        return eventRepository.findAllByIdIn(eventIds)
    }

    suspend fun findCampaignEvents(
        campaignId: Long,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?
    ): List<Event> {
        campaignRepository.findById(campaignId) ?: throw NotFoundByIdException("Campaign", campaignId)

        val eventIds = when {
            startTime != null && endTime != null -> campaignEventsRepository.findEventIdsByCampaignIdAndCreatedAtRange(
                campaignId,
                startTime,
                endTime
            )

            else -> campaignEventsRepository.findEventIdsByCampaignId(campaignId)
        }
        if (eventIds.isEmpty()) {
            return emptyList()
        }

        val events = eventRepository.findAllByIdIn(eventIds.distinct())
        return events.filter { event ->
            val createdAt = event.createdAt
            if ((startTime != null || endTime != null) && createdAt == null) {
                return@filter false
            }
            val startInclusive = startTime?.let { createdAt == null || createdAt >= it } ?: true
            val endExclusive = endTime?.let { createdAt == null || createdAt < it } ?: true
            startInclusive && endExclusive
        }
    }
}
