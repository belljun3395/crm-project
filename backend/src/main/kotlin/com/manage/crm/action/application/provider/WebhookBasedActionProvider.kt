package com.manage.crm.action.application.provider

import com.manage.crm.action.application.ActionDispatchOut
import com.manage.crm.action.application.ActionDispatchStatus
import io.netty.channel.ChannelOption
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import java.net.URI
import java.time.Duration

abstract class WebhookBasedActionProvider(
    webClientBuilder: WebClient.Builder
) : ActionProvider {
    private val webClient = webClientBuilder
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(10))
            )
        )
        .build()

    protected suspend fun postJson(
        destination: String,
        payload: Map<String, Any?>
    ): ActionDispatchOut {
        if (!isUrlSafe(destination)) {
            return ActionDispatchOut(
                status = ActionDispatchStatus.FAILED,
                channel = channel,
                destination = destination,
                errorCode = "UNSAFE_URL",
                errorMessage = "Destination URL is not allowed"
            )
        }

        return runCatching {
            webClient.post()
                .uri(destination)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .awaitBodyOrNull()

            ActionDispatchOut(
                status = ActionDispatchStatus.SUCCESS,
                channel = channel,
                destination = destination
            )
        }.getOrElse { error ->
            ActionDispatchOut(
                status = ActionDispatchStatus.FAILED,
                channel = channel,
                destination = destination,
                errorCode = "WEBHOOK_SEND_FAILED",
                errorMessage = error.message
            )
        }
    }

    private suspend fun WebClient.ResponseSpec.awaitBodyOrNull(): String? {
        return bodyToMono(String::class.java).awaitSingleOrNull()
    }

    private fun isUrlSafe(url: String): Boolean {
        return runCatching {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return@runCatching false
            !host.startsWith("localhost") &&
                !host.startsWith("127.") &&
                !host.startsWith("10.") &&
                !host.startsWith("192.168.") &&
                !host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
        }.getOrDefault(false)
    }
}
