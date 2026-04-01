package com.manage.crm.event.application

import com.manage.crm.event.application.dto.PostEventPropertyDto
import com.manage.crm.event.application.dto.PostEventUseCaseIn
import com.manage.crm.event.application.port.query.EventReadPort
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
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.event.event.CampaignEventPublisher
import com.manage.crm.journey.application.port.out.JourneyTriggerPort
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.support.exception.NotFoundByException
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.UserFixtures
import com.manage.crm.user.domain.vo.UserAttributesFixtures
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class PostEventUseCaseTest :
    BehaviorSpec({
        lateinit var eventRepository: EventRepository
        lateinit var campaignRepository: CampaignRepository
        lateinit var campaignEventsRepository: CampaignEventsRepository
        lateinit var campaignCacheManager: CampaignCacheManager
        lateinit var userReadPort: UserReadPort
        lateinit var eventReadPort: EventReadPort
        lateinit var segmentReadPort: SegmentReadPort
        lateinit var journeyTriggerPort: JourneyTriggerPort
        lateinit var campaignEventPublisher: CampaignEventPublisher
        lateinit var postEventUseCase: PostEventUseCase

        beforeContainer {
            eventRepository = mockk()
            campaignRepository = mockk()
            campaignEventsRepository = mockk()
            campaignCacheManager = mockk()
            userReadPort = mockk()
            eventReadPort = mockk()
            segmentReadPort = mockk()
            journeyTriggerPort = mockk(relaxed = true)
            campaignEventPublisher = mockk(relaxed = true)
            postEventUseCase =
                PostEventUseCase(
                    eventRepository,
                    campaignRepository,
                    campaignEventsRepository,
                    campaignCacheManager,
                    userReadPort,
                    eventReadPort,
                    segmentReadPort,
                    journeyTriggerPort,
                    campaignEventPublisher,
                )
        }

        fun readUser(
            id: Long,
            externalId: String = "user-$id",
        ) = UserReadModel(
            id = id,
            externalId = externalId,
            userAttributesJson = "{}",
            createdAt = LocalDateTime.now(),
        )

        given("UC-EVENT-001 PostEventUseCase") {
            `when`("post event") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties =
                            listOf(
                                PostEventPropertyDto(
                                    key = "key1",
                                    value = "value1",
                                ),
                                PostEventPropertyDto(
                                    key = "key2",
                                    value = "value2",
                                ),
                            ),
                        campaignName = null,
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId(useCaseIn.externalId)
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                        .build()
                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers { user.toReadModel() }

                val event =
                    EventFixtures
                        .giveMeOne()
                        .withName(useCaseIn.name)
                        .withUserId(user.id!!)
                        .withProperties(
                            PropertiesFixtures
                                .giveMeOne()
                                .withValue(
                                    useCaseIn.properties.map {
                                        PropertyFixtures
                                            .giveMeOne()
                                            .withKey(it.key)
                                            .withValue(it.value)
                                            .buildEvent()
                                    },
                                ).buildEvent(),
                        ).build()
                coEvery { eventRepository.save(any(Event::class)) } answers { event }

                val result = postEventUseCase.execute(useCaseIn)
                then("should return PostEventUseCaseOut") {
                    result.id shouldBe event.id
                    result.message shouldBe SaveEventMessage.EVENT_SAVE_SUCCESS.message
                }

                then("find user by externalId") {
                    coVerify(exactly = 1) { userReadPort.findByExternalId(useCaseIn.externalId) }
                }

                then("save event") {
                    coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
                }

                then("should not publish to dashboard stream when no campaign") {
                    coVerify(exactly = 0) { campaignEventPublisher.publishCampaignEvent(any()) }
                }
            }

            `when`("post event with campaign") {
                val properties =
                    listOf(
                        PostEventPropertyDto(
                            key = "key1",
                            value = "value1",
                        ),
                        PostEventPropertyDto(
                            key = "key2",
                            value = "value2",
                        ),
                    )
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties = properties,
                        campaignName = "campaign",
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId(useCaseIn.externalId)
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                        .build()
                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers { user.toReadModel() }

                val eventProperties =
                    PropertiesFixtures
                        .giveMeOne()
                        .withValue(
                            useCaseIn.properties.map {
                                PropertyFixtures
                                    .giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .buildEvent()
                            },
                        ).buildEvent()

                val event =
                    EventFixtures
                        .giveMeOne()
                        .withName(useCaseIn.name)
                        .withUserId(user.id!!)
                        .withProperties(eventProperties)
                        .build()
                coEvery { eventRepository.save(any(Event::class)) } answers { event }

                val campaign =
                    CampaignFixtures
                        .giveMeOne()
                        .withName(useCaseIn.campaignName!!)
                        .withProperties(
                            CampaignProperties(
                                useCaseIn.properties.map {
                                    CampaignProperty(it.key, it.value)
                                },
                            ),
                        ).build()

                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq(useCaseIn.campaignName!!),
                        captureLambda<suspend () -> Campaign?>(),
                    )
                } coAnswers {
                    campaign
                }

                val campaignEvents =
                    CampaignEvents.new(
                        campaignId = campaign.id!!,
                        eventId = event.id!!,
                    )
                coEvery { campaignEventsRepository.save(any(CampaignEvents::class)) } answers { campaignEvents }

                val result = postEventUseCase.execute(useCaseIn)
                then("should return PostEventUseCaseOut") {
                    result.id shouldBe event.id!!
                    result.message shouldBe SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN.message
                }

                then("find user by externalId") {
                    coVerify(exactly = 1) { userReadPort.findByExternalId(useCaseIn.externalId) }
                }

                then("save event") {
                    coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
                }

                then("find campaign by name from cache. this is default") {
                    coVerify(exactly = 1) {
                        campaignCacheManager.loadAndSaveIfMiss(
                            Campaign.UniqueFields.NAME,
                            useCaseIn.campaignName!!,
                            captureLambda<suspend () -> Campaign?>(),
                        )
                    }
                    coVerify(exactly = 0) { campaignRepository.findCampaignByName(campaign.name) }
                }

                then("set campaign and event") {
                    coVerify(exactly = 1) { campaignEventsRepository.save(any(CampaignEvents::class)) }
                }

                then("publish campaign event to dashboard stream") {
                    coVerify(exactly = 1) {
                        campaignEventPublisher.publishCampaignEvent(
                            match {
                                it.campaignId == campaign.id &&
                                    it.eventId == event.id &&
                                    it.userId == event.userId &&
                                    it.eventName == event.name
                            },
                        )
                    }
                }

                `when`("post event with campaign. when campaign is not cached") {
                    coEvery {
                        campaignCacheManager.loadAndSaveIfMiss(
                            eq(Campaign.UniqueFields.NAME),
                            eq(useCaseIn.campaignName!!),
                            captureLambda<suspend () -> Campaign?>(),
                        )
                    } coAnswers {
                        lambda<suspend () -> Campaign?>().coInvoke()
                    }
                    coEvery { campaignRepository.findCampaignByName(campaign.name) } answers { campaign }

                    campaignCacheManager.loadAndSaveIfMiss(Campaign.UniqueFields.NAME, useCaseIn.campaignName!!) {
                        campaignRepository.findCampaignByName(useCaseIn.campaignName!!)
                            ?: throw NotFoundByException("Campaign", "name", useCaseIn.campaignName!!)
                    }

                    then("try to load campaign from cache and save if miss") {
                        coVerify(exactly = 1) {
                            campaignCacheManager.loadAndSaveIfMiss(
                                Campaign.UniqueFields.NAME,
                                useCaseIn.campaignName!!,
                                captureLambda<suspend () -> Campaign?>(),
                            )
                        }
                        coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaign.name) }
                    }
                }
            }

            `when`("post event with campaign when dashboard service fails") {
                val properties =
                    listOf(
                        PostEventPropertyDto(
                            key = "key1",
                            value = "value1",
                        ),
                        PostEventPropertyDto(
                            key = "key2",
                            value = "value2",
                        ),
                    )
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties = properties,
                        campaignName = "campaign",
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId(useCaseIn.externalId)
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                        .build()
                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers { user.toReadModel() }

                val eventProperties =
                    PropertiesFixtures
                        .giveMeOne()
                        .withValue(
                            useCaseIn.properties.map {
                                PropertyFixtures
                                    .giveMeOne()
                                    .withKey(it.key)
                                    .withValue(it.value)
                                    .buildEvent()
                            },
                        ).buildEvent()

                val event =
                    EventFixtures
                        .giveMeOne()
                        .withName(useCaseIn.name)
                        .withUserId(user.id!!)
                        .withProperties(eventProperties)
                        .build()
                coEvery { eventRepository.save(any(Event::class)) } answers { event }

                val campaign =
                    CampaignFixtures
                        .giveMeOne()
                        .withName(useCaseIn.campaignName!!)
                        .withProperties(
                            CampaignProperties(
                                useCaseIn.properties.map {
                                    CampaignProperty(it.key, it.value)
                                },
                            ),
                        ).build()

                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq(useCaseIn.campaignName!!),
                        captureLambda<suspend () -> Campaign?>(),
                    )
                } coAnswers {
                    campaign
                }

                val campaignEvents =
                    CampaignEvents.new(
                        campaignId = campaign.id!!,
                        eventId = event.id!!,
                    )
                coEvery { campaignEventsRepository.save(any(CampaignEvents::class)) } answers { campaignEvents }

                // Dashboard service throws exception
                coEvery { campaignEventPublisher.publishCampaignEvent(any()) } throws RuntimeException("Redis connection failed")

                val result = postEventUseCase.execute(useCaseIn)

                then("should return success despite dashboard service failure") {
                    result.id shouldBe event.id!!
                    result.message shouldBe SaveEventMessage.EVENT_SAVE_WITH_CAMPAIGN.message
                }

                then("should still save campaign event") {
                    coVerify(exactly = 1) { campaignEventsRepository.save(any(CampaignEvents::class)) }
                }

                then("should attempt to publish to dashboard stream") {
                    coVerify(exactly = 1) { campaignEventPublisher.publishCampaignEvent(any()) }
                }
            }

            `when`("post event with not found campaign") {
                val properties =
                    listOf(
                        PostEventPropertyDto(
                            key = "key1",
                            value = "value1",
                        ),
                        PostEventPropertyDto(
                            key = "key2",
                            value = "value2",
                        ),
                    )
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties = properties,
                        campaignName = "campaign",
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId(useCaseIn.externalId)
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                        .build()
                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers { user.toReadModel() }

                val event =
                    EventFixtures
                        .giveMeOne()
                        .withName(useCaseIn.name)
                        .withUserId(user.id!!)
                        .withProperties(
                            PropertiesFixtures
                                .giveMeOne()
                                .withValue(
                                    useCaseIn.properties.map {
                                        PropertyFixtures
                                            .giveMeOne()
                                            .withKey(it.key)
                                            .withValue(it.value)
                                            .buildEvent()
                                    },
                                ).buildEvent(),
                        ).build()
                coEvery { eventRepository.save(any(Event::class)) } answers { event }

                val campaignName = useCaseIn.campaignName!!
                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq(useCaseIn.campaignName!!),
                        captureLambda<suspend () -> Campaign?>(),
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
                    coVerify(exactly = 1) { userReadPort.findByExternalId(useCaseIn.externalId) }
                }

                then("save event") {
                    coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
                }

                then("try to load campaign from cache and try to find campaign by name") {
                    coVerify(exactly = 1) {
                        campaignCacheManager.loadAndSaveIfMiss(
                            Campaign.UniqueFields.NAME,
                            useCaseIn.campaignName!!,
                            captureLambda<suspend () -> Campaign?>(),
                        )
                    }
                    coVerify(exactly = 1) { campaignRepository.findCampaignByName(campaignName) }
                }

                then("can't set campaign and event case campaign not found") {
                    coVerify(exactly = 0) { campaignEventsRepository.save(any(CampaignEvents::class)) }
                }

                then("should not publish to dashboard stream when campaign not found") {
                    coVerify(exactly = 0) { campaignEventPublisher.publishCampaignEvent(any()) }
                }
            }

            `when`("post event with campaign but not all match property keys") {
                val properties =
                    listOf(
                        PostEventPropertyDto(
                            key = "key1",
                            value = "value1",
                        ),
                        PostEventPropertyDto(
                            key = "key2",
                            value = "value2",
                        ),
                    )
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties = properties,
                        campaignName = "campaign",
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId(useCaseIn.externalId)
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("""{}""").build())
                        .build()
                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers { user.toReadModel() }

                val event =
                    EventFixtures
                        .giveMeOne()
                        .withName(useCaseIn.name)
                        .withUserId(user.id!!)
                        .withProperties(
                            PropertiesFixtures
                                .giveMeOne()
                                .withValue(
                                    useCaseIn.properties.map {
                                        PropertyFixtures
                                            .giveMeOne()
                                            .withKey(it.key)
                                            .withValue(it.value)
                                            .buildEvent()
                                    },
                                ).buildEvent(),
                        ).build()
                coEvery { eventRepository.save(any(Event::class)) } answers {
                    event
                }

                val notMatchProperties =
                    listOf(
                        PostEventPropertyDto(
                            key = "key1",
                            value = "value1",
                        ),
                    )
                val campaign =
                    CampaignFixtures
                        .giveMeOne()
                        .withName(useCaseIn.campaignName!!)
                        .withProperties(
                            PropertiesFixtures
                                .giveMeOne()
                                .withValue(
                                    notMatchProperties.map {
                                        PropertyFixtures
                                            .giveMeOne()
                                            .withKey(it.key)
                                            .withValue(it.value)
                                            .buildEvent()
                                    },
                                ).buildCampaign(),
                        ).build()
                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq(useCaseIn.campaignName!!),
                        captureLambda<suspend () -> Campaign?>(),
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
                    coVerify(exactly = 1) { userReadPort.findByExternalId(useCaseIn.externalId) }
                }

                then("save event") {
                    coVerify(exactly = 1) { eventRepository.save(any(Event::class)) }
                }

                then("find campaign by name from cache. this is default") {
                    coVerify(exactly = 1) {
                        campaignCacheManager.loadAndSaveIfMiss(
                            Campaign.UniqueFields.NAME,
                            useCaseIn.campaignName!!,
                            captureLambda<suspend () -> Campaign?>(),
                        )
                    }
                    coVerify(exactly = 0) { campaignRepository.findCampaignByName(campaign.name) }
                }

                then("can't set campaign and event cause property keys not match") {
                    coVerify(exactly = 0) { campaignEventsRepository.save(any(CampaignEvents::class)) }
                }

                then("should not publish to dashboard stream when property keys mismatch") {
                    coVerify(exactly = 0) { campaignEventPublisher.publishCampaignEvent(any()) }
                }
            }

            `when`("segmentId is provided") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "ignored-user",
                        segmentId = 55L,
                        properties =
                            listOf(
                                PostEventPropertyDto(
                                    key = "key1",
                                    value = "value1",
                                ),
                            ),
                        campaignName = null,
                    )

                coEvery { userReadPort.findAll() } returns listOf(readUser(1L), readUser(2L))
                coEvery { eventReadPort.findAllByUserIdIn(any()) } returns emptyList()
                coEvery { segmentReadPort.findTargetUserIds(55L, any(), any()) } returns listOf(1L, 2L)
                coEvery { eventRepository.save(any(Event::class)) } answers {
                    firstArg<Event>().apply {
                        id = if (userId == 1L) 101L else 102L
                    }
                }

                then("save events for all users in segment and ignore externalId lookup") {
                    val result = postEventUseCase.execute(useCaseIn)
                    result.id shouldBe 101L
                    result.message shouldBe "Event saved for segment users (2)"
                    coVerify(exactly = 1) { segmentReadPort.findTargetUserIds(55L, any(), any()) }
                    coVerify(exactly = 0) { userReadPort.findByExternalId(any()) }
                    coVerify(exactly = 2) { eventRepository.save(any(Event::class)) }
                }
            }

            `when`("segmentId and campaignName are both provided") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "ignored",
                        segmentId = 77L,
                        properties = listOf(PostEventPropertyDto(key = "key1", value = "value1")),
                        campaignName = "seg-campaign",
                    )

                coEvery { userReadPort.findAll() } returns listOf(readUser(1L), readUser(2L))
                coEvery { eventReadPort.findAllByUserIdIn(any()) } returns emptyList()
                coEvery { segmentReadPort.findTargetUserIds(77L, any(), any()) } returns listOf(1L, 2L)
                coEvery { eventRepository.save(any(Event::class)) } answers {
                    firstArg<Event>().apply { id = if (userId == 1L) 201L else 202L }
                }

                val campaign =
                    CampaignFixtures
                        .giveMeOne()
                        .withName("seg-campaign")
                        .withProperties(CampaignProperties(listOf(CampaignProperty("key1", "value1"))))
                        .build()

                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq("seg-campaign"),
                        captureLambda<suspend () -> Campaign?>(),
                    )
                } coAnswers { campaign }

                coEvery { campaignEventsRepository.save(any(CampaignEvents::class)) } answers { firstArg() }

                val result = postEventUseCase.execute(useCaseIn)

                then("returns segment+campaign combined message") {
                    result.message shouldBe "Event saved with campaign for segment users (2)"
                }

                then("saves campaign-event links for each segment user") {
                    coVerify(exactly = 2) { campaignEventsRepository.save(any(CampaignEvents::class)) }
                }
            }

            `when`("segmentId provided but campaign not found") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "ignored",
                        segmentId = 88L,
                        properties = listOf(PostEventPropertyDto(key = "key1", value = "value1")),
                        campaignName = "missing-campaign",
                    )

                coEvery { userReadPort.findAll() } returns listOf(readUser(10L))
                coEvery { eventReadPort.findAllByUserIdIn(any()) } returns emptyList()
                coEvery { segmentReadPort.findTargetUserIds(88L, any(), any()) } returns listOf(10L)
                coEvery { eventRepository.save(any(Event::class)) } answers {
                    firstArg<Event>().apply { id = 301L }
                }

                coEvery {
                    campaignCacheManager.loadAndSaveIfMiss(
                        eq(Campaign.UniqueFields.NAME),
                        eq("missing-campaign"),
                        captureLambda<suspend () -> Campaign?>(),
                    )
                } coAnswers { lambda<suspend () -> Campaign?>().coInvoke() }

                coEvery { campaignRepository.findCampaignByName("missing-campaign") } throws
                    NotFoundByException("Campaign", "name", "missing-campaign")

                val result = postEventUseCase.execute(useCaseIn)

                then("returns segment-specific not-in-campaign message") {
                    result.message shouldBe "Event saved for segment users (1) but not in campaign"
                }
            }

            `when`("journey trigger publish fails") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "user-1",
                        properties = listOf(PostEventPropertyDto(key = "k", value = "v")),
                        campaignName = null,
                    )

                val user =
                    UserFixtures
                        .giveMeOne()
                        .withExternalId("user-1")
                        .withUserAttributes(UserAttributesFixtures.giveMeOne().withValue("{}").build())
                        .build()
                coEvery { userReadPort.findByExternalId("user-1") } returns user.toReadModel()
                coEvery { eventRepository.save(any(Event::class)) } answers {
                    firstArg<Event>().apply { id = 401L }
                }
                coEvery { journeyTriggerPort.triggerByEvent(any()) } throws
                    RuntimeException("queue unavailable")

                val result = postEventUseCase.execute(useCaseIn)

                then("event save succeeds despite journey trigger failure") {
                    result.id shouldBe 401L
                    result.message shouldBe SaveEventMessage.EVENT_SAVE_SUCCESS.message
                }
            }

            `when`("post event with not found user") {
                val useCaseIn =
                    PostEventUseCaseIn(
                        name = "event",
                        externalId = "1",
                        properties =
                            listOf(
                                PostEventPropertyDto(
                                    key = "key1",
                                    value = "value1",
                                ),
                                PostEventPropertyDto(
                                    key = "key2",
                                    value = "value2",
                                ),
                            ),
                        campaignName = null,
                    )

                coEvery { userReadPort.findByExternalId(useCaseIn.externalId) } answers {
                    throw NotFoundByException("User", "externalId", useCaseIn.externalId)
                }

                then("should throw exception") {
                    val exception =
                        shouldThrow<NotFoundByException> {
                            postEventUseCase.execute(useCaseIn)
                        }
                    exception.message shouldBe "User not found by externalId: ${useCaseIn.externalId}"
                }

                then("find user by externalId") {
                    coVerify(exactly = 1) { userReadPort.findByExternalId(useCaseIn.externalId) }
                }

                then("not called save event") {
                    coVerify(exactly = 0) { eventRepository.save(any(Event::class)) }
                }
            }
        }
    })

private fun User.toReadModel(): UserReadModel =
    UserReadModel(
        id = requireNotNull(id),
        externalId = externalId,
        userAttributesJson = userAttributes.value,
        createdAt = createdAt,
    )
