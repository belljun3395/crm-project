package com.manage.crm.support.exception

class NotFoundByIdException : IllegalArgumentException {
    constructor(entityName: String, id: Long) : super(createMessage(entityName, id))
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    companion object {
        fun createMessage(
            entityName: String,
            id: Long
        ): String {
            return "$entityName not found by id: $id"
        }
    }
}
