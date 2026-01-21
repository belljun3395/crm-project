package com.manage.crm.event.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.connection.stream.StringRecord
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class CampaignDashboardEvent(
    val campaignId: Long,
    val eventId: Long,
    val userId: Long,
    val eventName: String,
    val timestamp: LocalDateTime,
    val streamId: String? = null
)

@Service
class CampaignDashboardStreamService(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>
) {
    val log = KotlinLogging.logger { }

    companion object {
        private const val STREAM_KEY_PREFIX = "campaign:dashboard:stream"
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        fun getStreamKey(campaignId: Long): String = "$STREAM_KEY_PREFIX:$campaignId"
    }

    /**
     * Publishes a campaign event to Redis Stream for real-time processing
     */
    suspend fun publishEvent(event: CampaignDashboardEvent) {
        try {
            val streamKey = getStreamKey(event.campaignId)
            val record: StringRecord = StreamRecords.string(
                mapOf(
                    "campaignId" to event.campaignId.toString(),
                    "eventId" to event.eventId.toString(),
                    "userId" to event.userId.toString(),
                    "eventName" to event.eventName,
                    "timestamp" to event.timestamp.format(DATE_TIME_FORMATTER)
                )
            ).withStreamKey(streamKey)

            reactiveRedisTemplate.opsForStream<String, Any>()
                .add(record)
                .awaitSingle()

            log.debug { "Published event to stream: campaignId=${event.campaignId}, eventId=${event.eventId}" }
        } catch (e: Exception) {
            log.error(e) { "Failed to publish event to Redis Stream: campaignId=${event.campaignId}" }
            throw e
        }
    }

    /**
     * Reads events from the stream in real-time
     * Returns a Flux of events that can be consumed by subscribers
     */
    fun streamEvents(
        campaignId: Long,
        duration: Duration = Duration.ofHours(1),
        lastEventId: String? = null
    ): Flux<CampaignDashboardEvent> {
        val streamKey = getStreamKey(campaignId)
        val streamOffset = when {
            lastEventId.isNullOrBlank() -> StreamOffset.latest(streamKey)
            else -> StreamOffset.fromStart(streamKey)
        }

        return reactiveRedisTemplate.opsForStream<String, Any>()
            .read(streamOffset)
            .filter { record ->
                lastEventId.isNullOrBlank() || record.id.value > lastEventId
            }
            .map { record -> mapRecordToEvent(record) }
            .timeout(duration)
            .onErrorResume { error ->
                log.error(error) { "Error streaming events for campaign: $campaignId" }
                Flux.empty()
            }
    }

    /**
     * Gets the total count of events in the stream for a campaign
     * Useful for monitoring and health checks
     */
    suspend fun getStreamLength(campaignId: Long): Long {
        return try {
            val streamKey = getStreamKey(campaignId)
            reactiveRedisTemplate.opsForStream<String, Any>()
                .size(streamKey)
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Failed to get stream length for campaign: $campaignId" }
            0L
        }
    }

    /**
     * Trims the stream to keep only the most recent events (for memory management)
     * Should be called periodically to prevent unbounded memory growth
     */
    suspend fun trimStream(campaignId: Long, maxLength: Long = 10000) {
        try {
            val streamKey = getStreamKey(campaignId)
            reactiveRedisTemplate.opsForStream<String, Any>()
                .trim(streamKey, maxLength)
                .awaitFirstOrNull()
            log.info { "Trimmed stream for campaign: $campaignId to max length: $maxLength" }
        } catch (e: Exception) {
            log.error(e) { "Failed to trim stream for campaign: $campaignId" }
        }
    }

    private fun mapRecordToEvent(record: MapRecord<String, *, *>): CampaignDashboardEvent {
        val values = record.value
        return CampaignDashboardEvent(
            campaignId = values["campaignId"]?.toString()?.toLong() ?: throw IllegalArgumentException("Missing campaignId"),
            eventId = values["eventId"]?.toString()?.toLong() ?: throw IllegalArgumentException("Missing eventId"),
            userId = values["userId"]?.toString()?.toLong() ?: throw IllegalArgumentException("Missing userId"),
            eventName = values["eventName"]?.toString() ?: throw IllegalArgumentException("Missing eventName"),
            timestamp = parseTimestamp(values["timestamp"]?.toString()) ?: LocalDateTime.now(),
            streamId = record.id.value
        )
    }

    private fun parseTimestamp(timestampStr: String?): LocalDateTime? {
        return try {
            timestampStr?.let { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }
        } catch (e: Exception) {
            log.error(e) { "Failed to parse timestamp: $timestampStr" }
            null
        }
    }
}
