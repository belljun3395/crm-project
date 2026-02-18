package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.vo.UserAttributes
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

private const val LIKE_ESCAPE_CHARACTER = "\\"

@Repository
class UserRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient
) : UserRepositoryCustom {

    override suspend fun findByEmail(email: String): User? {
        val selectQuery = """
            SELECT * FROM users
            WHERE user_attributes LIKE :pattern ESCAPE '\\'
        """.trimIndent()
        val pattern = "%\"email\": \"${escapeLikePattern(email)}\"%"

        return dataBaseClient.sql(selectQuery)
            .bind("pattern", pattern)
            .fetch()
            .all()
            .map {
                User.new(
                    id = it["id"] as Long,
                    externalId = it["external_id"] as String,
                    userAttributes = UserAttributes(it["user_attributes"] as String),
                    createdAt = it["created_at"] as LocalDateTime,
                    updatedAt = it["updated_at"] as LocalDateTime
                )
            }.awaitFirst()
    }

    override suspend fun findAllWithPagination(page: Int, size: Int): List<User> {
        val offset = page * size
        // ORDER BY id DESC: Return users in reverse chronological order (newest first)
        val query = """
            SELECT * FROM users
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        return dataBaseClient.sql(query)
            .bind("limit", size)
            .bind("offset", offset)
            .fetch()
            .all()
            .map {
                User.new(
                    id = it["id"] as Long,
                    externalId = it["external_id"] as String,
                    userAttributes = UserAttributes(it["user_attributes"] as String),
                    createdAt = it["created_at"] as LocalDateTime,
                    updatedAt = it["updated_at"] as LocalDateTime
                )
            }
            .collectList()
            .awaitFirst()
    }

    override suspend fun countAll(): Long {
        val query = "SELECT COUNT(*) as count FROM users"

        return dataBaseClient.sql(query)
            .fetch()
            .one()
            .map { it["count"] as Long }
            .awaitFirstOrNull() ?: 0L
    }

    override suspend fun searchUsers(query: String, page: Int, size: Int): List<User> {
        val offset = page * size
        val sql = """
            SELECT * FROM users
            WHERE external_id LIKE :pattern ESCAPE '\\'
               OR user_attributes LIKE :pattern ESCAPE '\\'
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()
        val pattern = "%${escapeLikePattern(query)}%"

        return dataBaseClient.sql(sql)
            .bind("pattern", pattern)
            .bind("limit", size)
            .bind("offset", offset)
            .fetch()
            .all()
            .map {
                User.new(
                    id = it["id"] as Long,
                    externalId = it["external_id"] as String,
                    userAttributes = UserAttributes(it["user_attributes"] as String),
                    createdAt = it["created_at"] as LocalDateTime,
                    updatedAt = it["updated_at"] as LocalDateTime
                )
            }
            .collectList()
            .awaitFirst()
    }

    override suspend fun countSearchUsers(query: String): Long {
        val sql = """
            SELECT COUNT(*) as count FROM users
            WHERE external_id LIKE :pattern ESCAPE '\\'
               OR user_attributes LIKE :pattern ESCAPE '\\'
        """.trimIndent()
        val pattern = "%${escapeLikePattern(query)}%"

        return dataBaseClient.sql(sql)
            .bind("pattern", pattern)
            .fetch()
            .one()
            .map { it["count"] as Long }
            .awaitFirstOrNull() ?: 0L
    }

    private fun escapeLikePattern(query: String): String {
        return query
            .replace(LIKE_ESCAPE_CHARACTER, "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}
