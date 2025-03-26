package com.manage.crm.event.domain.repository.query

import com.manage.crm.event.domain.JoinOperation
import com.manage.crm.event.domain.Operation
import com.manage.crm.event.domain.vo.Properties

data class SearchByPropertyQuery(
    val eventName: String,
    val properties: Properties,
    val operation: Operation,
    val joinOperation: JoinOperation = JoinOperation.END
)

class EventQuery
