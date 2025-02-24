package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Event
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EventRepository : CoroutineCrudRepository<Event, Long>, EventRepositoryCustom {
    fun findAllByName(name: String): List<Event>
}
