package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseIn
import com.manage.crm.email.domain.EmailSendHistoryFixtures
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.email.domain.vo.SentEmailStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.asFlow
import java.time.LocalDateTime

class BrowseEmailSendHistoriesUseCaseTest : BehaviorSpec({
    lateinit var emailSendHistoryRepository: EmailSendHistoryRepository
    lateinit var useCase: BrowseEmailSendHistoriesUseCase

    beforeContainer {
        emailSendHistoryRepository = mockk()
        useCase = BrowseEmailSendHistoriesUseCase(emailSendHistoryRepository)
    }

    fun emailSendHistoryStubs(size: Int, userId: Long? = null, sendStatus: String? = null) = (1..size).map { idx ->
        EmailSendHistoryFixtures.giveMeOne()
            .withId(idx.toLong())
            .withUserId(userId ?: idx.toLong())
            .withSendStatus(sendStatus ?: SentEmailStatus.SEND.name)
            .withCreatedAt(LocalDateTime.now().minusDays(idx.toLong()))
            .withUpdatedAt(LocalDateTime.now().minusDays(idx.toLong()))
            .build()
    }

    given("BrowseEmailSendHistoriesUseCase") {
        `when`("browse all email send histories") {
            val useCaseIn = BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = null)
            val historyStubSize = 5
            val historyStubs = emailSendHistoryStubs(historyStubSize)

            coEvery { emailSendHistoryRepository.count() } returns historyStubSize.toLong()
            coEvery { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() } answers { historyStubs.asFlow() }

            val result = useCase.execute(useCaseIn)

            then("should return all email send histories with pagination info") {
                result.histories.size shouldBe historyStubSize
                result.totalCount shouldBe historyStubSize
                result.page shouldBe 0
                result.size shouldBe 20
                result.histories.forEachIndexed { index, history ->
                    history.id shouldBe (index + 1).toLong()
                }
            }

            then("find all histories ordered by created at desc") {
                coVerify(exactly = 1) { emailSendHistoryRepository.count() }
                coVerify(exactly = 1) { emailSendHistoryRepository.findAllByOrderByCreatedAtDesc() }
            }
        }

        `when`("browse email send histories by userId") {
            val testUserId = 10L
            val useCaseIn = BrowseEmailSendHistoriesUseCaseIn(userId = testUserId, sendStatus = null)
            val historyStubSize = 3
            val historyStubs = emailSendHistoryStubs(historyStubSize, userId = testUserId)

            coEvery { emailSendHistoryRepository.countByUserId(testUserId) } returns historyStubSize.toLong()
            coEvery { emailSendHistoryRepository.findByUserId(testUserId) } answers { historyStubs.asFlow() }

            val result = useCase.execute(useCaseIn)

            then("should return email send histories for the user with pagination info") {
                result.histories.size shouldBe historyStubSize
                result.totalCount shouldBe historyStubSize
                result.histories.forEach { history ->
                    history.userId shouldBe testUserId
                }
            }

            then("find histories by user id") {
                coVerify(exactly = 1) { emailSendHistoryRepository.countByUserId(testUserId) }
                coVerify(exactly = 1) { emailSendHistoryRepository.findByUserId(testUserId) }
            }
        }

        `when`("browse email send histories by sendStatus") {
            val testStatus = SentEmailStatus.DELIVERY.name
            val useCaseIn = BrowseEmailSendHistoriesUseCaseIn(userId = null, sendStatus = testStatus)
            val historyStubSize = 4
            val historyStubs = emailSendHistoryStubs(historyStubSize, sendStatus = testStatus)

            coEvery { emailSendHistoryRepository.countBySendStatus(testStatus) } returns historyStubSize.toLong()
            coEvery { emailSendHistoryRepository.findBySendStatus(testStatus) } answers { historyStubs.asFlow() }

            val result = useCase.execute(useCaseIn)

            then("should return email send histories with the status and pagination info") {
                result.histories.size shouldBe historyStubSize
                result.totalCount shouldBe historyStubSize
                result.histories.forEach { history ->
                    history.sendStatus shouldBe testStatus
                }
            }

            then("find histories by send status") {
                coVerify(exactly = 1) { emailSendHistoryRepository.countBySendStatus(testStatus) }
                coVerify(exactly = 1) { emailSendHistoryRepository.findBySendStatus(testStatus) }
            }
        }

        `when`("browse email send histories by userId and sendStatus") {
            val testUserId = 10L
            val testStatus = SentEmailStatus.OPEN.name
            val useCaseIn = BrowseEmailSendHistoriesUseCaseIn(userId = testUserId, sendStatus = testStatus)
            val historyStubSize = 2
            val historyStubs = emailSendHistoryStubs(historyStubSize, userId = testUserId, sendStatus = testStatus)

            coEvery {
                emailSendHistoryRepository.countByUserIdAndSendStatus(testUserId, testStatus)
            } returns historyStubSize.toLong()
            coEvery {
                emailSendHistoryRepository.findByUserIdAndSendStatus(testUserId, testStatus)
            } answers { historyStubs.asFlow() }

            val result = useCase.execute(useCaseIn)

            then("should return email send histories for the user and status with pagination info") {
                result.histories.size shouldBe historyStubSize
                result.totalCount shouldBe historyStubSize
                result.histories.forEach { history ->
                    history.userId shouldBe testUserId
                    history.sendStatus shouldBe testStatus
                }
            }

            then("find histories by user id and send status") {
                coVerify(exactly = 1) {
                    emailSendHistoryRepository.countByUserIdAndSendStatus(testUserId, testStatus)
                }
                coVerify(exactly = 1) {
                    emailSendHistoryRepository.findByUserIdAndSendStatus(testUserId, testStatus)
                }
            }
        }
    }
})
