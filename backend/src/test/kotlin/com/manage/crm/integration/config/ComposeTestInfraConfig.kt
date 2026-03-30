package com.manage.crm.integration.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ComposeServiceConfig(
    val image: String? = null,
    val environment: Map<String, String> = emptyMap(),
    val ports: Map<Int, Int> = emptyMap(),
    val command: List<String> = emptyList()
)

object ComposeTestInfraConfig {
    private const val composeRelativePath = "resources/crm-local-develop-environment/docker-compose.yml"

    private val logger = LoggerFactory.getLogger(ComposeTestInfraConfig::class.java)
    private val objectMapper = ObjectMapper(YAMLFactory())

    private val composeFile: Path by lazy { resolveComposeFile() }
    private val services: Map<String, ComposeServiceConfig> by lazy { loadServices() }

    fun service(name: String): ComposeServiceConfig {
        return services[name] ?: error("Compose service not found: $name")
    }

    fun hostPort(serviceName: String, containerPort: Int): Int {
        return service(serviceName).ports[containerPort]
            ?: error("Port mapping not found for service=$serviceName containerPort=$containerPort")
    }

    private fun resolveComposeFile(): Path {
        var current: Path? = Paths.get(System.getProperty("user.dir")).toAbsolutePath()

        while (current != null) {
            val candidate = current.resolve(composeRelativePath)
            if (Files.exists(candidate)) {
                return candidate
            }
            current = current.parent
        }

        error("Unable to locate $composeRelativePath from ${System.getProperty("user.dir")}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadServices(): Map<String, ComposeServiceConfig> {
        val rawCompose = objectMapper.readValue(composeFile.toFile(), Map::class.java) as Map<String, Any?>
        val rawServices = rawCompose["services"] as? Map<String, Any?> ?: emptyMap()

        logger.info("Loaded test infra compose from {}", composeFile)

        return rawServices.mapValues { (_, rawService) ->
            val service = rawService as? Map<String, Any?> ?: emptyMap()
            ComposeServiceConfig(
                image = service["image"]?.toString(),
                environment = parseEnvironment(service["environment"]),
                ports = parsePorts(service["ports"]),
                command = parseCommand(service["command"])
            )
        }
    }

    private fun parseEnvironment(rawEnvironment: Any?): Map<String, String> {
        return when (rawEnvironment) {
            is Map<*, *> ->
                rawEnvironment.entries
                    .filter { it.key != null && it.value != null }
                    .associate { it.key.toString() to it.value.toString() }

            is List<*> -> rawEnvironment.mapNotNull { entry ->
                entry?.toString()
                    ?.substringBefore('#')
                    ?.trim()
                    ?.takeIf { it.isNotBlank() && '=' in it }
                    ?.let {
                        val separatorIndex = it.indexOf('=')
                        it.substring(0, separatorIndex).trim() to it.substring(separatorIndex + 1).trim()
                    }
            }.toMap()

            else -> emptyMap()
        }
    }

    private fun parsePorts(rawPorts: Any?): Map<Int, Int> {
        return when (rawPorts) {
            is List<*> -> rawPorts.mapNotNull { parsePortMapping(it?.toString()) }.toMap()
            else -> emptyMap()
        }
    }

    private fun parsePortMapping(rawPort: String?): Pair<Int, Int>? {
        val cleaned = rawPort
            ?.substringBefore('#')
            ?.trim()
            ?.trim('"', '\'')
            ?: return null

        val parts = cleaned.split(':')
        if (parts.size < 2) {
            return null
        }

        val hostPart = parts[parts.size - 2].substringAfterLast(':').substringBefore('/')
        val containerPart = parts.last().substringBefore('/')

        val hostPort = hostPart.toIntOrNull() ?: return null
        val containerPort = containerPart.toIntOrNull() ?: return null

        return containerPort to hostPort
    }

    private fun parseCommand(rawCommand: Any?): List<String> {
        return when (rawCommand) {
            is List<*> -> rawCommand.mapNotNull { it?.toString() }
            is String -> listOf(rawCommand)
            else -> emptyList()
        }
    }
}
