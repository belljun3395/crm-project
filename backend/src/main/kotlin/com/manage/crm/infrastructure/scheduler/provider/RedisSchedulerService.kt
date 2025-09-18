package com.manage.crm.infrastructure.scheduler.provider

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service

@Service
class RedisSchedulerService(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val SCHEDULED_TASKS_KEY = "scheduled:tasks"
        private const val TASK_DATA_PREFIX = "scheduled:task:"
    }

    // ================ Task Data Management =================
    fun setTaskData(keyValue: String, data: String) {
        val key = "$TASK_DATA_PREFIX$keyValue"
        redisTemplate.opsForValue().set(key, data)
    }

    fun getTaskData(keyValue: String): String? {
        val key = "$TASK_DATA_PREFIX$keyValue"
        return redisTemplate.opsForValue().get(key) as? String
    }

    fun removeTaskData(keyValue: String) {
        val key = "$TASK_DATA_PREFIX$keyValue"
        redisTemplate.delete(key)
    }

    // ================ Scheduled Tasks Management =================
    fun addTaskToScheduledTasks(key: String, score: Double) {
        redisTemplate.opsForZSet().add(SCHEDULED_TASKS_KEY, key, score)
    }

    fun browseAllTasksInScheduledTasks(): Set<Any> {
        return redisTemplate.opsForZSet().range(SCHEDULED_TASKS_KEY, 0, -1) ?: emptySet()
    }

    fun removeTaskInScheduledTasks(key: String) {
        redisTemplate.opsForZSet().remove(SCHEDULED_TASKS_KEY, key)
    }

    fun browseDueTaskIdsInScheduledTasks(maxTime: Double = System.currentTimeMillis() / 1000.0): Set<Long> {
        val taskIds = redisTemplate.opsForZSet().rangeByScore(SCHEDULED_TASKS_KEY, 0.0, maxTime) ?: emptySet()
        return taskIds.mapNotNull { it.toString().toLongOrNull() }.toSet()
    }

    fun removeTaskIdFromScheduledTasks(key: String): Long {
        return redisTemplate.opsForZSet().remove(SCHEDULED_TASKS_KEY, key) ?: 0
    }

    /**
     * 만료된 스케줄을 원자적으로 조회하고 제거합니다.
     * Lua 스크립트를 사용하여 Race Condition을 방지합니다.
     */
    fun getAndRemoveExpiredSchedules(currentTime: Double): Set<String> {
        val luaScript = """
            local expiredTasks = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            if #expiredTasks > 0 then
                redis.call('ZREM', KEYS[1], unpack(expiredTasks))
            end
            return expiredTasks
        """

        val script = DefaultRedisScript<List<*>>().apply {
            setScriptText(luaScript)
            setResultType(List::class.java)
        }
        
        val keys = listOf(SCHEDULED_TASKS_KEY)
        val args = arrayOf(currentTime.toString())

        val result = redisTemplate.execute(script, keys, *args)

        @Suppress("UNCHECKED_CAST")
        return when (result) {
            is List<*> -> result.mapNotNull { item: Any? -> item?.toString() }.toSet()
            else -> emptySet()
        }
    }
}
