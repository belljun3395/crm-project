package com.manage.crm.email.event.schedule

import com.manage.crm.email.domain.vo.EventId

abstract class ScheduledEvent

// ----------------- Scheduled Event -----------------
class CancelScheduledEvent(
    val scheduledEventId: EventId
) : ScheduledEvent()
