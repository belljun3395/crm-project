package com.manage.crm.user.application

import com.manage.crm.support.exception.NotFoundByIdException
import com.manage.crm.support.out
import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.application.service.JsonService
import com.manage.crm.user.application.service.UserRepositoryEventProcessor
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EnrollUserUseCase(
    private val userRepository: UserRepository,
    private val userRepositoryEventProcessor: UserRepositoryEventProcessor,
    private val jsonService: JsonService
) {
    @Transactional
    suspend fun execute(useCaseIn: EnrollUserUseCaseIn): EnrollUserUseCaseOut {
        val id: Long? = useCaseIn.id
        val externalId: String = useCaseIn.externalId
        val userAttributes: Json = useCaseIn.userAttributes.let { jsonService.execute(it, RequiredUserAttributeKey.EMAIL) }

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

        return out {
            EnrollUserUseCaseOut(
                id = updateOrSaveUser.id!!,
                externalId = updateOrSaveUser.externalId,
                userAttributes = updateOrSaveUser.userAttributes.value
            )
        }
    }
}
