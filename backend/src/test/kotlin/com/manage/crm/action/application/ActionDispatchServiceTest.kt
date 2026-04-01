package com.manage.crm.action.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.action.application.provider.ActionProvider
import com.manage.crm.action.application.provider.ActionProviderRegistry
import com.manage.crm.action.application.provider.ActionProviderRequest
import com.manage.crm.action.domain.ActionDispatchHistory
import com.manage.crm.action.domain.repository.ActionDispatchHistoryRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class ActionDispatchServiceTest :
    BehaviorSpec({
        lateinit var actionProviderRegistry: ActionProviderRegistry
        lateinit var actionDispatchHistoryRepository: ActionDispatchHistoryRepository
        lateinit var provider: ActionProvider
        lateinit var service: ActionDispatchService

        beforeTest {
            actionProviderRegistry = mockk()
            actionDispatchHistoryRepository = mockk()
            provider = mockk()
            service = ActionDispatchService(actionProviderRegistry, actionDispatchHistoryRepository, ObjectMapper())
        }

        afterTest { (_, _) ->
            clearMocks(actionProviderRegistry, actionDispatchHistoryRepository, provider)
        }

        given("ActionDispatchService.dispatch") {
            `when`("provider succeeds") {
                then("render templates, dispatch via provider, and persist success history") {
                    val input =
                        ActionDispatchIn(
                            channel = ActionChannel.SLACK,
                            destination = "https://hooks.slack.com/services/T000/B000/XXX",
                            subject = "{{name}} alert",
                            body = "hello {{name}}",
                            variables = mapOf("name" to "crm"),
                            campaignId = 10L,
                            journeyExecutionId = 20L,
                        )

                    val providerRequestSlot = slot<ActionProviderRequest>()
                    every { actionProviderRegistry.get(ActionChannel.SLACK) } returns provider
                    coEvery { provider.dispatch(capture(providerRequestSlot)) } returns
                        ActionDispatchOut(
                            status = ActionDispatchStatus.SUCCESS,
                            channel = ActionChannel.SLACK,
                            destination = input.destination,
                            providerMessageId = "provider-1",
                        )
                    coEvery { actionDispatchHistoryRepository.save(any()) } answers {
                        firstArg<ActionDispatchHistory>().apply {
                            id = 1L
                            createdAt = LocalDateTime.now()
                        }
                    }

                    val result = service.dispatch(input)

                    result.status shouldBe ActionDispatchStatus.SUCCESS
                    result.providerMessageId shouldBe "provider-1"

                    providerRequestSlot.captured.subject shouldBe "crm alert"
                    providerRequestSlot.captured.body shouldBe "hello crm"

                    val historySlot = slot<ActionDispatchHistory>()
                    coVerify(exactly = 1) { actionDispatchHistoryRepository.save(capture(historySlot)) }
                    historySlot.captured.status shouldBe ActionDispatchStatus.SUCCESS.name
                    historySlot.captured.subject shouldBe "crm alert"
                    historySlot.captured.body shouldBe "hello crm"
                    historySlot.captured.campaignId shouldBe 10L
                    historySlot.captured.journeyExecutionId shouldBe 20L
                }
            }

            `when`("provider lookup fails") {
                then("return failed status and persist failure history") {
                    val input =
                        ActionDispatchIn(
                            channel = ActionChannel.DISCORD,
                            destination = "https://discord.com/api/webhooks/1/2",
                            subject = null,
                            body = "hello",
                            variables = emptyMap(),
                            campaignId = null,
                            journeyExecutionId = null,
                        )

                    every { actionProviderRegistry.get(ActionChannel.DISCORD) } throws IllegalArgumentException("missing provider")
                    coEvery { actionDispatchHistoryRepository.save(any()) } answers {
                        firstArg<ActionDispatchHistory>().apply {
                            id = 2L
                            createdAt = LocalDateTime.now()
                        }
                    }

                    val result = service.dispatch(input)

                    result.status shouldBe ActionDispatchStatus.FAILED
                    result.errorCode shouldBe "PROVIDER_DISPATCH_ERROR"

                    val historySlot = slot<ActionDispatchHistory>()
                    coVerify(exactly = 1) { actionDispatchHistoryRepository.save(capture(historySlot)) }
                    historySlot.captured.status shouldBe ActionDispatchStatus.FAILED.name
                    historySlot.captured.errorCode shouldBe "PROVIDER_DISPATCH_ERROR"
                }
            }
        }

        given("ActionDispatchService.browse") {
            `when`("campaign and journey filters are both provided") {
                then("filter by campaign first then journey execution id") {
                    val historyA =
                        ActionDispatchHistory
                            .new(
                                channel = "EMAIL",
                                status = "SUCCESS",
                                destination = "a@example.com",
                                subject = null,
                                body = "ok",
                                variablesJson = "{\"name\":\"crm\"}",
                                providerMessageId = null,
                                errorCode = null,
                                errorMessage = null,
                                campaignId = 99L,
                                journeyExecutionId = 1L,
                            ).apply {
                                id = 100L
                                createdAt = LocalDateTime.of(2026, 2, 25, 11, 0, 0)
                            }

                    val historyB =
                        ActionDispatchHistory
                            .new(
                                channel = "EMAIL",
                                status = "SUCCESS",
                                destination = "b@example.com",
                                subject = null,
                                body = "ok",
                                variablesJson = "{}",
                                providerMessageId = null,
                                errorCode = null,
                                errorMessage = null,
                                campaignId = 99L,
                                journeyExecutionId = 2L,
                            ).apply {
                                id = 101L
                                createdAt = LocalDateTime.of(2026, 2, 25, 11, 1, 0)
                            }

                    coEvery {
                        actionDispatchHistoryRepository.findAllByCampaignIdOrderByCreatedAtDesc(99L)
                    } returns flowOf(historyA, historyB)

                    val result = service.browse(campaignId = 99L, journeyExecutionId = 1L)

                    result shouldHaveSize 1
                    result.first().id shouldBe 100L
                    result.first().variables shouldBe mapOf("name" to "crm")
                    result.first().createdAt shouldBe "2026-02-25T11:00:00"
                }
            }
        }
    })
