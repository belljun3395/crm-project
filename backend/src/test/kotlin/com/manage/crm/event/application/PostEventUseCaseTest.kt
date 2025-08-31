package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.CampaignEvents
import com.manage.crm.event.domain.CampaignFixtures
import com.manage.crm.event.domain.Event
import com.manage.crm.event.domain.EventFixtures
import com.manage.crm.event.domain.PropertiesFixtures
import com.manage.crm.event.domain.PropertyFixtures
import com.manage.crm.event.domain.cache.CampaignCacheManager
import com.manage.crm.event.domain.repository.CampaignEventsRepository
import com.manage.crm.event.domain.repository.CampaignRepository
import com.manage.crm.event.domain.repository.EventRepository
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributesFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.mockk

class PostEventUseCaseTest : BehaviorSpec({
    lateinit var eventRepository: EventRepository
    lateinit var campaignRepository: CampaignRepository
    lateinit var campaignEventsRepository: CampaignEventsRepository
    lateinit var campaignCacheManager: CampaignCacheManager
    lateinit var userRepository: UserRepository
    lateinit var postEventUseCase: PostEventUseCase

    beforeContainer {
        eventRepository = mockk()
        campaignRepository = mockk()
        campaignEventsRepository = mockk()
        campaignCacheManager = mockk()
        userRepository = mockk()
        postEventUseCase =
            PostEventUseCase(eventRepository, campaignRepository, campaignEventsRepository, campaignCacheManager, userRepository)
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

            val user = UserFixtures.giveMeOne()
                .withExternalId(useCaseIn.externalId)
                .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                .build()
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = EventFixtures.giveMeOne()
                .withName(useCaseIn.name)
                .withUserId(user.id!!)
                .withProperties(
                    PropertiesFixtures.giveMeOne()
                        .withValue(
                            useCaseIn.properties.map {
                                PropertyFixtures.giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
            coEvery { eventRepository.save(any(Event::class)) } answers { event }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe event.id
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

            val user = UserFixtures.giveMeOne()
                .withExternalId(useCaseIn.externalId)
                .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                .build()
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val eventProperties = PropertiesFixtures.giveMeOne()
                .withValue(
                    useCaseIn.properties.map {
                        PropertyFixtures.giveMeOne()
                            .withKey(it.key)
                            .withValue(it.value)
                            .build()
                    }
                )
                .build()

            val event = EventFixtures.giveMeOne()
                .withName(useCaseIn.name)
                .withUserId(user.id!!)
                .withProperties(eventProperties)
                .build()
            coEvery { eventRepository.save(any(Event::class)) } answers { event }

            val campaign = CampaignFixtures.giveMeOne()
                .withName(useCaseIn.campaignName!!)
                .withProperties(eventProperties)
                .build()

            coEvery {
                campaignCacheManager.loadAndSaveIfMiss(
                    eq(Campaign.UNIQUE_FIELDS.NAME),
                    eq(useCaseIn.campaignName!!),
                    captureLambda<suspend () -> Campaign?>()
                )
            } coAnswers {
                campaign
            }

            val campaignEvents = CampaignEvents.new(
                campaignId = campaign.id!!,
                eventId = event.id!!
            )
            coEvery { campaignEventsRepository.save(any(CampaignEvents::class)) } answers { campaignEvents }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe event.id!!
                result.message shouldBe SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("find campaign by name from cache. this is default") {
                coVerify(exactly = 1) {
                    campaignCacheManager.loadAndSaveIfMiss(
                        Campaign.UNIQUE_FIELDS.NAME,
                        useCaseIn.campaignName!!,
                        captureLambda<suspend () -> Campaign?>()
                    )
                }
                coVerify(exactly = 0) { campaignRepository.findCampaignByName(campaign.name) }
            }

            then("set campaign and event") {
                coVerify(exactly = 1) { campaignEventsRepository.save(any(CampaignEvents::class)) }
            }

            `when`("post event with campaign. when campaign is not cached") {
                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UNIQUE_FIELDS.NAME),
                        eq(useCaseIn.campaignName!!),
                        captureLambda<suspend () -> Campaign?>()
                    )
                } coAnswers {
                    lambda<suspend () -> Campaign?>().coInvoke()
                }
                coEvery { campaignRepository.findCampaignByName(campaign.name) } answers { campaign }

                campaignCacheManager.loadAndSaveIfMiss(Campaign.UNIQUE_FIELDS.NAME, useCaseIn.campaignName!!) {
                    campaignRepository.findCampaignByName(useCaseIn.campaignName!!)
                        ?: throw NotFoundByException("Campaign", "name", useCaseIn.campaignName!!)
                }

                then("try to load campaign from cache and save if miss") {
                    coVerify(exactly = 1) {
                        campaignCacheManager.loadAndSaveIfMiss(
                            Campaign.UNIQUE_FIELDS.NAME,
                            useCaseIn.campaignName!!,
                            captureLambda<suspend () -> Campaign?>()
                        )
                    }
                    coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaign.name) }
                }
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

            val user = UserFixtures.giveMeOne()
                .withExternalId(useCaseIn.externalId)
                .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                .build()
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = EventFixtures.giveMeOne()
                .withName(useCaseIn.name)
                .withUserId(user.id!!)
                .withProperties(
                    PropertiesFixtures.giveMeOne()
                        .withValue(
                            useCaseIn.properties.map {
                                PropertyFixtures.giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
            coEvery { eventRepository.save(any(Event::class)) } answers { event }

            val campaignName = useCaseIn.campaignName!!
            coEvery {
                campaignCacheManager.loadAndSaveIfMiss(
                    eq(Campaign.UNIQUE_FIELDS.NAME),
                    eq(useCaseIn.campaignName!!),
                    captureLambda<suspend () -> Campaign?>()
                )
            } coAnswers {
                lambda<suspend () -> Campaign?>().coInvoke()
            }

            coEvery { campaignRepository.findCampaignByName(campaignName) } answers {
                throw NotFoundByException("Campaign", "name", useCaseIn.campaignName!!)
            }

            val result = postEventUseCase.execute(useCaseIn)
            then("return PostEventUseCaseOut") {
                result.id shouldBe event.id
                result.message shouldBe SaveEventMessage.EVENT_SAVE_BUT_NOT_CAMPAIGN.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("try to load campaign from cache and try to find campaign by name") {
                coVerify(exactly = 1) {
                    campaignCacheManager.loadAndSaveIfMiss(
                        Campaign.UNIQUE_FIELDS.NAME,
                        useCaseIn.campaignName!!,
                        captureLambda<suspend () -> Campaign?>()
                    )
                }
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

            val user = UserFixtures.giveMeOne()
                .withExternalId(useCaseIn.externalId)
                .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                .build()
            coEvery { userRepository.findByExternalId(useCaseIn.externalId) } answers { user }

            val event = EventFixtures.giveMeOne()
                .withName(useCaseIn.name)
                .withUserId(user.id!!)
                .withProperties(
                    PropertiesFixtures.giveMeOne()
                        .withValue(
                            useCaseIn.properties.map {
                                PropertyFixtures.giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
            coEvery { eventRepository.save(any(Event::class)) } answers {
                event
            }

            val notMatchProperties = listOf(
                PostEventPropertyDto(
                    key = "key1",
                    value = "value1"
                )
            )
            val campaign = CampaignFixtures.giveMeOne()
                .withName(useCaseIn.campaignName!!)
                .withProperties(
                    PropertiesFixtures.giveMeOne()
                        .withValue(
                            notMatchProperties.map {
                                PropertyFixtures.giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .build()
                            }
                        )
                        .build()
                )
                .build()
            coEvery {
                campaignCacheManager.loadAndSaveIfMiss(
                    eq(Campaign.UNIQUE_FIELDS.NAME),
                    eq(useCaseIn.campaignName!!),
                    captureLambda<suspend () -> Campaign?>()
                )
            } coAnswers {
                campaign
            }

            val result = postEventUseCase.execute(useCaseIn)
            then("should return PostEventUseCaseOut") {
                result.id shouldBe event.id
                result.message shouldBe SaveEventMessage.PROPERTIES_MISMATCH.message
            }

            then("find user by externalId") {
                coVerify(exactly = 1) { userRepository.findByExternalId(useCaseIn.externalId) }
            }

            then("save event") {
                coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
            }

            then("find campaign by name from cache. this is default") {
                coVerify(exactly = 1) {
                    campaignCacheManager.loadAndSaveIfMiss(
                        Campaign.UNIQUE_FIELDS.NAME,
                        useCaseIn.campaignName!!,
                        captureLambda<suspend () -> Campaign?>()
                    )
                }
                coVerify(exactly = 0) { campaignRepository.findCampaignByName(campaign.name) }
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
