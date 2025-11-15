package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseIn
import com.manage.crm.email.domain.EmailSendHistoryFixtures
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import java.time.LocalDateTime

/**
 * Additional test cases for pagination behavior
 * 페이지네이션 동작을 검증하는 추가 테스트 케이스
 */
class BrowseEmailSendHistoriesUseCasePaginationTest : BehaviorSpec({
    lateinit var emailSendHistoryRepository: EmailSendHistoryRepository
    lateinit var useCase: BrowseEmailSendHistoriesUseCase

    beforeContainer {
        emailSendHistoryRepository = mockk()
        useCase = BrowseEmailSendHistoriesUseCase(emailSendHistoryRepository)
    }

    fun emailSendHistoryStubs(size: Int) = (1..size).map { idx ->
        EmailSendHistoryFixtures.giveMeOne()
            .withId(idx.toLong())
            .withUserId(idx.toLong())
            .withSendStatus(SentEmailStatus.SEND.name)
            .withCreatedAt(LocalDateTime.now().minusDays(idx.toLong()))
            .withUpdatedAt(LocalDateTime.now().minusDays(idx.toLong()))
            .build()
    }

    given("BrowseEmailSendHistoriesUseCase with pagination") {
        `when`("requesting second page") {
            val totalSize = 25
            val pageSize = 10
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 1, size = pageSize))

            then("should return second page items (11-20)") {
                result.histories.size shouldBe pageSize
                result.totalCount shouldBe totalSize
                result.page shouldBe 1
                result.size shouldBe pageSize
                // 두 번째 페이지는 11번부터 시작
                result.histories.first().id shouldBe 11L
                result.histories.last().id shouldBe 20L
            }
        }

        `when`("requesting last partial page") {
            val totalSize = 25
            val pageSize = 10
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 2, size = pageSize))

            then("should return remaining 5 items") {
                result.histories.size shouldBe 5
                result.totalCount shouldBe totalSize
                result.page shouldBe 2
                result.size shouldBe pageSize
                result.histories.first().id shouldBe 21L
                result.histories.last().id shouldBe 25L
            }
        }

        `when`("requesting with custom page size") {
            val totalSize = 30
            val pageSize = 7
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 3, size = pageSize))

            then("should return correct page with custom size") {
                result.histories.size shouldBe pageSize
                result.totalCount shouldBe totalSize
                result.page shouldBe 3
                result.size shouldBe pageSize
                // page=3, size=7이면 22번부터 시작 (3 * 7 + 1 = 22)
                result.histories.first().id shouldBe 22L
                result.histories.last().id shouldBe 28L
            }
        }

        `when`("page size exceeds maximum limit (100)") {
            val totalSize = 150
            val requestedSize = 200
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 0, size = requestedSize))

            then("should limit size to 100") {
                result.size shouldBe 100
                result.histories.size shouldBe 100
            }
        }

        `when`("page size is less than minimum (1)") {
            val totalSize = 10
            val requestedSize = 0
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 0, size = requestedSize))

            then("should set size to minimum (1)") {
                result.size shouldBe 1
                result.histories.size shouldBe 1
            }
        }

        `when`("page is negative") {
            val totalSize = 10
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = -5, size = 10))

            then("should set page to 0") {
                result.page shouldBe 0
            }
        }

        `when`("requesting page beyond available data") {
            val totalSize = 10
            val historyStubs = emailSendHistoryStubs(totalSize)

            coEvery { emailSendHistoryRepository.count() } returns totalSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null, page = 10, size = 10))

            then("should return empty list") {
                result.histories.size shouldBe 0
                result.totalCount shouldBe totalSize
                result.page shouldBe 10
            }
        }
    }
})
