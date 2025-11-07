package com.manage.crm.user.domain.repository

import com.manage.crm.user.domain.User
import com.manage.crm.user.domain.vo.UserAttributes
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class UserRepositoryCustomImpl(
    private val dataBaseClient: DatabaseClient
) : UserRepositoryCustom {

    override suspend fun findByEmail(email: String): User? {
        var selectQuery = """
            SELECT * FROM users
        """.trimIndent()
        val whereClause = mutableListOf<String>()
        whereClause.add("user_attributes LIKE '%\"email\": \"$email\"%'")
        selectQuery = selectQuery.plus(" WHERE ${whereClause.joinToString(" AND ")}")

        return dataBaseClient.sql(selectQuery)
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
            .toList()
    }

    override suspend fun countAll(): Long {
        val query = "SELECT COUNT(*) as count FROM users"

        return dataBaseClient.sql(query)
            .fetch()
            .one()
            .map { it["count"] as Long }
            .awaitFirstOrNull() ?: 0L
    }
}
