package com.manage.crm.event.controller.request

data class PostCampaignRequest(
    val name: String,
    val properties: List<PostCampaignPropertyDto>
)

data class PostCampaignPropertyDto(
    val key: String,
    val value: String
)
