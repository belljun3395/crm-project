package com.manage.crm.event.application.port.query

import java.time.LocalDateTime

data class EventReadModel(
    val id: Long,
    val userId: Long,
    val name: String,
    val createdAt: LocalDateTime?,
)

interface EventReadPort {
    suspend fun findAllByIdIn(ids: Collection<Long>): List<EventReadModel>

    suspend fun findAllByUserIdIn(userIds: Collection<Long>): List<EventReadModel>
}
