package com.manage.crm.email.application

import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseIn
import com.manage.crm.email.application.dto.BrowseEmailSendHistoriesUseCaseOut
import com.manage.crm.email.application.dto.EmailSendHistoryDto
import com.manage.crm.email.domain.repository.EmailSendHistoryRepository
import com.manage.crm.support.out
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class BrowseEmailSendHistoriesUseCase(
    private val emailSendHistoryRepository: EmailSendHistoryRepository
) {
    suspend fun execute(useCaseIn: BrowseEmailSendHistoriesUseCaseIn): BrowseEmailSendHistoriesUseCaseOut {
        val userId = useCaseIn.userId
        val sendStatus = useCaseIn.sendStatus

        val histories = when {
            userId != null && sendStatus != null -> {
                emailSendHistoryRepository.findByUserIdAndSendStatus(userId, sendStatus)
            }
            userId != null -> {
                emailSendHistoryRepository.findByUserId(userId)
            }
            sendStatus != null -> {
                emailSendHistoryRepository.findBySendStatus(sendStatus)
            }
            else -> {
                emailSendHistoryRepository.findAllByOrderByCreatedAtDesc()
            }
        }.toList()

        return out {
            histories
                .map { history ->
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
                }
                .let { BrowseEmailSendHistoriesUseCaseOut(it) }
        }
    }
}
