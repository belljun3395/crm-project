package com.manage.crm.user.application

import com.manage.crm.support.out
import com.manage.crm.user.application.dto.EnrollUserUseCaseIn
import com.manage.crm.user.application.dto.EnrollUserUseCaseOut
import com.manage.crm.user.application.service.JsonService
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.domain.vo.Json
import com.manage.crm.user.domain.vo.RequiredUserAttributeKey
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException

/**
 * - `userAttributes`
 *     - `User`  엔티티의 속성을 JSON 형태로 받는다.
 *     - `email` 속성이 반드시 포함되어야 한다.
 * - `updateOrSaveUser`: `id`가 있으면 `User` 엔티티를 업데이트하고, 없으면 새로운 `User` 엔티티를 저장한다.
 */
@Service
class EnrollUserUseCase(
    private val userRepository: UserRepository,
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
                    ?: throw IllegalArgumentException("User not found by id: $id")
            } else {
                userRepository.save(
                    User(
                        externalId = externalId,
                        userAttributes = userAttributes
                    )
                )
            }
        }

        return out {
            EnrollUserUseCaseOut(
                id = updateOrSaveUser.id!!,
                externalId = updateOrSaveUser.externalId!!,
                userAttributes = updateOrSaveUser.userAttributes?.value!!
            )
        }
    }
}
