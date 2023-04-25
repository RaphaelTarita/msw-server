package msw.server.core.common.util

import kotlin.math.max
import kotlin.math.min

fun Long.coerceToInt(): Int {
    return max(min(this, Int.MAX_VALUE.toLong()), Int.MIN_VALUE.toLong()).toInt()
}

fun invertInsertionPoint(inverted: Int): Int {
    return -(inverted + 1)
}