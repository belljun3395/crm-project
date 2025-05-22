package com.manage.crm.user.event

abstract class UserEvent

class RefreshTotalUsersCommand(
    val oldTotalUsers: Long
) : UserEvent()
