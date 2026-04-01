package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionChannel
import com.manage.crm.action.application.ActionDispatchOut
import com.manage.crm.action.application.ActionDispatchStatus
import com.manage.crm.email.application.dto.SendEmailInDto
import com.manage.crm.email.application.dto.VariablesContent
import com.manage.crm.email.application.service.MailService
import com.manage.crm.email.domain.vo.SentEmailStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class EmailActionProvider(
    @Qualifier("mailServiceImpl")
    private val mailService: MailService,
) : ActionProvider {
    override val channel: ActionChannel = ActionChannel.EMAIL

    override suspend fun dispatch(request: ActionProviderRequest): ActionDispatchOut {
        if (request.destination.isBlank()) {
            return ActionDispatchOut(
                status = ActionDispatchStatus.FAILED,
                channel = channel,
                destination = request.destination,
                errorCode = "EMPTY_DESTINATION",
                errorMessage = "Email destination is required",
            )
        }

        return runCatching {
            val result =
                mailService.send(
                    SendEmailInDto(
                        to = request.destination,
                        subject = request.subject ?: "(no subject)",
                        template = request.body,
                        content = VariablesContent(request.variables),
                        emailBody = request.body,
                        destination = request.destination,
                        eventType = SentEmailStatus.SEND,
                    ),
                )

            ActionDispatchOut(
                status = ActionDispatchStatus.SUCCESS,
                channel = channel,
                destination = request.destination,
                providerMessageId = result.messageId,
            )
        }.getOrElse { error ->
            ActionDispatchOut(
                status = ActionDispatchStatus.FAILED,
                channel = channel,
                destination = request.destination,
                errorCode = "EMAIL_SEND_FAILED",
                errorMessage = error.message,
            )
        }
    }
}
