package com.manage.crm.user.application.service

interface UserService {
    /**
     * Get the total user count
     */
    suspend fun getTotalUserCount(): Long

    /**
     * Increment the total user count and return the new value
     */
    suspend fun incrementTotalUserCount(): Long
}
