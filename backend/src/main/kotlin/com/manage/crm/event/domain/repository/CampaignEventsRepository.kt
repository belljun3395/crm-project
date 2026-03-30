package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.CampaignEvents
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Repository for querying campaign-linked events and aggregate counts.
 */
interface CampaignEventsRepository : CoroutineCrudRepository<CampaignEvents, Long>, CampaignEventsCustomRepository
