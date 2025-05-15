package com.manage.crm.email.exception

class VariablesNotMatchException : IllegalArgumentException {
    constructor(sourceVariables: List<String>, targetVariables: List<String>) : super(
        createMessage(sourceVariables, targetVariables)
    )
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    companion object {
        fun createMessage(
            sourceVariables: List<String>,
            targetVariables: List<String>
        ): String {
            return "Variables do not match: \n$sourceVariables != $targetVariables"
        }
    }
}
