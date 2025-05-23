package com.manage.crm.config.web.compression

class IllegalCompressionRequestException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class IllegalCompressionResponseException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class CompressionException
