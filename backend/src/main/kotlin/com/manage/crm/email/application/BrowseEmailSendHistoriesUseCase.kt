package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseIn
import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseOut
import com.manage.crm.email.application.dto.EmailSendHistoryDto
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BrowseEmailSendHistoriesUseCase(
    private val emailSendHistoryRepository: EmailSendHistoryRepository
) {
    suspend fun execute(useCaseIn: BrowseEmailSendHistoriesUseCaseIn): BrowseEmailSendHistoriesUseCaseOut {
        val userId = useCaseIn.userId
        val sendStatus = useCaseIn.sendStatus
        val page = useCaseIn.page.coerceAtLeast(0)
        val size = useCaseIn.size.coerceIn(1, 100)

        val totalCount = when {
            userId != null && sendStatus != null -> {
                emailSendHistoryRepository.countByUserIdAndSendStatus(userId, sendStatus)
            }
            userId != null -> {
                emailSendHistoryRepository.countByUserId(userId)
            }
            sendStatus != null -> {
                emailSendHistoryRepository.countBySendStatus(sendStatus)
            }
            else -> {
                emailSendHistoryRepository.count()
            }
        }

        val histories = when {
            userId != null && sendStatus != null -> {
                emailSendHistoryRepository.findByUserIdAndSendStatusOrderByCreatedAtDesc(userId, sendStatus)
            }
            userId != null -> {
                emailSendHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
            }
            sendStatus != null -> {
                emailSendHistoryRepository.findBySendStatusOrderByCreatedAtDesc(sendStatus)
            }
            else -> {
                emailSendHistoryRepository.findAllByOrderByCreatedAtDesc()
            }
        }
            .drop(page * size)
            .take(size)
            .toList()

        return out {
            BrowseEmailSendHistoriesUseCaseOut(
                histories = histories.map { history ->
                    EmailSendHistoryDto(
                        id = history.id!!,
                        userId = history.userId,
                        userEmail = history.userEmail.value,
                        emailMessageId = history.emailMessageId,
                        emailBody = history.emailBody,
                        sendStatus = history.sendStatus,
                        createdAt = history.createdAt.toString(),
                        updatedAt = history.updatedAt.toString()
                    )
                },
                totalCount = totalCount.toInt(),
                page = page,
                size = size
            )
        }
    }
}
