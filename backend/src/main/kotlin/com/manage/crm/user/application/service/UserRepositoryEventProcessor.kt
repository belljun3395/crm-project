package com.manage.crm.user.application.service

import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import com.manage.crm.user.event.NewUserEvent
import com.manage.crm.user.support.UserEventPublisher
import org.springframework.stereotype.Component

@Component
class UserRepositoryEventProcessor(
    private val userRepository: UserRepository,
    private val userEventPublisher: UserEventPublisher
) {
    /**
     * 사용자 정보를 저장하고 이벤트 관련 후처리를 수행합니다.
     * - 사용자가 새로 생성된 경우: `NewUserEvent`를 발행합니다.
     */
    suspend fun save(user: User): User {
        return if (user.isNewUser()) {
            userRepository.save(user).apply {
                this.id?.let {
                    userEventPublisher.publish(NewUserEvent(it))
                }
            }
        } else {
            userRepository.save(user)
        }
    }
}
