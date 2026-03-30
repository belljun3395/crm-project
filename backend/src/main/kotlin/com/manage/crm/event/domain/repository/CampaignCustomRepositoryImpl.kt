package com.manage.crm.event.domain.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.event.domain.Campaign
import com.manage.crm.event.domain.vo.CampaignProperties
import com.manage.crm.event.domain.vo.CampaignProperty
import com.manage.crm.infrastructure.jooq.CrmJooqTables
import com.manage.crm.infrastructure.jooq.JooqR2dbcExecutor
import com.manage.crm.infrastructure.jooq.optionalLocalDateTime
import io.r2dbc.postgresql.codec.Json
import kotlinx.coroutines.flow.flow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CampaignCustomRepositoryImpl(
    private val dslContext: DSLContext,
    private val jooqExecutor: JooqR2dbcExecutor,
    private val objectMapper: ObjectMapper
) : CampaignCustomRepository {
    override fun findRecentCampaigns(limit: Int) = flow {
        val query = dslContext
            .select()
            .from(CrmJooqTables.Campaigns.table)
            .orderBy(CrmJooqTables.Campaigns.createdAt.desc())
            .limit(limit)

        val campaigns = jooqExecutor.fetchList(query) { row ->
            val propertiesJson = when (val value = row["properties"]) {
                is Json -> value.asString()
                else -> value.toString()
            }

            Campaign(
                id = (row["id"] as Number).toLong(),
                name = row["name"] as String,
                properties = CampaignProperties(
                    objectMapper.readValue(propertiesJson, List::class.java)
                        .stream()
                        .map { objectMapper.convertValue(it, Map::class.java) }
                        .map { CampaignProperty(it["key"] as String, it["value"] as String) }
                        .toList()
                ),
                createdAt = row.optionalLocalDateTime("created_at")
            )
        }
        campaigns.forEach { emit(it) }
    }
}
