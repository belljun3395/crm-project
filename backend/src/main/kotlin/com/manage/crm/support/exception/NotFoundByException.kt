package com.manage.crm.support.exception

class NotFoundByException : IllegalArgumentException {
    constructor(entityName: String, fieldName: String, fieldValue: Any) : super(
        createMessage(entityName, fieldName, fieldValue)
    )
    constructor(entityName: String, fieldName1: String, fieldValue1: Any, fieldName2: String, fieldValue2: Any) : super(
        createMessage(entityName, fieldName1, fieldValue1, fieldName2, fieldValue2)
    )
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    companion object {
        fun createMessage(
            entityName: String,
            fieldName: String,
            fieldValue: Any
        ): String {
            return "$entityName not found by $fieldName: $fieldValue"
        }

        fun createMessage(
            entityName: String,
            fieldName1: String,
            fieldValue1: Any,
            fieldName2: String,
            fieldValue2: Any
        ): String {
            return "$entityName not found by $fieldName1 and $fieldName2: $fieldValue1, $fieldValue2"
        }
    }
}
