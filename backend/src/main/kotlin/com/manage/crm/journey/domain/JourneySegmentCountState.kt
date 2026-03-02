package com.manage.crm.journey.domain

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_segment_count_states")
class JourneySegmentCountState(
    @Id
    var id: Long? = null,
    @Column("journey_id")
    var journeyId: Long,
    @Column("last_count")
    var lastCount: Long,
    @Column("transition_version")
    var transitionVersion: Long = 0L,
    @LastModifiedDate
    @Column("updated_at")
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            journeyId: Long,
            lastCount: Long,
            transitionVersion: Long
        ): JourneySegmentCountState {
            return JourneySegmentCountState(
                journeyId = journeyId,
                lastCount = lastCount,
                transitionVersion = transitionVersion
            )
        }
    }
}
