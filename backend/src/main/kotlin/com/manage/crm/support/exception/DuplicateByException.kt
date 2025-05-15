package com.manage.crm.support.exception

class DuplicateByException : IllegalArgumentException {
    constructor(entityName: String, fieldName: String, fieldValue: Any) : super(
        createMessage(entityName, fieldName, fieldValue)
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
            return "Duplicate $entityName by $fieldName: $fieldValue"
        }
    }
}
