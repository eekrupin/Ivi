package ru.ekrupin.ivi.data.reminder

import javax.inject.Inject

class AndroidReminderScheduler @Inject constructor() : ReminderScheduler {
    override fun refreshAll() = Unit
}
