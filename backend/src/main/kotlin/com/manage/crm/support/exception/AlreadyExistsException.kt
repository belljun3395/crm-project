package com.manage.crm.support.exception

class AlreadyExistsException : IllegalArgumentException {
    constructor(entityName: String, fieldName: String, fieldValue: Any) :
        super(createMessage(entityName, fieldName, fieldValue))
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    companion object {
        fun createMessage(
            entityName: String,
            fieldName: String,
            fieldValue: Any
        ): String {
            return "$entityName already exists with $fieldName: $fieldValue"
        }
    }
}
