package com.manage.crm.email.event.template

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import java.util.UUID
import kotlin.test.assertEquals

class EventTemplateTransactionListenerTest(
    val emailTemplateRepository: EmailTemplateRepository,
) : MailEventInvokeSituationTest() {
    init {
        given("email template repository") {
            afterEach {
                emailTemplateRepository.deleteAll()
            }

            then("email template is modified") {
                val uniqueName = "templateName-${UUID.randomUUID()}"
                val emailTemplate =
                    EmailTemplate.new(
                        templateName = uniqueName,
                        subject = "subject",
                        body = "body",
                        variables = Variables(emptyList()),
                    )
                emailTemplateRepository.save(emailTemplate)

                emailTemplate
                    .modify()
                    .modifySubject("newSubject")
                    .modifyBody("newBody", Variables(emptyList()))
                    .done()
                emailTemplateRepository.save(emailTemplate)

                val event = emailTemplate.domainEvents.first() as PostEmailTemplateEvent

                assertEquals(emailTemplate.id, event.templateId)
            }
        }
    }
}
