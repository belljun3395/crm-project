package com.manage.crm.event.application.dto

import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import java.time.LocalDateTime

data class SearchEventsUseCaseIn(
    val eventName: String,
    val propertyAndOperations: List<PropertyAndOperationDto>
)

data class PropertyAndOperationDto(
    val properties: List<SearchEventPropertyDto>,
    val operation: Operation,
    val joinOperation: JoinOperation
)

data class SearchEventPropertyDto(
    val key: String,
    val value: String
)

data class SearchEventsUseCaseOut(
    val events: List<EventDto>
)

data class EventDto(
    val id: Long,
    val name: String,
    val externalId: String?,
    val properties: List<SearchEventPropertyDto>,
    val createdAt: LocalDateTime
)

class SearchEventsUseCaseDto
