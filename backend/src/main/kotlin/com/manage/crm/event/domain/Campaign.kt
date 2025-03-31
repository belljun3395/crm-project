package com.manage.crm.event.domain

import com.manage.crm.event.domain.vo.Properties
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
    @Column("created_at")
    var createdAt: LocalDateTime? = null
) {
    /**
     * Check if the campaign has exactly the same property keys as the given list of keys.
     */
    fun allMatchPropertyKeys(keys: List<String>): Boolean {
        val campaignPropertyKeys = properties?.getKeys() ?: return false
        return campaignPropertyKeys.size == keys.size && campaignPropertyKeys.containsAll(keys)
    }
}
