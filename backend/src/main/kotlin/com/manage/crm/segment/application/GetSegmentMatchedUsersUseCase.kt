package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseIn
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseOut
import com.manage.crm.segment.application.dto.SegmentMatchedUserDto
import com.manage.crm.segment.service.SegmentTargetingService
import com.manage.crm.support.out
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * UC-SEGMENT-005
 * Lists users that match segment rules with optional campaign scope.
 *
 * Input: segment id and optional campaign id.
 * Success: returns matched users sorted by id with compact profile fields.
 */
@Component
class GetSegmentMatchedUsersUseCase(
    private val segmentTargetingService: SegmentTargetingService,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        private const val PAGE_SIZE = 500
    }

    /**
     * Resolves user ids from segment targeting and materializes user summaries.
     */
    suspend fun execute(useCaseIn: GetSegmentMatchedUsersUseCaseIn): GetSegmentMatchedUsersUseCaseOut {
        val targetUserIds = segmentTargetingService.resolveUserIds(useCaseIn.segmentId, useCaseIn.campaignId)
        if (targetUserIds.isEmpty()) {
            return out {
                GetSegmentMatchedUsersUseCaseOut(users = emptyList())
            }
        }

        val targetIdSet = targetUserIds.toSet()
        val users = loadUsersInTargetSet(targetIdSet)
        val matchedUsers = users
            .mapNotNull { user -> toMatchedUserDtoOrNull(user) }
            .sortedBy { it.id }

        return out {
            GetSegmentMatchedUsersUseCaseOut(users = matchedUsers)
        }
    }

    /**
     * Loads users page-by-page and keeps only ids present in [targetIdSet].
     */
    private suspend fun loadUsersInTargetSet(targetIdSet: Set<Long>): List<User> {
        val users = mutableListOf<User>()
        var page = 0
        while (true) {
            val batch = userRepository.findAllWithPagination(page, PAGE_SIZE)
            if (batch.isEmpty()) {
                break
            }
            users += batch.filter { user ->
                val userId = user.id ?: return@filter false
                targetIdSet.contains(userId)
            }
            if (batch.size < PAGE_SIZE) {
                break
            }
            page += 1
        }
        return users
    }

    /**
     * Converts one domain user to API-facing matched-user dto.
     */
    private fun toMatchedUserDtoOrNull(user: User): SegmentMatchedUserDto? {
        val userId = user.id ?: return null
        val userAttributes = parseUserAttributes(user)
        return SegmentMatchedUserDto(
            id = userId,
            externalId = user.externalId,
            email = userAttributes?.get("email")?.asText(),
            name = userAttributes?.get("name")?.asText(),
            createdAt = user.createdAt?.format(FORMATTER)
        )
    }

    /**
     * Parses user attribute json and tolerates malformed json.
     */
    private fun parseUserAttributes(user: User): JsonNode? {
        return runCatching { objectMapper.readTree(user.userAttributes.value) }
            .onFailure { error ->
                log.warn(error) {
                    "Failed to parse userAttributes JSON for userId=${user.id}, externalId=${user.externalId}"
                }
            }
            .getOrNull()
    }
}
