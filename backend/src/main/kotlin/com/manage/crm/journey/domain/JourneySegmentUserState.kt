package com.manage.crm.journey.domain

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journey_segment_user_states")
class JourneySegmentUserState(
    @Id
    var id: Long? = null,
    @Column("journey_id")
    var journeyId: Long,
    @Column("user_id")
    var userId: Long,
    @Column("in_segment")
    var inSegment: Boolean,
    @Column("attributes_hash")
    var attributesHash: String? = null,
    @Column("transition_version")
    var transitionVersion: Long = 0L,
    @LastModifiedDate
    @Column("updated_at")
    var updatedAt: LocalDateTime? = null,
) {
    companion object {
        fun new(
            journeyId: Long,
            userId: Long,
            inSegment: Boolean,
            attributesHash: String?,
            transitionVersion: Long,
        ): JourneySegmentUserState =
            JourneySegmentUserState(
                journeyId = journeyId,
                userId = userId,
                inSegment = inSegment,
                attributesHash = attributesHash,
                transitionVersion = transitionVersion,
            )
    }
}
