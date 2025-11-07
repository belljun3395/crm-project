package com.manage.crm.event.controller

import com.manage.crm.config.SwaggerTag
import com.manage.crm.event.application.GetCampaignDashboardUseCase
import com.manage.crm.event.application.GetCampaignSummaryUseCase
import com.manage.crm.event.application.GetStreamStatusUseCase
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseIn
import com.manage.crm.event.application.dto.GetCampaignDashboardUseCaseOut
import com.manage.crm.event.application.dto.GetCampaignSummaryUseCaseIn
import com.manage.crm.event.application.dto.GetStreamStatusUseCaseIn
import com.manage.crm.event.controller.dto.CampaignEventData
import com.manage.crm.event.controller.dto.CampaignSummaryResponse
import com.manage.crm.event.controller.dto.StreamStatusResponse
import com.manage.crm.event.domain.TimeWindowUnit
import com.manage.crm.event.service.CampaignDashboardService
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime

@Tag(name = SwaggerTag.EVENT_SWAGGER_TAG, description = "캠페인 대시보드 API")
@Validated
@RestController
@RequestMapping(value = ["/api/v1/campaigns"])
class CampaignDashboardController(
    private val getCampaignDashboardUseCase: GetCampaignDashboardUseCase,
    private val getCampaignSummaryUseCase: GetCampaignSummaryUseCase,
    private val getStreamStatusUseCase: GetStreamStatusUseCase,
    private val campaignDashboardService: CampaignDashboardService
) {

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
        durationSeconds: Long = 3600
    ): Flux<ServerSentEvent<CampaignEventData>> {
        val duration = Duration.ofSeconds(durationSeconds)

        return campaignDashboardService.streamCampaignEvents(campaignId)
            .map { event ->
                ServerSentEvent.builder<CampaignEventData>()
                    .id(event.eventId.toString())
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
        val result = getStreamStatusUseCase.execute(
            GetStreamStatusUseCaseIn(campaignId = campaignId)
        )
        val response = StreamStatusResponse(
            campaignId = result.campaignId,
            streamLength = result.streamLength,
            checkedAt = result.checkedAt
        )
        return ApiResponseGenerator.success(response, HttpStatus.OK)
    }
}
