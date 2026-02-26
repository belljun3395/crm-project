package com.manage.crm.action.application

enum class ActionChannel {
    EMAIL,
    SLACK,
    DISCORD;

    companion object {
        fun from(value: String): ActionChannel {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported channel: $value")
        }
    }
}

enum class ActionDispatchStatus {
    SUCCESS,
    FAILED
}

data class ActionDispatchIn(
    val channel: ActionChannel,
    val destination: String,
    val subject: String?,
    val body: String,
    val variables: Map<String, String>,
    val campaignId: Long?,
    val journeyExecutionId: Long?
)

data class ActionDispatchOut(
    val status: ActionDispatchStatus,
    val channel: ActionChannel,
    val destination: String,
    val providerMessageId: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null
)

data class ActionDispatchHistoryDto(
    val id: Long,
    val channel: String,
    val status: String,
    val destination: String,
    val subject: String?,
    val body: String,
    val variables: Map<String, String>,
    val providerMessageId: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val campaignId: Long?,
    val journeyExecutionId: Long?,
    val createdAt: String
)
