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
)
