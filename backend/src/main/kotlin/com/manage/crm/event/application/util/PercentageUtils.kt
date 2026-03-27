package com.manage.crm.event.application.util

import kotlin.math.roundToInt

fun toPercentage(numerator: Int, denominator: Int): Double {
    if (denominator <= 0) {
        return 0.0
    }
    return ((numerator.toDouble() / denominator.toDouble()) * 10000.0).roundToInt() / 100.0
}
