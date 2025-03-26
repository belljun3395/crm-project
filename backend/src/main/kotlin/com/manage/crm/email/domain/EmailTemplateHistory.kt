package com.manage.crm.email.domain

import com.manage.crm.email.domain.vo.Variables
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

// TODO: templateId 와 version을 기준으로 유니크 해야한다.
@Table("email_template_histories")
class EmailTemplateHistory(
    @Id
    var id: Long? = null,
    @Column("template_id")
    var templateId: Long? = null,
    @Column("subject")
    var subject: String? = null,
    @Column("body")
    var body: String? = null,
    @Column("variables")
    var variables: Variables = Variables(),
    @Column("version")
    var version: Float = 1.0f,
    @CreatedDate
    var createdAt: LocalDateTime? = null
)
