package com.manage.crm.event.service

import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.exception.NotFoundByIdException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Coordinates campaign-event relation queries and materializes full event payloads.
 */
@Service
class CampaignEventsService(
    private val campaignRepository: CampaignRepository,
    private val eventRepository: EventRepository,
    private val campaignEventsRepository: CampaignEventsRepository,
) {
    /**
     * Returns all events linked to a campaign by joining through campaign-event relation ids.
     */
    suspend fun findAllEventsByCampaignId(campaignId: Long): List<Event> {
        val eventIds = campaignEventsRepository.findEventIdsByCampaignId(campaignId)
        return eventRepository.findAllByIdIn(eventIds)
    }

    /**
     * Returns campaign events for a single user by relation lookup and event hydration.
     */
    suspend fun findAllEventsByCampaignIdAndUserId(
        campaignId: Long,
        userId: Long,
    ): List<Event> {
        val eventIds = campaignEventsRepository.findEventIdsByCampaignIdAndUserId(campaignId, userId)
        return eventRepository.findAllByIdIn(eventIds)
    }

    /**
     * Resolves campaign events with optional time filtering.
     *
     * Responsibility:
     * 1. Validate campaign existence.
     * 2. Prefer repository-level time range filtering when both bounds are provided.
     * 3. Apply null-safe in-memory filtering only for partial bounds.
     */
    suspend fun findCampaignEvents(
        campaignId: Long,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): List<Event> {
        campaignRepository.findById(campaignId) ?: throw NotFoundByIdException("Campaign", campaignId)

        val eventIds =
            when {
                startTime != null && endTime != null ->
                    campaignEventsRepository.findEventIdsByCampaignIdAndCreatedAtRange(
                        campaignId,
                        startTime,
                        endTime,
                    )

                else -> campaignEventsRepository.findEventIdsByCampaignId(campaignId)
            }
        if (eventIds.isEmpty()) {
            return emptyList()
        }

        val events = eventRepository.findAllByIdIn(eventIds.distinct())
        if (startTime != null && endTime != null) {
            return events
        }
        if (startTime == null && endTime == null) {
            return events
        }

        return events.filter { event ->
            val createdAt = event.createdAt ?: return@filter false
            val startInclusive = startTime?.let { createdAt >= it } ?: true
            val endExclusive = endTime?.let { createdAt < it } ?: true
            startInclusive && endExclusive
        }
    }
}
