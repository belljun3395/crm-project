package com.manage.crm.segment.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseIn
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.UserAttributes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime

class GetSegmentMatchedUsersUseCaseTest : BehaviorSpec({
    lateinit var segmentTargetingService: SegmentTargetingService
    lateinit var userRepository: UserRepository
    lateinit var useCase: GetSegmentMatchedUsersUseCase

    beforeTest {
        segmentTargetingService = mockk()
        userRepository = mockk()
        useCase = GetSegmentMatchedUsersUseCase(
            segmentTargetingService = segmentTargetingService,
            userRepository = userRepository,
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
                coVerify(exactly = 0) { userRepository.findAllByIdIn(any()) }
            }
        }

        `when`("matched user ids are resolved") {
            then("return sorted matched users with profile fields") {
                val firstUser = User.new(
                    id = 2L,
                    externalId = "user-2",
                    userAttributes = UserAttributes("""{"email":"two@example.com","name":"Two"}"""),
                    createdAt = LocalDateTime.of(2025, 1, 1, 10, 0),
                    updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0)
                )
                val secondUser = User.new(
                    id = 1L,
                    externalId = "user-1",
                    userAttributes = UserAttributes("""{"email":"one@example.com","name":"One"}"""),
                    createdAt = LocalDateTime.of(2025, 1, 2, 10, 0),
                    updatedAt = LocalDateTime.of(2025, 1, 2, 10, 0)
                )
                coEvery { segmentTargetingService.resolveUserIds(10L, 100L) } returns listOf(2L, 1L)
                coEvery { userRepository.findAllByIdIn(listOf(2L, 1L)) } returns listOf(firstUser, secondUser)

                val result = useCase.execute(
                    GetSegmentMatchedUsersUseCaseIn(segmentId = 10L, campaignId = 100L)
                )

                result.users.map { it.id } shouldBe listOf(1L, 2L)
                result.users[0].email shouldBe "one@example.com"
                result.users[1].name shouldBe "Two"
                coVerify(exactly = 1) { userRepository.findAllByIdIn(listOf(2L, 1L)) }
            }
        }

        `when`("matched user has malformed userAttributes JSON") {
            then("return user with null profile fields without throwing") {
                val malformedUser = User.new(
                    id = 5L,
                    externalId = "user-bad",
                    userAttributes = UserAttributes("not-valid-json"),
                    createdAt = LocalDateTime.of(2025, 1, 1, 0, 0),
                    updatedAt = LocalDateTime.of(2025, 1, 1, 0, 0)
                )

                coEvery { segmentTargetingService.resolveUserIds(20L, null) } returns listOf(5L)
                coEvery { userRepository.findAllByIdIn(listOf(5L)) } returns listOf(malformedUser)

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
