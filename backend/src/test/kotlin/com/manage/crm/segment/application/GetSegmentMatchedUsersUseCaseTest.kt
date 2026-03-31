package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseIn
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class GetSegmentMatchedUsersUseCaseTest : BehaviorSpec({
    lateinit var segmentTargetingService: SegmentTargetingService
    lateinit var userReadPort: UserReadPort
    lateinit var useCase: GetSegmentMatchedUsersUseCase

    beforeTest {
        segmentTargetingService = mockk()
        userReadPort = mockk()
        useCase = GetSegmentMatchedUsersUseCase(
            segmentTargetingService = segmentTargetingService,
            userReadPort = userReadPort,
            objectMapper = jacksonObjectMapper()
        )
    }

    given("UC-SEGMENT-005 get matched segment users") {
        `when`("no matched user ids are resolved") {
            then("return empty list without user pagination query") {
                coEvery { segmentTargetingService.resolveUserIds(10L, null) } returns emptyList()

                val result = useCase.execute(
                    GetSegmentMatchedUsersUseCaseIn(segmentId = 10L, campaignId = null)
                )

                result.users shouldBe emptyList()
                coVerify(exactly = 0) { userReadPort.findAllByIdIn(any()) }
            }
        }

        `when`("matched user ids are resolved") {
            then("return sorted matched users with profile fields") {
                val firstUser = UserReadModel(
                    id = 2L,
                    externalId = "user-2",
                    userAttributesJson = """{"email":"two@example.com","name":"Two"}""",
                    createdAt = LocalDateTime.of(2025, 1, 1, 10, 0)
                )
                val secondUser = UserReadModel(
                    id = 1L,
                    externalId = "user-1",
                    userAttributesJson = """{"email":"one@example.com","name":"One"}""",
                    createdAt = LocalDateTime.of(2025, 1, 2, 10, 0)
                )
                coEvery { segmentTargetingService.resolveUserIds(10L, 100L) } returns listOf(2L, 1L)
                coEvery { userReadPort.findAllByIdIn(listOf(2L, 1L)) } returns listOf(firstUser, secondUser)

                val result = useCase.execute(
                    GetSegmentMatchedUsersUseCaseIn(segmentId = 10L, campaignId = 100L)
                )

                result.users.map { it.id } shouldBe listOf(1L, 2L)
                result.users[0].email shouldBe "one@example.com"
                result.users[1].name shouldBe "Two"
                coVerify(exactly = 1) { userReadPort.findAllByIdIn(listOf(2L, 1L)) }
            }
        }

        `when`("matched user has malformed userAttributes JSON") {
            then("return user with null profile fields without throwing") {
                val malformedUser = UserReadModel(
                    id = 5L,
                    externalId = "user-bad",
                    userAttributesJson = "not-valid-json",
                    createdAt = LocalDateTime.of(2025, 1, 1, 0, 0)
                )

                coEvery { segmentTargetingService.resolveUserIds(20L, null) } returns listOf(5L)
                coEvery { userReadPort.findAllByIdIn(listOf(5L)) } returns listOf(malformedUser)

                val result = useCase.execute(
                    GetSegmentMatchedUsersUseCaseIn(segmentId = 20L)
                )

                result.users.size shouldBe 1
                result.users[0].id shouldBe 5L
                result.users[0].email shouldBe null
                result.users[0].name shouldBe null
            }
        }
    }
})
