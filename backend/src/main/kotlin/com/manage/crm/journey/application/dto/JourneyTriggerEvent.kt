package com.manage.crm.journey.application.dto

import java.time.LocalDateTime

/**
 * Journey-owned event carrier type.
 *
 * Replaces direct use of event.domain.Event inside journey's automation layer.
 * Properties are stored as a flat map for O(1) lookup during condition evaluation.
 */
data class JourneyTriggerEvent(
    val id: Long,
    val name: String,
    val userId: Long,
    val properties: Map<String, String>,
    val createdAt: LocalDateTime,
)
