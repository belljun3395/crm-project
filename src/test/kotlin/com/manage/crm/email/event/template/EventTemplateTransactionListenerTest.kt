package com.manage.crm.email.event.template

import com.manage.crm.email.MailEventInvokeSituationTest
import com.manage.crm.email.domain.EmailTemplate
import com.manage.crm.email.domain.repository.EmailTemplateRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.modulith.test.Scenario

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
                variables = emptyList()
            )
            emailTemplateRepository.save(emailTemplate) // save email template
            // when
            run {
                emailTemplate.modify()
                    .modifySubject("newSubject")
                    .modifyBody("newBody", emptyList())
                    .done()
                emailTemplateRepository.save(emailTemplate) // modify email template

                val event = emailTemplate.domainEvents.first() as PostEmailTemplateEvent
                `when`(postEmailTemplateEventHandler.handle(event)).thenReturn(Unit)

                // then
                run {
                    scenario.publish(event)
                        .andWaitForEventOfType(PostEmailTemplateEvent::class.java)
                        .toArriveAndAssert { _, _ ->
                            // then
                            runBlocking {
                                verify(postEmailTemplateEventHandler, times(1)).handle(event)
                            }
                        }
                }
            }
        }
    }
}
