package ru.ekrupin.ivi.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.toDisplayDate(): String = format(displayFormatter)

fun parseDisplayDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), displayFormatter) }.getOrNull()
