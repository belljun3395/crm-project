package com.manage.crm.user.application

import com.manage.crm.support.out
import com.manage.crm.user.application.dto.GetTotalUserCountUseCaseOut
import com.manage.crm.user.application.service.UserService
import org.springframework.stereotype.Service

/**
 * - `totalCount`: 서비스를 통해 총 사용자 수를 조회한다.
 */
@Service
class GetTotalUserCountUseCase(
    private val userService: UserService
) {
    suspend fun execute(): GetTotalUserCountUseCaseOut {
        val totalCount = userService.getTotalUserCount()

        return out {
            GetTotalUserCountUseCaseOut(
                totalCount = totalCount
            )
        }
    }
}
