package com.manage.crm.user.domain

import com.manage.crm.user.domain.vo.Json
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
class User(
    @Id
    var id: Long? = null,
    @Column("external_id")
    var externalId: String? = null,
    @Column("user_attributes")
    var userAttributes: Json? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null
) {
    fun updateAttributes(attributes: Json) {
        userAttributes = attributes
    }
}
