package com.manage.crm.event.stream

import com.manage.crm.event.event.CampaignDashboardEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.connection.stream.StringRecord
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.collections.get

/**
 * Encapsulates Redis Stream IO for campaign dashboard events.
 */
@Service
class CampaignDashboardStreamManager(
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, Any>
) {
    private val log = KotlinLogging.logger { }

    companion object {
        private const val STREAM_KEY_PREFIX = "campaign:dashboard:stream"
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * Builds stream key for a campaign-specific dashboard event stream.
         */
        fun getStreamKey(campaignId: Long): String = "$STREAM_KEY_PREFIX:$campaignId"
    }

    /**
     * Publishes a single dashboard event record to Redis Stream.
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
        } catch (e: Exception) {
            log.error(e) { "Failed to publish event to Redis Stream: campaignId=${event.campaignId}" }
            throw e
        }
    }

    /**
     * Opens a reactive stream reader for campaign events.
     *
     * Responsibility:
     * 1. Start from stream beginning when no cursor is provided.
     * 2. Start from explicit cursor when reconnecting.
     * 3. Keep polling Redis Stream with short blocking reads for live updates.
     * 4. Complete when requested duration expires or client disconnects.
     */
    fun streamEvents(
        campaignId: Long,
        duration: Duration = Duration.ofHours(1),
        lastEventId: String? = null
    ): Flux<CampaignDashboardEvent> {
        val streamKey = getStreamKey(campaignId)
        val startOffset = if (lastEventId.isNullOrBlank()) {
            ReadOffset.from("0-0")
        } else {
            ReadOffset.from(lastEventId)
        }

        return Flux.create<CampaignDashboardEvent> { sink ->
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            val job = scope.launch {
                var currentOffset = startOffset
                val deadline = System.currentTimeMillis() + duration.toMillis()
                while (isActive && !sink.isCancelled && System.currentTimeMillis() < deadline) {
                    val records = reactiveRedisTemplate.opsForStream<String, Any>()
                        .read(
                            StreamReadOptions.empty()
                                .count(100)
                                .block(Duration.ofSeconds(1)),
                            StreamOffset.create(streamKey, currentOffset)
                        )
                        .collectList()
                        .awaitSingle()

                    if (records.isEmpty()) {
                        continue
                    }

                    records.forEach { record ->
                        sink.next(mapRecordToEvent(record))
                        currentOffset = ReadOffset.from(record.id.value)
                    }
                }

                if (!sink.isCancelled) {
                    sink.complete()
                }
            }

            sink.onDispose {
                job.cancel()
            }
        }
            .onErrorResume { error ->
                log.error(error) { "Error streaming events for campaign: $campaignId" }
                Flux.empty()
            }
    }

    /**
     * Returns current stream length. Returns `0` on read failure.
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
     * Trims the stream to bounded length to prevent unbounded growth.
     */
    suspend fun trimStream(campaignId: Long, maxLength: Long = 10000) {
        try {
            val streamKey = getStreamKey(campaignId)
            reactiveRedisTemplate.opsForStream<String, Any>()
                .trim(streamKey, maxLength)
                .awaitFirstOrNull()
        } catch (e: Exception) {
            log.error(e) { "Failed to trim stream for campaign: $campaignId" }
        }
    }

    /**
     * Deletes an entire campaign stream key.
     */
    suspend fun deleteStream(campaignId: Long) {
        try {
            val streamKey = getStreamKey(campaignId)
            reactiveRedisTemplate.delete(streamKey).awaitFirstOrNull()
        } catch (e: Exception) {
            log.error(e) { "Failed to delete stream for campaign: $campaignId" }
        }
    }

    /**
     * Reads a bounded batch of events from a cursor.
     *
     * Starts from `0-0` when no cursor is provided.
     */
    suspend fun readEventsBatch(
        campaignId: Long,
        lastId: String?,
        count: Long = 100
    ): List<CampaignDashboardEvent> {
        val streamKey = getStreamKey(campaignId)
        val readOffset = if (lastId.isNullOrBlank()) {
            ReadOffset.from("0-0")
        } else {
            ReadOffset.from(lastId)
        }

        return try {
            reactiveRedisTemplate.opsForStream<String, Any>()
                .read(
                    StreamReadOptions.empty().count(count),
                    StreamOffset.create(streamKey, readOffset)
                )
                .map { record -> mapRecordToEvent(record) }
                .collectList()
                .awaitSingle()
        } catch (e: Exception) {
            log.error(e) { "Failed to read events batch for campaign: $campaignId" }
            emptyList()
        }
    }

    /**
     * Maps a Redis stream record to strongly typed dashboard event payload.
     */
    private fun mapRecordToEvent(record: MapRecord<String, *, *>): CampaignDashboardEvent {
        val values = record.value
        return CampaignDashboardEvent(
            campaignId = values["campaignId"]?.toString()?.toLong()
                ?: throw IllegalArgumentException("Missing campaignId"),
            eventId = values["eventId"]?.toString()?.toLong()
                ?: throw IllegalArgumentException("Missing eventId"),
            userId = values["userId"]?.toString()?.toLong()
                ?: throw IllegalArgumentException("Missing userId"),
            eventName = values["eventName"]?.toString()
                ?: throw IllegalArgumentException("Missing eventName"),
            timestamp = parseTimestamp(values["timestamp"]?.toString()) ?: LocalDateTime.now(),
            streamId = record.id.value
        )
    }

    /**
     * Parses ISO timestamp value written to stream record.
     */
    private fun parseTimestamp(timestampStr: String?): LocalDateTime? {
        return try {
            timestampStr?.let { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }
        } catch (e: Exception) {
            log.error(e) { "Failed to parse timestamp: $timestampStr" }
            null
        }
    }
}
