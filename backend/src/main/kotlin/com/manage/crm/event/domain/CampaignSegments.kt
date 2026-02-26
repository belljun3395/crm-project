package com.manage.crm.event.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("campaign_segments")
class CampaignSegments(
    @Id
    var id: Long? = null,
    @Column("campaign_id")
    var campaignId: Long,
    @Column("segment_id")
    var segmentId: Long,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            campaignId: Long,
            segmentId: Long
        ): CampaignSegments {
            return CampaignSegments(
                campaignId = campaignId,
                segmentId = segmentId
            )
        }
    }
}
