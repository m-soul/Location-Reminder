package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.AndroidMainCoroutineRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository

//    @get:Rule
//    var mainCoroutineRule = AndroidMainCoroutineRule()


    @Before
    fun createDatabaseAndRepository() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).setTransactionExecutor(Executors.newSingleThreadExecutor()).allowMainThreadQueries()
            .build()
        remindersLocalRepository =
            RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDatabase()
    {
      database.close()
    }

    @Test
    fun saveReminder_getReminderById()
    {
        runTest {
            //GIVEN saving a reminder
            val reminder = ReminderDTO("title","description","location",1.0,1.0)
            remindersLocalRepository.saveReminder(reminder)
            //WHEN getting this reminder by id
            val loadedReminder = remindersLocalRepository.getReminder(reminder.id)
            //THEN
            assertThat(loadedReminder, `is`(Result.Success(reminder)))
        }
    }

    @Test
    fun getReminderById_NoReminderWithThisId_ReturnError()
    {
        runTest {
            //GIVEN saving a reminder
            val reminder = ReminderDTO("title","description","location",1.0,1.0)
            remindersLocalRepository.saveReminder(reminder)
            //WHEN getting this reminder with wrong id
            val loadedReminder = remindersLocalRepository.getReminder("Wrong id")
            //THEN
            assertThat(loadedReminder, `is`(Result.Error("Reminder not found!")))
        }
    }


}