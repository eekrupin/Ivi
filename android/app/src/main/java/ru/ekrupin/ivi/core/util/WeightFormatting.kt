package ru.ekrupin.ivi.core.util

import java.util.Locale

fun Int.toWeightLabel(): String = String.format(Locale.US, "%.1f кг", this / 1000.0)

fun parseWeightInputToGrams(value: String): Int? {
    val normalized = value.trim().replace(',', '.')
    val kilograms = normalized.toDoubleOrNull() ?: return null
    if (kilograms <= 0.0) return null
    return (kilograms * 1000).toInt()
}
