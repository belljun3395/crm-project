package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Event
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EventRepository : CoroutineCrudRepository<Event, Long>, EventRepositoryCustom {
    suspend fun findAllByName(name: String): List<Event>
    suspend fun findAllByIdIn(ids: List<Long>): List<Event>
}
