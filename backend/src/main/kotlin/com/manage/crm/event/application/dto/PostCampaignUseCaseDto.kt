package com.manage.crm.event.application.dto

data class PostCampaignUseCaseIn(
    val name: String,
    val properties: List<PostCampaignPropertyDto>
)

data class PostCampaignPropertyDto(
    val key: String,
    val value: String
)

data class PostCampaignUseCaseOut(
    val id: Long,
    val name: String,
    val properties: List<PostCampaignPropertyDto>
)

class PostCampaignUseCaseDto
