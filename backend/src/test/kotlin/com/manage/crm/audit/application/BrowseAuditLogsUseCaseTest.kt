package com.manage.crm.audit.application

import com.manage.crm.audit.application.dto.BrowseAuditLogsUseCaseIn
import com.manage.crm.audit.domain.AuditLog
import com.manage.crm.audit.domain.repository.AuditLogRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

class BrowseAuditLogsUseCaseTest :
    BehaviorSpec({
        lateinit var auditLogRepository: AuditLogRepository
        lateinit var useCase: BrowseAuditLogsUseCase

        beforeTest {
            auditLogRepository = mockk()
            useCase = BrowseAuditLogsUseCase(auditLogRepository)
        }

        given("browse audit logs") {
            `when`("no filter is provided") {
                then("return logs with limit applied") {
                    val latest =
                        AuditLog
                            .new(
                                actorId = "admin-1",
                                action = "WEBHOOK_UPDATE",
                                resourceType = "WEBHOOK",
                                resourceId = "10",
                                requestMethod = "PUT",
                                requestPath = "/api/v1/webhooks/10",
                                statusCode = 200,
                                detail = "updated",
                            ).apply {
                                id = 2L
                                createdAt = LocalDateTime.of(2024, 1, 2, 10, 0)
                            }
                    val older =
                        AuditLog
                            .new(
                                actorId = "admin-2",
                                action = "WEBHOOK_CREATE",
                                resourceType = "WEBHOOK",
                                resourceId = "11",
                                requestMethod = "POST",
                                requestPath = "/api/v1/webhooks",
                                statusCode = 201,
                                detail = "created",
                            ).apply {
                                id = 1L
                                createdAt = LocalDateTime.of(2024, 1, 1, 10, 0)
                            }

                    every { auditLogRepository.findAllByOrderByCreatedAtDesc() } returns flowOf(latest, older)

                    val result = useCase.execute(BrowseAuditLogsUseCaseIn(limit = 1))

                    result.logs.size shouldBe 1
                    result.logs[0].id shouldBe 2L
                    result.logs[0].action shouldBe "WEBHOOK_UPDATE"
                }
            }

            `when`("action filter is provided") {
                then("query action-specific flow and return matched logs") {
                    val actionLog =
                        AuditLog
                            .new(
                                actorId = "admin-3",
                                action = "WEBHOOK_DELETE",
                                resourceType = "WEBHOOK",
                                resourceId = "12",
                                requestMethod = "DELETE",
                                requestPath = "/api/v1/webhooks/12",
                                statusCode = 204,
                                detail = "deleted",
                            ).apply {
                                id = 3L
                                createdAt = LocalDateTime.of(2024, 1, 3, 10, 0)
                            }

                    every { auditLogRepository.findByActionOrderByCreatedAtDesc("WEBHOOK_DELETE") } returns flowOf(actionLog)

                    val result =
                        useCase.execute(
                            BrowseAuditLogsUseCaseIn(
                                limit = 50,
                                action = "WEBHOOK_DELETE",
                            ),
                        )

                    result.logs.size shouldBe 1
                    result.logs[0].id shouldBe 3L
                    result.logs[0].action shouldBe "WEBHOOK_DELETE"
                }
            }

            `when`("multiple filters are provided") {
                then("query combined filter flow without falling back to full scan") {
                    val combinedLog =
                        AuditLog
                            .new(
                                actorId = "admin-4",
                                action = "WEBHOOK_UPDATE",
                                resourceType = "WEBHOOK",
                                resourceId = "42",
                                requestMethod = "PUT",
                                requestPath = "/api/v1/webhooks/42",
                                statusCode = 200,
                                detail = "updated",
                            ).apply {
                                id = 4L
                                createdAt = LocalDateTime.of(2024, 1, 4, 10, 0)
                            }

                    every {
                        auditLogRepository.findByActionAndResourceTypeOrderByCreatedAtDesc(
                            action = "WEBHOOK_UPDATE",
                            resourceType = "WEBHOOK",
                        )
                    } returns flowOf(combinedLog)

                    val result =
                        useCase.execute(
                            BrowseAuditLogsUseCaseIn(
                                limit = 50,
                                action = "WEBHOOK_UPDATE",
                                resourceType = "WEBHOOK",
                            ),
                        )

                    result.logs.size shouldBe 1
                    result.logs[0].id shouldBe 4L
                    result.logs[0].resourceType shouldBe "WEBHOOK"

                    verify(exactly = 0) { auditLogRepository.findAllByOrderByCreatedAtDesc() }
                }
            }
        }
    })
