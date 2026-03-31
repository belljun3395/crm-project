package com.manage.crm.segment.application

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
    }

    suspend fun execute(useCaseIn: GetSegmentMatchedUsersUseCaseIn): GetSegmentMatchedUsersUseCaseOut {
        val targetUserIds = segmentTargetingService.resolveUserIds(useCaseIn.segmentId, useCaseIn.campaignId)
        if (targetUserIds.isEmpty()) {
            return out {
                GetSegmentMatchedUsersUseCaseOut(users = emptyList())
            }
        }

        val users = userRepository.findAllByIdIn(targetUserIds)
        val matchedUsers = users
            .mapNotNull { user -> toMatchedUserDtoOrNull(user) }
            .sortedBy { it.id }

        return out {
            GetSegmentMatchedUsersUseCaseOut(users = matchedUsers)
        }
    }

    private fun toMatchedUserDtoOrNull(user: User): SegmentMatchedUserDto? {
        val userId = user.id ?: return null
        val userAttributes = runCatching { objectMapper.readTree(user.userAttributes.value) }
            .onFailure { error ->
                log.warn(error) {
                    "Failed to parse userAttributes JSON for userId=${user.id}, externalId=${user.externalId}"
                }
            }
            .getOrNull()
        return SegmentMatchedUserDto(
            id = userId,
            externalId = user.externalId,
            email = userAttributes?.get("email")?.asText(),
            name = userAttributes?.get("name")?.asText(),
            createdAt = user.createdAt?.format(FORMATTER)
        )
    }
}
