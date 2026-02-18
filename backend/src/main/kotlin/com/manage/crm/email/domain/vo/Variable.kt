package com.manage.crm.email.domain.vo

const val SOURCE_DELIMITER = "."
const val LEGACY_TYPE_DELIMITER = "_"
const val DELIMITER = ":"

abstract class Variable(
    val source: VariableSource,
    val key: String,
    val defaultValue: String? = null
) {
    fun hasDefault(): Boolean = defaultValue != null

    /** New standard key format: `user.email` */
    fun keyWithSource(): String = "${source.value}$SOURCE_DELIMITER$key"

    /** Legacy key format for backward compatibility: `user_email` */
    fun legacyKeyWithType(): String = "${source.value}$LEGACY_TYPE_DELIMITER$key"

    /**
     * Display value in the new standard format.
     * Used for storage and API representation: `user.email` or `user.email:default`
     */
    fun displayValue(): String =
        if (hasDefault()) "${keyWithSource()}$DELIMITER$defaultValue" else keyWithSource()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Variable

        if (source != other.source) return false
        if (key != other.key) return false
        if (defaultValue != other.defaultValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }
}
