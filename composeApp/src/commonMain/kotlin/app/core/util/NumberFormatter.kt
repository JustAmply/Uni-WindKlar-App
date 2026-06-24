package app.core.util

import kotlin.math.round

/**
 * Formats an [Int] value with German thousands separators (dot).
 * Example: 1234567 -> "1.234.567"
 */
fun formatGermanNumber(number: Int): String {
    return formatGermanNumber(number.toLong())
}

/**
 * Formats a [Long] value with German thousands separators (dot).
 * Example: 1234567 -> "1.234.567"
 */
fun formatGermanNumber(number: Long): String {
    if (number < 0) {
        if (number == Long.MIN_VALUE) {
            return "-9.223.372.036.854.775.808"
        }
        return "-" + formatGermanNumber(-number)
    }
    return number.toString().reversed().chunked(3).joinToString(".").reversed()
}

/**
 * Formats a [Double] value with German representation:
 * - Thousands separator: dot (.)
 * - Decimal separator: comma (,)
 * - Rounds to the specified number of [decimals].
 * - If [dropTrailingZero] is true, drops trailing ",0" fractional parts.
 *
 * Example:
 * - formatGermanNumber(12345.67, 1) -> "12.345,7"
 * - formatGermanNumber(12345.0, 1, true) -> "12.345"
 * - formatGermanNumber(12345.0, 1, false) -> "12.345,0"
 */
fun formatGermanNumber(
    value: Double,
    decimals: Int = 1,
    dropTrailingZero: Boolean = true
): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()
    
    val isNegative = value < 0
    val absValue = if (isNegative) -value else value
    
    val roundedValue = absValue.roundTo(decimals)
    val integerPart = roundedValue.toLong()
    val formattedInteger = formatGermanNumber(integerPart)
    
    if (decimals <= 0) {
        return if (isNegative) "-$formattedInteger" else formattedInteger
    }
    
    val fraction = roundedValue - integerPart
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    val fractionInt = round(fraction * multiplier).toLong()
    
    if (dropTrailingZero && fractionInt == 0L) {
        return if (isNegative) "-$formattedInteger" else formattedInteger
    }
    
    val fractionStr = fractionInt.toString().padStart(decimals, '0')
    val result = "$formattedInteger,$fractionStr"
    return if (isNegative) "-$result" else result
}

/**
 * Formats a [Float] value with German representation.
 */
fun formatGermanNumber(
    value: Float,
    decimals: Int = 1,
    dropTrailingZero: Boolean = true
): String {
    return formatGermanNumber(value.toDouble(), decimals, dropTrailingZero)
}

/**
 * Rounds a Double to the specified number of decimal places.
 */
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
