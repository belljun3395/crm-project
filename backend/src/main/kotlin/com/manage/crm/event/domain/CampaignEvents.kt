package com.manage.crm.event.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("campaign_events")
class CampaignEvents(
    @Id
    var id: Long? = null,
    @Column("campaign_id")
    var campaignId: Long? = null,
    @Column("event_id")
    var eventId: Long? = null,
    @Column("created_at")
    var createdAt: LocalDateTime? = null
)
