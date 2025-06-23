package com.manage.crm.user.application.service

interface UserService {
    /**
     * 총 사용자 수를 반환합니다.
     */
    suspend fun getTotalUserCount(): Long

    /**
     * 총 사용자 수를 증가시키고, 업데이트된 총 사용자 수를 반환합니다.
     */
    suspend fun incrementTotalUserCount(): Long
}
