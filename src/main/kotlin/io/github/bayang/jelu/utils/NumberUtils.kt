package io.github.bayang.jelu.utils

import java.math.BigDecimal

fun centsToDouble(cents: Long?): Double? {
    if (cents == null) {
        return null
    }
    return BigDecimal(cents).movePointLeft(2).toDouble()
}
