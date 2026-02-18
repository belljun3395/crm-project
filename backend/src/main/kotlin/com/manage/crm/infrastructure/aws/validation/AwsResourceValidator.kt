package com.manage.crm.infrastructure.aws.validation

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest

@Component
@Profile("!test & !openapi")
@ConditionalOnBean(SnsClient::class, SqsClient::class)
class AwsResourceValidator(
    private val snsClient: SnsClient,
    private val sqsClient: SqsClient,
    @Value("\${spring.aws.sns.cache-invalidation-topic-arn:#{null}}")
    private val snsTopicArn: String?
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}

    // SQS Queue names from CacheInvalidationListener
    private val sqsQueueNames = listOf(
        "crm-dr-cache-invalidation-queue-aws",
        "crm-dr-cache-invalidation-queue-gcp"
    )

    override fun run(args: ApplicationArguments?) {
        log.info { "Starting AWS resource validation..." }

        var validationFailed = false

        // Validate SNS Topic
        if (!snsTopicArn.isNullOrBlank()) {
            if (!validateSnsTopic(snsTopicArn)) {
                validationFailed = true
            }
        } else {
            log.warn { "SNS topic ARN is not configured. Skipping SNS validation." }
        }

        // Validate SQS Queues
        sqsQueueNames.forEach { queueName ->
            if (!validateSqsQueue(queueName)) {
                validationFailed = true
            }
        }

        if (validationFailed) {
            log.error { "AWS resource validation failed. Please check the configuration." }
            throw IllegalStateException("AWS resource validation failed")
        } else {
            log.info { "AWS resource validation completed successfully." }
        }
    }

    private fun validateSnsTopic(topicArn: String): Boolean {
        return try {
            val request = GetTopicAttributesRequest.builder()
                .topicArn(topicArn)
                .build()
            snsClient.getTopicAttributes(request)
            log.info { "✓ SNS Topic validated: $topicArn" }
            true
        } catch (e: Exception) {
            log.error(e) { "✗ SNS Topic validation failed: $topicArn" }
            false
        }
    }

    private fun validateSqsQueue(queueName: String): Boolean {
        return try {
            val request = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build()
            val response = sqsClient.getQueueUrl(request)
            log.info { "✓ SQS Queue validated: $queueName (URL: ${response.queueUrl()})" }
            true
        } catch (e: Exception) {
            log.error(e) { "✗ SQS Queue validation failed: $queueName" }
            false
        }
    }
}
