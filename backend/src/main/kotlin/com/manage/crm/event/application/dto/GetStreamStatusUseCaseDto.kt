package com.manage.crm.event.application.dto

import java.time.LocalDateTime

data class GetStreamStatusUseCaseIn(
    val campaignId: Long
)

data class GetStreamStatusUseCaseOut(
    val campaignId: Long,
    val streamLength: Long,
    val checkedAt: LocalDateTime
)
