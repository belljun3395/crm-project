package com.manage.crm.email.domain.vo

data class Variables(
    val value: List<Variable> = emptyList()
) : Iterable<Variable> {

    override fun iterator(): Iterator<Variable> = value.iterator()

    fun isEmpty(): Boolean = value.isEmpty()

    fun getDisplayVariables(source: VariableSource? = null): List<String> {
        return getVariables(source).map { it.displayValue() }
    }

    fun getVariables(source: VariableSource? = null): List<Variable> {
        return if (source == null) value else filterBySource(source)
    }

    fun findVariable(key: String): Variable? = value.find { it.key == key }

    fun findVariableDefault(key: String): String? = value.find { it.key == key }?.defaultValue

    fun filterBySource(source: VariableSource): List<Variable> =
        value.filter { it.source == source }
}
