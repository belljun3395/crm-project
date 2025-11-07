package com.manage.crm.email.application.dto

data class BrowseEmailSendHistoriesUseCaseIn(
    val userId: Long?,
    val sendStatus: String?
)

data class BrowseEmailSendHistoriesUseCaseOut(
    val histories: List<EmailSendHistoryDto>
)

data class EmailSendHistoryDto(
    val id: Long,
    val userId: Long,
    val userEmail: String,
    val emailMessageId: String,
    val emailBody: String,
    val sendStatus: String,
    val createdAt: String,
    val updatedAt: String
)
