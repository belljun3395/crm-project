package com.manage.crm.support.transactional

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionSynchronization
import org.springframework.transaction.reactive.TransactionSynchronizationManager
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext

@Service
class TransactionSynchronizationTemplate {
    val log = KotlinLogging.logger {}

    /**
     * Invoked after transaction commit/rollback. Can perform resource cleanup after transaction completion.
     */
    suspend fun afterCompletion(
        context: CoroutineContext = Dispatchers.IO,
        blockDescription: String,
        block: suspend () -> Unit
    ) {
        TransactionSynchronizationManager.forCurrentTransaction().map { manager ->
            manager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCompletion(status: Int): Mono<Void> {
                    return mono(context) {
                        log.debug { "do after completion: $blockDescription" }
                        block()
                        return@mono null
                    }
                }
            })
        }.awaitSingle()
    }

    /**
     * Invoked after transaction commit. Can perform further operations right after the main transaction has successfully committed.
     */
    suspend fun afterCommit(
        context: CoroutineContext = Dispatchers.IO,
        blockDescription: String,
        block: suspend () -> Unit
    ) {
        TransactionSynchronizationManager.forCurrentTransaction().map { manager ->
            manager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit(): Mono<Void> {
                    return mono(context) {
                        log.debug { "do after commit: $blockDescription" }
                        block()
                        return@mono null
                    }
                }
            })
        }.awaitSingle()
    }
}
