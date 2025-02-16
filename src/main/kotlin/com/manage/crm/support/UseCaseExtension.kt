package com.manage.crm.support

/**
 * Use this Extension when you want to return a value after logic execution.
 *
 * In this Scope, Any Logic can be executed. **Only map the result to the return type.**
 */
fun <T, R> T.out(block: T.() -> R): R {
    return run(block)
}

class UseCaseExtension
