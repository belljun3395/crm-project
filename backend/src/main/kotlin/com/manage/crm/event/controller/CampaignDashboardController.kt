package com.manage.crm.event.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.event.application.DeleteCampaignUseCase
import com.manage.crm.event.application.GetCampaignDashboardStreamStatusUseCase
import com.manage.crm.event.application.GetCampaignDashboardUseCase
import com.manage.crm.event.application.GetCampaignFunnelAnalyticsUseCase
import com.manage.crm.event.application.GetCampaignSegmentComparisonUseCase
import com.manage.crm.event.application.GetCampaignSummaryUseCase
import com.manage.crm.event.application.GetCampaignUseCase
import com.manage.crm.event.application.ListCampaignsUseCase
import com.manage.crm.event.application.PostCampaignUseCase
import com.manage.crm.event.application.StreamCampaignDashboardUseCase
import com.manage.crm.event.application.UpdateCampaignUseCase
import com.manage.crm.event.application.dto.CampaignPropertyUseCaseDto
import com.manage.crm.event.application.dto.DeleteCampaignUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignFunnelAnalyticsUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignSegmentComparisonUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignUseCaseIn
import com.manage.crm.event.application.dto.GetStreamStatusUseCaseIn
import com.manage.crm.event.application.dto.ListCampaignsUseCaseIn
import com.manage.crm.event.application.dto.PostCampaignPropertyDto
import com.manage.crm.event.application.dto.PostCampaignUseCaseIn
import com.manage.crm.event.application.dto.PostCampaignUseCaseOut
import com.manage.crm.event.application.dto.StreamCampaignDashboardUseCaseIn
import com.manage.crm.event.application.dto.UpdateCampaignUseCaseIn
import com.manage.crm.event.controller.dto.CampaignEventData
import com.manage.crm.event.controller.dto.CampaignSummaryResponse
import com.manage.crm.event.controller.dto.StreamStatusResponse
import com.manage.crm.event.controller.request.PostCampaignRequest
import com.manage.crm.event.controller.request.PutCampaignRequest
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.support.web.ApiResponse
import com.manage.crm.support.web.ApiResponseGenerator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Tag(name = SwaggerTag.EVENT_SWAGGER_TAG, description = "캠페인 대시보드 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/campaigns"])
class CampaignDashboardController(
    private val listCampaignsUseCase: ListCampaignsUseCase,
    private val getCampaignUseCase: GetCampaignUseCase,
    private val updateCampaignUseCase: UpdateCampaignUseCase,
    private val deleteCampaignUseCase: DeleteCampaignUseCase,
    private val streamCampaignDashboardUseCase: StreamCampaignDashboardUseCase,
    private val getCampaignDashboardUseCase: GetCampaignDashboardUseCase,
    private val getCampaignFunnelAnalyticsUseCase: GetCampaignFunnelAnalyticsUseCase,
    private val getCampaignSegmentComparisonUseCase: GetCampaignSegmentComparisonUseCase,
    private val getCampaignSummaryUseCase: GetCampaignSummaryUseCase,
    private val getCampaignDashboardStreamStatusUseCase: GetCampaignDashboardStreamStatusUseCase,
    private val postCampaignUseCase: PostCampaignUseCase
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    @GetMapping
    suspend fun listCampaigns(
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ApiResponse<ApiResponse.SuccessBody<List<CampaignListItemDto>>> {
        val result = listCampaignsUseCase.execute(ListCampaignsUseCaseIn(limit = limit))
        val response = result.campaigns.map {
            CampaignListItemDto(
                id = it.id,
                name = it.name,
                createdAt = it.createdAt?.format(formatter)
            )
        }
        return ApiResponseGenerator.success(response, HttpStatus.OK)
    }

    @GetMapping("/{campaignId}")
    suspend fun getCampaign(
        @PathVariable campaignId: Long
    ): ApiResponse<ApiResponse.SuccessBody<CampaignDetailDto>> {
        val campaign = getCampaignUseCase.execute(GetCampaignUseCaseIn(campaignId = campaignId))
        return ApiResponseGenerator.success(
            CampaignDetailDto(
                id = campaign.id,
                name = campaign.name,
                properties = campaign.properties.map {
                    CampaignPropertyDto(key = it.key, value = it.value)
                },
                segmentIds = campaign.segmentIds,
                createdAt = campaign.createdAt?.format(formatter)
            ),
            HttpStatus.OK
        )
    }

    @PostMapping
    suspend fun postCampaign(
        @RequestBody request: PostCampaignRequest
    ): ApiResponse<ApiResponse.SuccessBody<PostCampaignUseCaseOut>> {
        return postCampaignUseCase
            .execute(
                PostCampaignUseCaseIn(
                    name = request.name,
                    segmentIds = request.segmentIds ?: emptyList(),
                    properties = request.properties.map {
                        PostCampaignPropertyDto(key = it.key, value = it.value)
                    }
                )
            )
            .let { ApiResponseGenerator.success(it, HttpStatus.CREATED) }
    }

    @PutMapping("/{campaignId}")
    suspend fun updateCampaign(
        @PathVariable campaignId: Long,
        @RequestBody request: PutCampaignRequest
    ): ApiResponse<ApiResponse.SuccessBody<CampaignDetailDto>> {
        val result = updateCampaignUseCase.execute(
            UpdateCampaignUseCaseIn(
                campaignId = campaignId,
                name = request.name,
                properties = request.properties.map {
                    CampaignPropertyUseCaseDto(key = it.key, value = it.value)
                },
                segmentIds = request.segmentIds
            )
        )

        return ApiResponseGenerator.success(
            CampaignDetailDto(
                id = result.id,
                name = result.name,
                properties = result.properties.map {
                    CampaignPropertyDto(key = it.key, value = it.value)
                },
                segmentIds = result.segmentIds,
                createdAt = result.createdAt?.format(formatter)
            ),
            HttpStatus.OK
        )
    }

    @DeleteMapping("/{campaignId}")
    suspend fun deleteCampaign(
        @PathVariable campaignId: Long
    ): ApiResponse<ApiResponse.SuccessBody<CampaignDeleteResponseDto>> {
        val result = deleteCampaignUseCase.execute(DeleteCampaignUseCaseIn(campaignId = campaignId))
        return ApiResponseGenerator.success(CampaignDeleteResponseDto(success = result.success), HttpStatus.OK)
    }

    @Operation(
        summary = "캠페인 대시보드 조회",
        description = "특정 캠페인의 대시보드 메트릭 정보를 조회합니다. 시간 범위 또는 시간 단위를 지정할 수 있습니다."
    )
    @GetMapping("/{campaignId}/dashboard")
    suspend fun getCampaignDashboard(
        @PathVariable campaignId: Long,
        @Parameter(description = "조회 시작 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startTime: LocalDateTime? = null,
        @Parameter(description = "조회 종료 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endTime: LocalDateTime? = null,
        @Parameter(description = "시간 단위: MINUTE, HOUR, DAY, WEEK, MONTH")
        @RequestParam(required = false)
        timeWindowUnit: TimeWindowUnit? = null
    ): ApiResponse<ApiResponse.SuccessBody<GetCampaignDashboardUseCaseOut>> {
        val result = getCampaignDashboardUseCase.execute(
            GetCampaignDashboardUseCaseIn(
                campaignId = campaignId,
                startTime = startTime,
                endTime = endTime,
                timeWindowUnit = timeWindowUnit
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }

    @Operation(
        summary = "캠페인 대시보드 실시간 스트리밍",
        description = "캠페인의 실시간 이벤트 스트림을 Server-Sent Events (SSE)로 전송합니다."
    )
    @GetMapping(
        path = ["/{campaignId}/dashboard/stream"],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun streamCampaignDashboard(
        @PathVariable campaignId: Long,
        @Parameter(description = "스트리밍 지속 시간 (초), 기본값: 3600초 (1시간)")
        @RequestParam(required = false, defaultValue = "3600")
        durationSeconds: Long = 3600,
        @Parameter(description = "SSE 재연결용 Last-Event-ID (쿼리 파라미터)")
        @RequestParam(required = false)
        lastEventId: String? = null,
        @Parameter(description = "SSE 재연결용 Last-Event-ID (헤더)")
        @RequestHeader(name = "Last-Event-ID", required = false)
        lastEventIdHeader: String? = null
    ): Flux<ServerSentEvent<CampaignEventData>> {
        val duration = Duration.ofSeconds(durationSeconds)
        val resolvedLastEventId = lastEventId ?: lastEventIdHeader
        val connectedEvent = ServerSentEvent.builder<CampaignEventData>()
            .event("connected")
            .comment("Stream connected")
            .build()

        return streamCampaignDashboardUseCase.execute(
            StreamCampaignDashboardUseCaseIn(
                campaignId = campaignId,
                durationSeconds = durationSeconds,
                lastEventId = resolvedLastEventId
            )
        )
            .map { event ->
                val eventId = event.streamId ?: event.eventId.toString()
                ServerSentEvent.builder<CampaignEventData>()
                    .id(eventId)
                    .event("campaign-event")
                    .data(
                        CampaignEventData(
                            campaignId = event.campaignId,
                            eventId = event.eventId,
                            userId = event.userId,
                            eventName = event.eventName,
                            timestamp = event.timestamp
                        )
                    )
                    .build()
            }
            .startWith(connectedEvent)
            .timeout(duration)
            .onErrorResume { error ->
                Flux.just(
                    ServerSentEvent.builder<CampaignEventData>()
                        .event("error")
                        .comment(error.message ?: "Stream error occurred")
                        .build()
                )
            }
            .concatWith(
                Flux.just(
                    ServerSentEvent.builder<CampaignEventData>()
                        .event("stream-end")
                        .comment("Stream ended")
                        .build()
                )
            )
    }

    @Operation(
        summary = "캠페인 요약 정보 조회",
        description = "캠페인의 요약 통계 정보를 조회합니다 (전체/24시간/7일 이벤트 수 등)."
    )
    @GetMapping("/{campaignId}/dashboard/summary")
    suspend fun getCampaignSummary(
        @PathVariable campaignId: Long
    ): ApiResponse<ApiResponse.SuccessBody<CampaignSummaryResponse>> {
        val result = getCampaignSummaryUseCase.execute(
            GetCampaignSummaryUseCaseIn(campaignId = campaignId)
        )
        val response = CampaignSummaryResponse(
            campaignId = result.campaignId,
            totalEvents = result.totalEvents,
            eventsLast24Hours = result.eventsLast24Hours,
            eventsLast7Days = result.eventsLast7Days,
            lastUpdated = result.lastUpdated
        )
        return ApiResponseGenerator.success(response, HttpStatus.OK)
    }

    @Operation(
        summary = "캠페인 스트림 상태 조회",
        description = "Redis Stream의 현재 이벤트 개수를 조회합니다 (모니터링 및 헬스체크용)."
    )
    @GetMapping("/{campaignId}/dashboard/stream/status")
    suspend fun getStreamStatus(
        @PathVariable campaignId: Long
    ): ApiResponse<ApiResponse.SuccessBody<StreamStatusResponse>> {
        val result = getCampaignDashboardStreamStatusUseCase.execute(
            GetStreamStatusUseCaseIn(campaignId = campaignId)
        )
        val response = StreamStatusResponse(
            campaignId = result.campaignId,
            streamLength = result.streamLength,
            checkedAt = result.checkedAt
        )
        return ApiResponseGenerator.success(response, HttpStatus.OK)
    }

    @Operation(
        summary = "캠페인 퍼널 분석 조회",
        description = "이벤트 단계(step)를 기준으로 캠페인 퍼널 지표(이벤트 수, 유효 유저 수, 이전 단계 대비 전환율)를 조회합니다."
    )
    @GetMapping("/{campaignId}/analytics/funnel")
    suspend fun getCampaignFunnelAnalytics(
        @PathVariable campaignId: Long,
        @Parameter(description = "퍼널 단계 이벤트 이름(쉼표 구분)")
        @RequestParam
        steps: String,
        @Parameter(description = "조회 시작 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startTime: LocalDateTime? = null,
        @Parameter(description = "조회 종료 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endTime: LocalDateTime? = null
    ): ApiResponse<ApiResponse.SuccessBody<GetCampaignFunnelAnalyticsUseCaseOut>> {
        val parsedSteps = steps.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val result = getCampaignFunnelAnalyticsUseCase.execute(
            GetCampaignFunnelAnalyticsUseCaseIn(
                campaignId = campaignId,
                steps = parsedSteps,
                startTime = startTime,
                endTime = endTime
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }

    @Operation(
        summary = "캠페인 세그먼트 비교 분석 조회",
        description = "세그먼트별 타겟 유저 대비 이벤트 전환율을 비교 조회합니다."
    )
    @GetMapping("/{campaignId}/analytics/segment-comparison")
    suspend fun getCampaignSegmentComparison(
        @PathVariable campaignId: Long,
        @Parameter(description = "비교할 세그먼트 ID 목록(쉼표 구분)")
        @RequestParam
        segmentIds: String,
        @Parameter(description = "필터링할 이벤트 이름 (미입력 시 캠페인 전체 이벤트 대상)")
        @RequestParam(required = false)
        eventName: String?,
        @Parameter(description = "조회 시작 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startTime: LocalDateTime? = null,
        @Parameter(description = "조회 종료 시간 (ISO 8601 형식)")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        endTime: LocalDateTime? = null
    ): ApiResponse<ApiResponse.SuccessBody<GetCampaignSegmentComparisonUseCaseOut>> {
        val parsedSegmentIds = segmentIds
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()

        val result = getCampaignSegmentComparisonUseCase.execute(
            GetCampaignSegmentComparisonUseCaseIn(
                campaignId = campaignId,
                segmentIds = parsedSegmentIds,
                eventName = eventName,
                startTime = startTime,
                endTime = endTime
            )
        )
        return ApiResponseGenerator.success(result, HttpStatus.OK)
    }
}

data class CampaignListItemDto(
    val id: Long,
    val name: String,
    val createdAt: String?
)

data class CampaignPropertyDto(
    val key: String,
    val value: String
)

data class CampaignDetailDto(
    val id: Long,
    val name: String,
    val properties: List<CampaignPropertyDto>,
    val segmentIds: List<Long>,
    val createdAt: String?
)

data class CampaignDeleteResponseDto(
    val success: Boolean
)
