package com.manage.crm.user.exception

class JsonException : IllegalArgumentException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    companion object {
        fun notContainKey(key: String): JsonException {
            return JsonException("Attribute does not contain key: $key")
        }

        fun notJsonFormat(attribute: String): JsonException {
            return JsonException("Attribute is not JSON format: $attribute")
        }
    }
}
