package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.event.domain.vo.Properties
import com.manage.crm.event.domain.vo.Property
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime.now

class PostEventUseCaseTest : BehaviorSpec({
    lateinit var eventRepository: EventRepository
    lateinit var campaignRepository: CampaignRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var userRepository: UserRepository
    lateinit var postEventUseCase: PostEventUseCase

    beforeContainer {
        eventRepository = mockk()
        campaignRepository = mockk()
        campaignEventsRepository = mockk()
        userRepository = mockk()
        postEventUseCase =
            PostEventUseCase(eventRepository, campaignRepository, campaignEventsRepository, userRepository)
    }

    given("PostEventUseCase") {
        `when`("post event") {
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = listOf(
                    PostEventPropertyDto(
                        key = "key1",
                        value = "value1"
                    ),
                    PostEventPropertyDto(
                        key = "key2",
                        value = "value2"
                    )
                ),
                campaignName = null
            )

            val user = User.new(
                id = 1L,
                externalId = useCaseIn.externalId,
                userAttributes = Json("""{}""".trimIndent()),
                createdAt = now(),
                updatedAt = now()
            )
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = Event.new(
                name = useCaseIn.name,
                userId = user.id!!,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event.apply {
                    id = 1
                    createdAt = now()
                }
            }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe 1
                result.message shouldBe SaveEventMessage.EVENT_SAVE_SUCCESS.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }
        }

        `when`("post event with campaign") {
            val properties = listOf(
                PostEventPropertyDto(
                    key = "key1",
                    value = "value1"
                ),
                PostEventPropertyDto(
                    key = "key2",
                    value = "value2"
                )
            )
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = properties,
                campaignName = "campaign"
            )

            val user = User.new(
                id = 1L,
                externalId = useCaseIn.externalId,
                userAttributes = Json("""{}""".trimIndent()),
                createdAt = now(),
                updatedAt = now()
            )
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = Event.new(
                name = useCaseIn.name,
                userId = user.id!!,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
            val eventId = 1L
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event.apply {
                    id = eventId
                    createdAt = now()
                }
            }

            val campaignName = useCaseIn.campaignName!!
            val campaignId = 1L
            val campaign = Campaign.new(
                id = campaignId,
                name = campaignName,
                properties = Properties(
                    properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                ),
                createdAt = now()
            )
            coEvery { campaignRepository.findCampaignByName(campaignName) } answers { campaign }

            val campaignEvents = CampaignEvents.new(
                campaignId = campaignId,
                eventId = eventId
            )
            coEvery { campaignEventsRepository.save(any(CampaignEvents::class)) } answers { campaignEvents }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe eventId
                result.message shouldBe SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("find campaign by name") {
                coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaignName) }
            }

            then("set campaign and event") {
                coVerify(exactly = 1) { campaignEventsRepository.save(any(CampaignEvents::class)) }
            }
        }

        `when`("post event with not found campaign") {
            val properties = listOf(
                PostEventPropertyDto(
                    key = "key1",
                    value = "value1"
                ),
                PostEventPropertyDto(
                    key = "key2",
                    value = "value2"
                )
            )
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = properties,
                campaignName = "campaign"
            )

            val user = User.new(
                id = 1L,
                externalId = useCaseIn.externalId,
                userAttributes = Json("""{}""".trimIndent()),
                createdAt = now(),
                updatedAt = now()
            )
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = Event.new(
                name = useCaseIn.name,
                userId = user.id!!,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
            val eventId = 1L
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event.apply {
                    id = eventId
                    createdAt = now()
                }
            }

            val campaignName = useCaseIn.campaignName!!
            coEvery { campaignRepository.findCampaignByName(campaignName) } answers { null }

            val result = postEventUseCase.execute(useCaseIn)
            then("return PostEventUseCaseOut") {
                result.id shouldBe eventId
                result.message shouldBe SaveEventMessage.EVENT_SAVE_BUT_NOT_CAMPAIGN.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("find campaign by name") {
                coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaignName) }
            }

            then("can't set campaign and event case campaign not found") {
                coVerify(exactly = 0) { campaignEventsRepository.save(any(CampaignEvents::class)) }
            }
        }

        `when`("post event with campaign but not all match property keys") {
            val properties = listOf(
                PostEventPropertyDto(
                    key = "key1",
                    value = "value1"
                ),
                PostEventPropertyDto(
                    key = "key2",
                    value = "value2"
                )
            )
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = properties,
                campaignName = "campaign"
            )

            val user = User.new(
                id = 1L,
                externalId = useCaseIn.externalId,
                userAttributes = Json("""{}""".trimIndent()),
                createdAt = now(),
                updatedAt = now()
            )
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = Event.new(
                name = useCaseIn.name,
                userId = user.id!!,
                properties = Properties(
                    useCaseIn.properties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                )
            )
            val eventId = 1L
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event.apply {
                    id = eventId
                    createdAt = now()
                }
            }

            val notMatchProperties = listOf(
                PostEventPropertyDto(
                    key = "key1",
                    value = "value1"
                )
            )
            val campaignName = useCaseIn.campaignName!!
            val campaignId = 1L
            val campaign = Campaign.new(
                id = campaignId,
                name = campaignName,
                properties = Properties(
                    notMatchProperties.map {
                        Property(
                            key = it.key,
                            value = it.value
                        )
                    }.toList()
                ),
                createdAt = now()
            )
            coEvery { campaignRepository.findCampaignByName(campaignName) } answers { campaign }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe eventId
                result.message shouldBe SaveEventMessage.PROPERTIES_MISMATCH.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("find campaign by name") {
                coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaignName) }
            }

            then("can't set campaign and event cause property keys not match") {
                coVerify(exactly = 0) { campaignEventsRepository.save(any(CampaignEvents::class)) }
            }
        }

        `when`("post event with not found user") {
            val useCaseIn = PostEventUseCaseIn(
                name = "event",
                externalId = "1",
                properties = listOf(
                    PostEventPropertyDto(
                        key = "key1",
                        value = "value1"
                    ),
                    PostEventPropertyDto(
                        key = "key2",
                        value = "value2"
                    )
                ),
                campaignName = null
            )

            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers {
                throw NotFoundByException("User", "externalId", useCaseIn.externalId)
            }

            then("should throw exception") {
                val exception = shouldThrow<NotFoundByException> {
                    postEventUseCase.execute(useCaseIn)
                }
                exception.message shouldBe "User not found by externalId: ${useCaseIn.externalId}"
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("not called save event") {
                coVerify(exactly = 0) { eventRepository.save(any(Event::class)) }
            }
        }
    }
})
