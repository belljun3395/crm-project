package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import com.manage.crm.infrastructure.scheduler.ScheduleInfo
import com.manage.crm.infrastructure.scheduler.ScheduleName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.scheduler.SchedulerClient
import software.amazon.awssdk.services.scheduler.model.ActionAfterCompletion
import software.amazon.awssdk.services.scheduler.model.ConflictException
import software.amazon.awssdk.services.scheduler.model.CreateScheduleRequest
import software.amazon.awssdk.services.scheduler.model.CreateScheduleResponse
import software.amazon.awssdk.services.scheduler.model.DeleteScheduleRequest
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindow
import software.amazon.awssdk.services.scheduler.model.FlexibleTimeWindowMode
import software.amazon.awssdk.services.scheduler.model.ListSchedulesRequest
import software.amazon.awssdk.services.scheduler.model.Target
import java.time.LocalDateTime

fun LocalDateTime.toScheduleExpression(): String =
    "at(%04d-%02d-%02dT%02d:%02d:%02d)".format(
        this.year,
        this.monthValue,
        this.dayOfMonth,
        this.hour,
        this.minute,
        this.second
    )

@Service
class AwsSchedulerService(
    private val awsSchedulerClient: SchedulerClient,
    private val buildProperties: BuildProperties,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.aws.schedule.role-arn}") private val roleArn: String,
    @Value("\${spring.aws.schedule.sqs-arn}") private val targetArn: String,
    @Value("\${spring.aws.schedule.group-name}") private val groupName: String
) {
    val log = KotlinLogging.logger {}

    fun createSchedule(name: String, schedule: LocalDateTime, input: ScheduleInfo): CreateScheduleResponse {
        val json = objectMapper.writeValueAsString(input)
        val target =
            Target
                .builder()
                .arn(targetArn)
                .roleArn(roleArn)
                .input(json)
                .build()

        val request: CreateScheduleRequest =
            CreateScheduleRequest
                .builder()
                .name(name)
                .scheduleExpression(schedule.toScheduleExpression())
                .scheduleExpressionTimezone("Asia/Seoul")
                .groupName(groupName)
                .description("This Schedule is created by ${buildProperties.name} ${buildProperties.version} Application")
                .target(target)
                .actionAfterCompletion(ActionAfterCompletion.NONE)
                .flexibleTimeWindow(
                    FlexibleTimeWindow
                        .builder()
                        .mode(FlexibleTimeWindowMode.OFF)
                        .build()
                ).build()

        try {
            val response = awsSchedulerClient.createSchedule(request)
            log.info { "Successfully created schedule $name in schedule group $groupName The ARN is ${response.scheduleArn()}" }
            return response
        } catch (ex: ConflictException) {
            log.error { "A conflict exception occurred while creating the schedule: $ex.message" }
            throw RuntimeException("A conflict exception occurred while creating the schedule: ${ex.message}", ex)
        } catch (ex: Exception) {
            log.error { "Error creating schedule: ${ex.message}" }
            throw RuntimeException("Error creating schedule: ${ex.message}", ex)
        }
    }

    fun browseSchedule(): List<ScheduleName> {
        return awsSchedulerClient
            .listSchedules(ListSchedulesRequest.builder().build())
            .schedules()
            .map { ScheduleName(it.name()) }
            .toList()
    }

    fun deleteSchedule(scheduleName: ScheduleName) {
        try {
            awsSchedulerClient.deleteSchedule(
                DeleteScheduleRequest
                    .builder()
                    .name(scheduleName.value)
                    .groupName(groupName)
                    .build()
            )
            log.info { "Successfully deleted schedule $scheduleName" }
        } catch (ex: Exception) {
            log.error { "Error deleting schedule: ${ex.message}" }
            throw RuntimeException("Error deleting schedule: ${ex.message}", ex)
        }
    }
}
