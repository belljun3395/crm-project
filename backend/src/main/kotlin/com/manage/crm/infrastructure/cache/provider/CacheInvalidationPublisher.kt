package com.manage.crm.infrastructure.cache.provider

interface CacheInvalidationPublisher {
    fun publishCacheInvalidation(keys: List<String>)
}
