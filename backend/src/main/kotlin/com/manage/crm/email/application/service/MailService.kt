package com.manage.crm.email.application.service

import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.SendEmailOutDto

interface MailService {
    /**
     * 이메일을 전송합니다.
     */
    suspend fun send(args: SendEmailInDto): SendEmailOutDto
}
