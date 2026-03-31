package com.manage.crm.segment.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseIn
import com.manage.crm.segment.application.dto.GetSegmentMatchedUsersUseCaseOut
import com.manage.crm.segment.application.dto.SegmentMatchedUserDto
import com.manage.crm.segment.application.port.query.SegmentReadPort
import com.manage.crm.support.out
import com.manage.crm.user.application.port.query.UserReadModel
import com.manage.crm.user.application.port.query.UserReadPort
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
    private val segmentReadPort: SegmentReadPort,
    private val userReadPort: UserReadPort,
    private val objectMapper: ObjectMapper
) {
    private val log = KotlinLogging.logger {}

    companion object {
        private val FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    suspend fun execute(useCaseIn: GetSegmentMatchedUsersUseCaseIn): GetSegmentMatchedUsersUseCaseOut {
        val targetUserIds = segmentReadPort.findTargetUserIds(useCaseIn.segmentId, useCaseIn.campaignId)
        if (targetUserIds.isEmpty()) {
            return out {
                GetSegmentMatchedUsersUseCaseOut(users = emptyList())
            }
        }

        val users = userReadPort.findAllByIdIn(targetUserIds)
        val matchedUsers = users
            .mapNotNull { user -> toMatchedUserDtoOrNull(user) }
            .sortedBy { it.id }

        return out {
            GetSegmentMatchedUsersUseCaseOut(users = matchedUsers)
        }
    }

    private fun toMatchedUserDtoOrNull(user: UserReadModel): SegmentMatchedUserDto? {
        val userAttributes = runCatching { objectMapper.readTree(user.userAttributesJson) }
            .onFailure { error ->
                log.warn(error) {
                    "Failed to parse userAttributes JSON for userId=${user.id}, externalId=${user.externalId}"
                }
            }
            .getOrNull()
        return SegmentMatchedUserDto(
            id = user.id,
            externalId = user.externalId,
            email = userAttributes?.get("email")?.asText(),
            name = userAttributes?.get("name")?.asText(),
            createdAt = user.createdAt?.format(FORMATTER)
        )
    }
}
