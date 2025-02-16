package com.manage.crm.support.transactional

import org.springframework.stereotype.Component
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.support.DefaultTransactionDefinition

@Component
class TransactionTemplates(reactiveTransactionManager: ReactiveTransactionManager) {
    val writer = TransactionalOperator.create(reactiveTransactionManager)
    val newTxWriter = TransactionalOperator.create(
        reactiveTransactionManager,
        DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
    )
}
