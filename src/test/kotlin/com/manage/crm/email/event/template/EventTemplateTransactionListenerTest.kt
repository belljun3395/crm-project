package com.manage.crm.email.event.template

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import com.manage.crm.email.domain.vo.Variables
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.modulith.test.Scenario
import kotlin.test.assertEquals

class EventTemplateTransactionListenerTest(
    val emailTemplateRepository: EmailTemplateRepository
) : MailEventInvokeSituationTest() {

    @Test
    fun `email template is modified`(scenario: Scenario) {
        runTest {
            // given
            val emailTemplate = EmailTemplate.new(
                templateName = "templateName",
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
            `when`(postEmailTemplateEventHandler.handle(event)).thenReturn(Unit)

            // then
            val expectedInvocationTime = 1
            scenario.publish(event)
                .andWaitForStateChange(
                    { mockingDetails(postEmailTemplateEventHandler).invocations.size },
                    { mockingDetails(postEmailTemplateEventHandler).invocations.size == expectedInvocationTime }
                )
                .andVerify { invocationTime ->
                    assertEquals(invocationTime, expectedInvocationTime)
                }
        }
    }
}
