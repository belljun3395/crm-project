package com.manage.crm.journey.domain.repository

interface JourneyCustomRepository {
    suspend fun updateLifecycleStatusIfVersionMatches(
        journeyId: Long,
        lifecycleStatus: String,
        active: Boolean,
        expectedVersion: Int,
        newVersion: Int,
    ): Int
}
