package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("journeys")
class Journey(
    @Id
    var id: Long? = null,
    @Column("name")
    var name: String,
    @Column("trigger_type")
    var triggerType: String,
    @Column("trigger_event_name")
    var triggerEventName: String? = null,
    @Column("trigger_segment_id")
    var triggerSegmentId: Long? = null,
    @Column("active")
    var active: Boolean = true,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            name: String,
            triggerType: String,
            triggerEventName: String?,
            triggerSegmentId: Long?,
            active: Boolean
        ): Journey {
            return Journey(
                name = name,
                triggerType = triggerType,
                triggerEventName = triggerEventName,
                triggerSegmentId = triggerSegmentId,
                active = active
            )
        }
    }
}
