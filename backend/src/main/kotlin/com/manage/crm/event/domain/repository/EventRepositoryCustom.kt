package com.manage.crm.event.domain.repository

import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.query.SearchByPropertyQuery

interface EventRepositoryCustom {
    suspend fun searchByProperty(query: SearchByPropertyQuery): List<Event>
    suspend fun searchByProperties(queries: List<SearchByPropertyQuery>): List<Event>
}
