package com.manage.crm.event.application

import com.manage.crm.event.application.dto.StreamCampaignDashboardUseCaseIn
import com.manage.crm.event.event.CampaignDashboardEventFixtures
import com.manage.crm.event.stream.CampaignDashboardStreamManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime

class StreamCampaignDashboardUseCaseTest :
    BehaviorSpec({
        lateinit var campaignDashboardStreamManager: CampaignDashboardStreamManager
        lateinit var streamCampaignDashboardUseCase: StreamCampaignDashboardUseCase

        beforeContainer {
            campaignDashboardStreamManager = mockk()
            streamCampaignDashboardUseCase = StreamCampaignDashboardUseCase(campaignDashboardStreamManager)
        }

        given("UC-CAMPAIGN-006 StreamCampaignDashboardUseCase") {
            `when`("streaming with explicit duration and cursor") {
                val campaignId = 1L
                val durationSeconds = 120L
                val lastEventId = "10-0"
                val event =
                    CampaignDashboardEventFixtures
                        .aCampaignDashboardEvent()
                        .withCampaignId(campaignId)
                        .withEventId(100L)
                        .withUserId(200L)
                        .withEventName("event-a")
                        .withTimestamp(LocalDateTime.of(2026, 3, 27, 13, 0, 0))
                        .withStreamId("10-1")
                        .build()
                every {
                    campaignDashboardStreamManager.streamEvents(
                        campaignId = campaignId,
                        duration = Duration.ofSeconds(durationSeconds),
                        lastEventId = lastEventId,
                    )
                } returns Flux.just(event)

                val result =
                    streamCampaignDashboardUseCase.execute(
                        StreamCampaignDashboardUseCaseIn(
                            campaignId = campaignId,
                            durationSeconds = durationSeconds,
                            lastEventId = lastEventId,
                        ),
                    )

                then("should delegate to stream manager with converted duration") {
                    val events = result.collectList().block() ?: emptyList()
                    events shouldHaveSize 1
                    events.first().eventName shouldBe "event-a"
                    verify(exactly = 1) {
                        campaignDashboardStreamManager.streamEvents(
                            campaignId = campaignId,
                            duration = Duration.ofSeconds(durationSeconds),
                            lastEventId = lastEventId,
                        )
                    }
                }
            }
        }
    })
