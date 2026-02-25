package com.manage.crm.segment.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("segment_conditions")
class SegmentCondition(
    @Id
    var id: Long? = null,
    @Column("segment_id")
    var segmentId: Long,
    @Column("field_name")
    var fieldName: String,
    @Column("operator")
    var operator: String,
    @Column("value_type")
    var valueType: String,
    @Column("condition_value")
    var conditionValue: String,
    @Column("position")
    var position: Int,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            segmentId: Long,
            fieldName: String,
            operator: String,
            valueType: String,
            conditionValue: String,
            position: Int
        ): SegmentCondition {
            return SegmentCondition(
                segmentId = segmentId,
                fieldName = fieldName,
                operator = operator,
                valueType = valueType,
                conditionValue = conditionValue,
                position = position
            )
        }
    }
}
