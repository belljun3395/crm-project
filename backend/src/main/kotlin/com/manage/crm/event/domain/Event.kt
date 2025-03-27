package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.Properties
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("events")
class Event(
    @Id
    var id: Long? = null,
    @Column("name")
    var name: String? = null,
    @Column("user_id")
    var userId: Long? = null,
    @Column("properties")
    var properties: Properties? = null,
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (id != other.id) return false
        if (name != other.name) return false
        if (userId != other.userId) return false
        if (properties != other.properties) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (userId?.hashCode() ?: 0)
        result = 31 * result + (properties?.hashCode() ?: 0)
        result = 31 * result + (createdAt?.hashCode() ?: 0)
        return result
    }
}
