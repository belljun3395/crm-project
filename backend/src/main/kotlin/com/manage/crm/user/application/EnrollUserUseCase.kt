package com.manage.crm.user.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.application.service.JsonService
import com.manage.crm.user.application.service.UserRepositoryEventProcessor
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import com.manage.crm.user.domain.vo.UserAttributes
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

@Service
class EnrollUserUseCase(
    private val userRepository: UserRepository,
    private val userRepositoryEventProcessor: UserRepositoryEventProcessor,
    private val jsonService: JsonService,
    private val snsClient: SnsClient,
    private val env: Environment
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val snsTopicArn: String? = env.getProperty("AWS_SNS_CACHE_INVALIDATION_TOPIC_ARN")

    @Transactional
    suspend fun execute(useCaseIn: EnrollUserUseCaseIn): EnrollUserUseCaseOut {
        val id: Long? = useCaseIn.id
        val externalId: String = useCaseIn.externalId
        val userAttributes: UserAttributes = useCaseIn.userAttributes
            .let { jsonService.execute(it, RequiredUserAttributeKey.EMAIL) }
            .let { UserAttributes(it) }

        val updateOrSaveUser = run {
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
                        userAttributes = userAttributes
                    )
                )
            }
        }

        // Publish cache invalidation message
        val userId = updateOrSaveUser.id!!
        val message = "{\"action\":\"invalidate\", \"keys\":[\"user:$userId\"]}"
        if (snsTopicArn.isNullOrBlank()) {
            log.warn("Skip cache invalidation publish: AWS_SNS_CACHE_INVALIDATION_TOPIC_ARN is not set")
        } else {
            val publishRequest = PublishRequest.builder()
                .topicArn(snsTopicArn)
                .message(message)
                .build()
            snsClient.publish(publishRequest)
        }

        return out {
            EnrollUserUseCaseOut(
                id = userId,
                externalId = updateOrSaveUser.externalId,
                userAttributes = updateOrSaveUser.userAttributes.value
            )
        }
    }
}
