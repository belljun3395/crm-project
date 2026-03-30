package com.manage.crm.event.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CampaignCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper
) : CampaignCustomRepository {
    override fun findRecentCampaigns(limit: Int) =
        dataBaseClient.sql(
            """
            SELECT *
            FROM campaigns
            ORDER BY created_at DESC
            LIMIT :limit
            """.trimIndent()
        )
            .bind("limit", limit)
            .fetch()
            .all()
            .map { row ->
                Campaign(
                    id = (row["id"] as Number).toLong(),
                    name = row["name"] as String,
                    properties = CampaignProperties(
                        objectMapper.readValue(row["properties"] as String, List::class.java)
                            .stream()
                            .map { objectMapper.convertValue(it, Map::class.java) }
                            .map { CampaignProperty(it["key"] as String, it["value"] as String) }
                            .toList()
                    ),
                    createdAt = row["created_at"] as? LocalDateTime
                )
            }
            .asFlow()
}
