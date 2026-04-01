package com.manage.crm.user.application

import com.manage.crm.journey.queue.JourneyTriggerQueuePublisher
import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.support.transactional.TransactionSynchronizationTemplate
import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.application.service.JsonService
import com.manage.crm.user.application.service.UserRepositoryEventProcessor
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.cache.UserCacheManager
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import com.manage.crm.user.domain.vo.UserAttributes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UC-USER-001
 * Registers a new user or updates an existing user's attributes.
 *
 * Input: user id (optional), externalId, and user attributes containing required email.
 * Success: persists the user state and returns persisted id/externalId/attributes.
 * Failure: throws when target user id does not exist or required attributes are invalid.
 * Side effects: publishes user cache invalidation for the persisted user id.
 */
@Service
class EnrollUserUseCase(
    private val userRepository: UserRepository,
    private val userRepositoryEventProcessor: UserRepositoryEventProcessor,
    private val jsonService: JsonService,
    private val userCacheManager: UserCacheManager,
    private val journeyTriggerQueuePublisher: JourneyTriggerQueuePublisher,
    private val transactionSynchronizationTemplate: TransactionSynchronizationTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun execute(useCaseIn: EnrollUserUseCaseIn): EnrollUserUseCaseOut {
        val id: Long? = useCaseIn.id
        val externalId: String = useCaseIn.externalId
        val userAttributes: UserAttributes =
            useCaseIn.userAttributes
                .let { jsonService.execute(it, RequiredUserAttributeKey.EMAIL) }
                .let { UserAttributes(it) }

        val updateOrSaveUser =
            run {
                if (id != null) {
                    userRepository
                        .findById(id)
                        ?.apply { updateAttributes(userAttributes) }
                        ?.apply { userRepository.save(this) }
                        ?: throw NotFoundByIdException("User", id)
                } else {
                    userRepositoryEventProcessor.save(
                        User.new(
                            externalId = externalId,
                            userAttributes = userAttributes,
                        ),
                    )
                }
            }

        // Update user cache count
        val userId = updateOrSaveUser.id!!
        userCacheManager.updateTotalUserCountUpdateAt()
        runCatching {
            transactionSynchronizationTemplate.afterCommit(
                blockDescription = "enqueue journey segment trigger after user commit",
            ) {
                journeyTriggerQueuePublisher.publishSegmentContextTrigger(listOf(userId))
            }
        }.onFailure { error ->
            log.error("Failed to enqueue segment-triggered journey automation for userId=$userId", error)
        }

        return out {
            EnrollUserUseCaseOut(
                id = userId,
                externalId = updateOrSaveUser.externalId,
                userAttributes = updateOrSaveUser.userAttributes.value,
            )
        }
    }
}
