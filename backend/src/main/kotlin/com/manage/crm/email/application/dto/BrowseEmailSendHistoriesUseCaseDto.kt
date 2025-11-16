package com.manage.crm.email.application.dto

data class BrowseEmailSendHistoriesUseCaseIn(
    val userId: Long?,
    val sendStatus: String?,
    val page: Int = 0,
    val size: Int = 20
)

data class BrowseEmailSendHistoriesUseCaseOut(
    val histories: List<EmailSendHistoryDto>,
    val totalCount: Int,
    val page: Int,
    val size: Int
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
