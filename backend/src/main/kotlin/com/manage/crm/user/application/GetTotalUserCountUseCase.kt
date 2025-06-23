package com.manage.crm.user.application

import com.manage.crm.support.out
import com.manage.crm.user.application.dto.GetTotalUserCountUseCaseOut
import com.manage.crm.user.application.service.UserService
import org.springframework.stereotype.Service

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
