package com.manage.crm.action.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("action_dispatch_histories")
class ActionDispatchHistory(
    @Id
    var id: Long? = null,
    @Column("channel")
    var channel: String,
    @Column("status")
    var status: String,
    @Column("destination")
    var destination: String,
    @Column("subject")
    var subject: String? = null,
    @Column("body")
    var body: String,
    @Column("variables_json")
    var variablesJson: String? = null,
    @Column("provider_message_id")
    var providerMessageId: String? = null,
    @Column("error_code")
    var errorCode: String? = null,
    @Column("error_message")
    var errorMessage: String? = null,
    @Column("campaign_id")
    var campaignId: Long? = null,
    @Column("journey_execution_id")
    var journeyExecutionId: Long? = null,
    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime? = null,
) {
    companion object {
        fun new(
            channel: String,
            status: String,
            destination: String,
            subject: String?,
            body: String,
            variablesJson: String?,
            providerMessageId: String?,
            errorCode: String?,
            errorMessage: String?,
            campaignId: Long?,
            journeyExecutionId: Long?,
        ): ActionDispatchHistory =
            ActionDispatchHistory(
                channel = channel,
                status = status,
                destination = destination,
                subject = subject,
                body = body,
                variablesJson = variablesJson,
                providerMessageId = providerMessageId,
                errorCode = errorCode,
                errorMessage = errorMessage,
                campaignId = campaignId,
                journeyExecutionId = journeyExecutionId,
            )
    }
}
