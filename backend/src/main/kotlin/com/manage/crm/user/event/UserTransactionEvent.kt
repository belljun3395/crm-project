package com.manage.crm.user.event

abstract class UserTransactionEvent

abstract class UserTransactionAfterCompletionEvent : UserTransactionEvent()

class NewUserEvent(
    val userId: Long
) : UserTransactionAfterCompletionEvent()
