package com.manage.crm.segment.service

/**
 * Resolves user ids that satisfy a segment condition set.
 */
interface SegmentTargetingService {
    /**
     * Returns user ids that match all conditions in a segment.
     *
     * If [campaignId] is provided, evaluation scope is limited to users/events linked to that campaign.
     */
    suspend fun resolveUserIds(segmentId: Long, campaignId: Long?): List<Long>
}
