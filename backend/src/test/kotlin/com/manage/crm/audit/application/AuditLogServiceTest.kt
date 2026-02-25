package com.manage.crm.audit.application

import com.manage.crm.audit.application.dto.RecordAuditLogCommand
import com.manage.crm.audit.domain.repository.AuditLogRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException

class AuditLogServiceTest : BehaviorSpec({
    lateinit var auditLogRepository: AuditLogRepository
    lateinit var auditLogService: AuditLogService

    beforeTest {
        auditLogRepository = mockk(relaxed = true)
        auditLogService = AuditLogService(auditLogRepository)
    }

    given("record audit log command") {
        `when`("record is requested") {
            then("persist audit log entry") {
                val command = RecordAuditLogCommand(
                    actorId = "admin-1",
                    action = "WEBHOOK_CREATE",
                    resourceType = "WEBHOOK",
                    resourceId = "101",
                    requestMethod = "POST",
                    requestPath = "/api/v1/webhooks",
                    statusCode = 201,
                    detail = "created webhook"
                )

                coEvery { auditLogRepository.save(any()) } answers { firstArg() }

                auditLogService.record(command)

                coVerify(exactly = 1) { auditLogRepository.save(any()) }
            }
        }

        `when`("repository save throws cancellation") {
            then("rethrow cancellation to preserve coroutine cancellation semantics") {
                val command = RecordAuditLogCommand(
                    actorId = "admin-1",
                    action = "WEBHOOK_CREATE",
                    resourceType = "WEBHOOK",
                    resourceId = "101",
                    requestMethod = "POST",
                    requestPath = "/api/v1/webhooks",
                    statusCode = 201,
                    detail = "created webhook"
                )

                coEvery { auditLogRepository.save(any()) } throws CancellationException("cancel")

                shouldThrow<CancellationException> {
                    auditLogService.record(command)
                }
            }
        }
    }
})
