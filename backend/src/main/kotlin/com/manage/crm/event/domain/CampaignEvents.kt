package com.manage.crm.event.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("campaign_events")
class CampaignEvents(
    @Id
    var id: Long? = null,
    @Column("campaign_id")
    var campaignId: Long,
    @Column("event_id")
    var eventId: Long,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            campaignId: Long,
            eventId: Long
        ): CampaignEvents {
            return CampaignEvents(
                campaignId = campaignId,
                eventId = eventId
            )
        }
    }
}
