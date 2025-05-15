package com.manage.crm.email.domain.vo

data class EmailTemplateVersion(
    val value: Float = INITIAL_VERSION_AMOUNT
) {
    companion object {
        private const val INITIAL_VERSION_AMOUNT = 1.0f
        private const val DEFAULT_VERSION_PLUS_AMOUNT = 0.1f

        fun isValidUpdateVersion(currentVersion: EmailTemplateVersion, newVersion: Float): Boolean {
            return newVersion > currentVersion.value
        }

        fun calcNewVersion(currentVersion: EmailTemplateVersion, plusAmount: Float = DEFAULT_VERSION_PLUS_AMOUNT): Float {
            return currentVersion.value + plusAmount
        }
    }

    init {
        require(value >= INITIAL_VERSION_AMOUNT) {
            "Version must be greater than or equal to $INITIAL_VERSION_AMOUNT"
        }
    }
}
