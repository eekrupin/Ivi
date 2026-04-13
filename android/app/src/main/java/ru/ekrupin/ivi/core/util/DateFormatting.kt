package ru.ekrupin.ivi.core.util

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.toDisplayDate(): String = format(displayFormatter)

fun parseDisplayDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim(), displayFormatter) }.getOrNull()

fun LocalDate.toAgeLabel(today: LocalDate = LocalDate.now()): String {
    if (isAfter(today)) return "Возраст не указан"

    val period = Period.between(this, today)
    val years = period.years
    val months = period.months

    return when {
        years > 0 && months > 0 -> "${years.toYearWord()} ${months.toMonthWord()}"
        years > 0 -> years.toYearWord()
        months > 0 -> months.toMonthWord()
        else -> "Меньше месяца"
    }
}

private fun Int.toYearWord(): String = "$this ${pluralize(this, "год", "года", "лет")}"

private fun Int.toMonthWord(): String = "$this ${pluralize(this, "месяц", "месяца", "месяцев")}"

private fun pluralize(value: Int, one: String, few: String, many: String): String {
    val lastTwo = value % 100
    val last = value % 10
    return when {
        lastTwo in 11..14 -> many
        last == 1 -> one
        last in 2..4 -> few
        else -> many
    }
}
