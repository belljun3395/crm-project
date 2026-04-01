package com.manage.crm.user.domain.repository

import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.infrastructure.jooq.optionalLocalDateTime
import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.vo.UserAttributes
import io.r2dbc.postgresql.codec.Json
import org.jooq.DSLContext
import org.jooq.impl.DSL.condition
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.inline
import org.springframework.stereotype.Repository

private const val LIKE_ESCAPE_CHARACTER = "\\"

/**
 * DM-USER-001
 * Provides custom user search and lookup queries.
 *
 * Input: email or search query with pagination.
 * Success: returns matching users and counts.
 * Failure: returns empty result when no matching user exists.
 * Side effects: reads user records from DB using LIKE-escaped and bound patterns.
 */
@Repository
class UserCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor,
) : UserCustomRepository {
    override suspend fun findAllExistByUserAttributesKey(key: String?): List<User> {
        val attributeKey = key ?: "email"
        val query =
            dslContext
                .select()
                .from(CrmJooqTables.Users.table)
                .where(condition("jsonb_exists({0}, {1})", CrmJooqTables.Users.userAttributes, inline(attributeKey)))

        return jooqExecutor.fetchList(query, ::toUser)
    }

    override suspend fun findByEmail(email: String): User? {
        val query =
            dslContext
                .select()
                .from(CrmJooqTables.Users.table)
                .where(condition("jsonb_extract_path_text({0}, 'email') = {1}", CrmJooqTables.Users.userAttributes, inline(email)))

        return jooqExecutor.fetchOne(query, ::toUser)
    }

    override suspend fun findAllWithPagination(
        page: Int,
        size: Int,
    ): List<User> {
        val offset = page * size
        val query =
            dslContext
                .select()
                .from(CrmJooqTables.Users.table)
                .orderBy(CrmJooqTables.Users.id.desc())
                .limit(size)
                .offset(offset)

        return jooqExecutor.fetchList(query, ::toUser)
    }

    override suspend fun countAll(): Long {
        val query =
            dslContext
                .select(count().`as`("count"))
                .from(CrmJooqTables.Users.table)

        return jooqExecutor.fetchOne(query) { (it["count"] as Number).toLong() } ?: 0L
    }

    override suspend fun searchUsers(
        query: String,
        page: Int,
        size: Int,
    ): List<User> {
        val offset = page * size
        val pattern = "%${escapeLikePattern(query)}%"
        val userAttributesAsText = CrmJooqTables.Users.userAttributes.cast(String::class.java)
        val searchQuery =
            dslContext
                .select()
                .from(CrmJooqTables.Users.table)
                .where(
                    CrmJooqTables.Users.externalId
                        .like(pattern)
                        .escape('\\'),
                ).or(userAttributesAsText.like(pattern).escape('\\'))
                .orderBy(CrmJooqTables.Users.id.desc())
                .limit(size)
                .offset(offset)

        return jooqExecutor.fetchList(searchQuery, ::toUser)
    }

    override suspend fun countSearchUsers(query: String): Long {
        val pattern = "%${escapeLikePattern(query)}%"
        val userAttributesAsText = CrmJooqTables.Users.userAttributes.cast(String::class.java)
        val countQuery =
            dslContext
                .select(count().`as`("count"))
                .from(CrmJooqTables.Users.table)
                .where(
                    CrmJooqTables.Users.externalId
                        .like(pattern)
                        .escape('\\'),
                ).or(userAttributesAsText.like(pattern).escape('\\'))

        return jooqExecutor.fetchOne(countQuery) { (it["count"] as Number).toLong() } ?: 0L
    }

    private fun escapeLikePattern(query: String): String =
        query
            .replace(LIKE_ESCAPE_CHARACTER, "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    private fun toUser(row: Map<String, Any>): User {
        val userAttributes =
            when (val value = row["user_attributes"]) {
                is Json -> value.asString()
                else -> value.toString()
            }

        return User(
            id = (row["id"] as Number).toLong(),
            externalId = row["external_id"] as String,
            userAttributes = UserAttributes(userAttributes),
            createdAt = row.optionalLocalDateTime("created_at"),
            updatedAt = row.optionalLocalDateTime("updated_at"),
        )
    }
}
