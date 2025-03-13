package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto

interface NonVariablesMailService {
    suspend fun send(args: SendEmailInDto): SendEmailOutDto
}
