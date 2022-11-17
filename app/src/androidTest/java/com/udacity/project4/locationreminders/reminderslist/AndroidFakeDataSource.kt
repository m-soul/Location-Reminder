package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class AndroidFakeDataSource(var reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {
    private var shouldReturnError = false
    fun setShouldReturnError(value: Boolean)
    {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError)
        {
            return Result.Error("Error: couldn't retrieve the reminders")
        }
        return Result.Success(ArrayList(reminders))
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError)
        {
            return Result.Error("Error: couldn't retrieve the reminders")
        }
        val reminder = reminders.find {
            id == it.id
        }
        return if (reminder != null) {
            Result.Success(reminder)
        } else {
            Result.Error("Reminder not found!")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}