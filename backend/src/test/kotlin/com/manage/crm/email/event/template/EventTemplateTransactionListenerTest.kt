package com.manage.crm.email.event.template

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class EventTemplateTransactionListenerTest(
    val emailTemplateRepository: EmailTemplateRepository
) : MailEventInvokeSituationTest() {

    @AfterEach
    fun cleanup() = runTest {
        emailTemplateRepository.deleteAll()
    }

    @Test
    fun `email template is modified`() {
        runTest {
            // given
            val uniqueName = "templateName-${UUID.randomUUID()}"
            val emailTemplate = EmailTemplate.new(
                templateName = uniqueName,
                subject = "subject",
                body = "body",
                variables = Variables(emptyList())
            )
            emailTemplateRepository.save(emailTemplate) // save email template
            // when
            emailTemplate.modify()
                .modifySubject("newSubject")
                .modifyBody("newBody", Variables(emptyList()))
                .done()
            emailTemplateRepository.save(emailTemplate) // modify email template

            val event = emailTemplate.domainEvents.first() as PostEmailTemplateEvent

            // then
            assertEquals(emailTemplate.id, event.templateId)
        }
    }
}
