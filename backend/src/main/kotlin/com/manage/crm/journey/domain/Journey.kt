package com.manage.crm.journey.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

/**
 * Journey aggregate root that stores trigger metadata and lifecycle state.
 */
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
    @Column("trigger_segment_event")
    var triggerSegmentEvent: String? = null,
    @Column("trigger_segment_watch_fields")
    var triggerSegmentWatchFields: String? = null,
    @Column("trigger_segment_count_threshold")
    var triggerSegmentCountThreshold: Long? = null,
    @Column("active")
    var active: Boolean = true,
    @Column("lifecycle_status")
    var lifecycleStatus: String = "ACTIVE",
    @Column("version")
    var version: Int = 1,
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
            triggerSegmentEvent: String?,
            triggerSegmentWatchFields: String?,
            triggerSegmentCountThreshold: Long?,
            active: Boolean,
            lifecycleStatus: String = "ACTIVE",
            version: Int = 1
        ): Journey {
            return Journey(
                name = name,
                triggerType = triggerType,
                triggerEventName = triggerEventName,
                triggerSegmentId = triggerSegmentId,
                triggerSegmentEvent = triggerSegmentEvent,
                triggerSegmentWatchFields = triggerSegmentWatchFields,
                triggerSegmentCountThreshold = triggerSegmentCountThreshold,
                active = active,
                lifecycleStatus = lifecycleStatus,
                version = version
            )
        }
    }
}
