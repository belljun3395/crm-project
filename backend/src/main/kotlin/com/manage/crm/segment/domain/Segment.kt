package com.manage.crm.segment.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("segments")
class Segment(
    @Id
    var id: Long? = null,
    @Column("name")
    var name: String,
    @Column("description")
    var description: String? = null,
    @Column("is_active")
    var active: Boolean = true,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            id: Long? = null,
            name: String,
            description: String?,
            active: Boolean
        ): Segment {
            return Segment(
                id = id,
                name = name,
                description = description,
                active = active
            )
        }
    }
}
