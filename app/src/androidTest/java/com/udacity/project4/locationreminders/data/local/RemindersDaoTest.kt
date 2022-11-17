package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    //    TODO: Add testing implementation to the RemindersDao.kt
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase()
    {
        database.close()
    }

    @Test
    fun saveReminder_getReminderById()
    {
        runBlockingTest {
            //GIVEN a reminder
            val reminder = ReminderDTO("title","description","location",1.0,1.0)
            database.reminderDao().saveReminder(reminder)
            //WHEN getting this reminder by id
            val loadedReminder = database.reminderDao().getReminderById(reminder.id)
            //THEN
            assertThat(loadedReminder?.id, Matchers.`is`(reminder.id))
            assertThat(loadedReminder?.description, Matchers.`is`(reminder.description))
            assertThat(loadedReminder?.title, Matchers.`is`(reminder.title))
            assertThat(loadedReminder?.location, Matchers.`is`(reminder.location))
            assertThat(loadedReminder?.latitude, Matchers.`is`(reminder.latitude))
            assertThat(loadedReminder?.longitude, Matchers.`is`(reminder.longitude))
        }

    }
    @Test
    fun getReminderById_NoReminderWithThisId_ReturnNull()
    {
        runBlockingTest {
            //GIVEN a reminder
            val reminder = ReminderDTO("title","description","location",1.0,1.0)
            database.reminderDao().saveReminder(reminder)
            //WHEN getting a reminder with wrong id
            val loadedReminder = database.reminderDao().getReminderById("WrongId")
            //THEN this reminder is null
            assertThat(loadedReminder, nullValue())
        }

    }
}