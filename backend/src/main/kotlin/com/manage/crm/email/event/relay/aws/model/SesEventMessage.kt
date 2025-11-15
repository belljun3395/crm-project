package com.manage.crm.email.event.relay.aws.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesSnsNotification(
    @JsonProperty("Type")
    val type: String? = null,
    @JsonProperty("MessageId")
    val messageId: String? = null,
    @JsonProperty("TopicArn")
    val topicArn: String? = null,
    @JsonProperty("Subject")
    val subject: String? = null,
    @JsonProperty("Timestamp")
    val timestamp: String? = null,
    @JsonProperty("Message")
    val rawMessage: String,
    @JsonProperty("SigningCertURL")
    val signingCertUrl: String? = null,
    @JsonProperty("SignatureVersion")
    val signatureVersion: String? = null,
    @JsonProperty("Signature")
    val signature: String? = null,
    @JsonProperty("UnsubscribeURL")
    val unsubscribeUrl: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesEventMessage(
    @JsonProperty("eventType")
    val eventType: SesEventType? = null,
    @JsonProperty("notificationType")
    val notificationType: SesEventType? = null,
    @JsonProperty("mail")
    val mail: SesMail,
    @JsonProperty("delivery")
    val delivery: SesDelivery? = null,
    @JsonProperty("open")
    val open: SesOpen? = null,
    @JsonProperty("click")
    val click: SesClick? = null,
    @JsonProperty("bounce")
    val bounce: SesBounce? = null,
    @JsonProperty("complaint")
    val complaint: SesComplaint? = null,
    @JsonProperty("reject")
    val reject: SesReject? = null,
    @JsonProperty("deliveryDelay")
    val deliveryDelay: SesDeliveryDelay? = null,
    @JsonProperty("renderingFailure")
    val renderingFailure: SesRenderingFailure? = null,
    @JsonProperty("send")
    val send: SesSend? = null
) {
    val resolvedEventType: SesEventType?
        get() = eventType ?: notificationType
}

enum class SesEventType {
    SEND,
    DELIVERY,
    OPEN,
    CLICK,
    BOUNCE,
    COMPLAINT,
    REJECT,
    DELIVERY_DELAY,
    RENDERING_FAILURE;

    companion object {
        @JvmStatic
        @JsonCreator
        fun from(value: String?): SesEventType? {
            val normalized = value
                ?.replace("-", "", ignoreCase = true)
                ?.replace("_", "", ignoreCase = true)
                ?.uppercase()
            return when (normalized) {
                "SEND" -> SEND
                "DELIVERY" -> DELIVERY
                "OPEN" -> OPEN
                "CLICK" -> CLICK
                "BOUNCE" -> BOUNCE
                "COMPLAINT" -> COMPLAINT
                "REJECT" -> REJECT
                "DELIVERYDELAY" -> DELIVERY_DELAY
                "RENDERINGFAILURE" -> RENDERING_FAILURE
                else -> null
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesMail(
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("source")
    val source: String? = null,
    @JsonProperty("sourceArn")
    val sourceArn: String? = null,
    @JsonProperty("sendingAccountId")
    val sendingAccountId: String? = null,
    @JsonProperty("messageId")
    val messageId: String,
    @JsonProperty("destination")
    val destination: List<String> = emptyList(),
    @JsonProperty("headersTruncated")
    val headersTruncated: Boolean? = null,
    @JsonProperty("headers")
    val headers: List<SesHeader>? = null,
    @JsonProperty("commonHeaders")
    val commonHeaders: SesCommonHeaders? = null,
    @JsonProperty("tags")
    val tags: Map<String, List<String>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesHeader(
    @JsonProperty("name")
    val name: String? = null,
    @JsonProperty("value")
    val value: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesCommonHeaders(
    @JsonProperty("from")
    val from: List<String>? = null,
    @JsonProperty("to")
    val to: List<String>? = null,
    @JsonProperty("subject")
    val subject: String? = null,
    @JsonProperty("messageId")
    val messageId: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesSend(
    @JsonProperty("timestamp")
    val timestamp: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesDelivery(
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("processingTimeMillis")
    val processingTimeMillis: Long? = null,
    @JsonProperty("recipients")
    val recipients: List<String>? = null,
    @JsonProperty("smtpResponse")
    val smtpResponse: String? = null,
    @JsonProperty("remoteMtaIp")
    val remoteMtaIp: String? = null,
    @JsonProperty("reportingMTA")
    val reportingMta: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesOpen(
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("ipAddress")
    val ipAddress: String? = null,
    @JsonProperty("userAgent")
    val userAgent: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesClick(
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("ipAddress")
    val ipAddress: String? = null,
    @JsonProperty("userAgent")
    val userAgent: String? = null,
    @JsonProperty("link")
    val link: String? = null,
    @JsonProperty("linkTags")
    val linkTags: Map<String, List<String>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesBounce(
    @JsonProperty("bounceType")
    val bounceType: String? = null,
    @JsonProperty("bounceSubType")
    val bounceSubType: String? = null,
    @JsonProperty("bouncedRecipients")
    val bouncedRecipients: List<SesBouncedRecipient>? = null,
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("reportingMTA")
    val reportingMta: String? = null,
    @JsonProperty("remoteMtaIp")
    val remoteMtaIp: String? = null,
    @JsonProperty("feedbackId")
    val feedbackId: String? = null,
    @JsonProperty("smtpResponse")
    val smtpResponse: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesBouncedRecipient(
    @JsonProperty("emailAddress")
    val emailAddress: String? = null,
    @JsonProperty("action")
    val action: String? = null,
    @JsonProperty("status")
    val status: String? = null,
    @JsonProperty("diagnosticCode")
    val diagnosticCode: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesComplaint(
    @JsonProperty("complainedRecipients")
    val complainedRecipients: List<SesComplainedRecipient>? = null,
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("feedbackId")
    val feedbackId: String? = null,
    @JsonProperty("complaintFeedbackType")
    val complaintFeedbackType: String? = null,
    @JsonProperty("arrivalDate")
    val arrivalDate: String? = null,
    @JsonProperty("userAgent")
    val userAgent: String? = null,
    @JsonProperty("abuseReportingEnabled")
    val abuseReportingEnabled: Boolean? = null,
    @JsonProperty("complaintSubType")
    val complaintSubType: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesComplainedRecipient(
    @JsonProperty("emailAddress")
    val emailAddress: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesReject(
    @JsonProperty("reason")
    val reason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesDeliveryDelay(
    @JsonProperty("timestamp")
    val timestamp: String? = null,
    @JsonProperty("delayedRecipients")
    val delayedRecipients: List<SesDelayedRecipient>? = null,
    @JsonProperty("expirationTime")
    val expirationTime: String? = null,
    @JsonProperty("delayType")
    val delayType: String? = null,
    @JsonProperty("diagnosticCode")
    val diagnosticCode: String? = null,
    @JsonProperty("remoteMtaIp")
    val remoteMtaIp: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesRenderingFailure(
    @JsonProperty("templateName")
    val templateName: String? = null,
    @JsonProperty("errorMessage")
    val errorMessage: String? = null,
    @JsonProperty("failedRecipient")
    val failedRecipient: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SesDelayedRecipient(
    @JsonProperty("emailAddress")
    val emailAddress: String? = null,
    @JsonProperty("delayType")
    val delayType: String? = null
)
