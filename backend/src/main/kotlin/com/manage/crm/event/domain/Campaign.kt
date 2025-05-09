package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.Properties
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("campaigns")
class Campaign(
    @Id
    var id: Long? = null,
    @Column("name")
    var name: String? = null,
    @Column("properties")
    var properties: Properties? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null
) {
    companion object {
        fun new(
            name: String,
            properties: Properties
        ): Campaign {
            return Campaign(
                name = name,
                properties = properties
            )
        }

        fun new(
            id: Long?,
            name: String,
            properties: Properties,
            createdAt: LocalDateTime
        ): Campaign {
            return Campaign(
                id = id,
                name = name,
                properties = properties,
                createdAt = createdAt
            )
        }
    }

    /**
     * Check if the campaign has exactly the same property keys as the given list of keys.
     */
    fun allMatchPropertyKeys(keys: List<String>): Boolean {
        val campaignPropertyKeys = properties?.getKeys() ?: return false
        return campaignPropertyKeys.size == keys.size && campaignPropertyKeys.containsAll(keys)
    }
}
