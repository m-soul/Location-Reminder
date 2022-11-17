package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.getOrAwaitValue

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.*
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest
{
    private val reminder1 = ReminderDTO("title1", "description1", "location1", 1.0, 1.0)
    private val reminder2 = ReminderDTO("title2", "description2", "location2", 2.0, 2.0)

    private var listOfReminders = listOf(reminder1,reminder2).sortedBy { it.id }
    lateinit var fakeDataSource: FakeDataSource
    lateinit var saveReminderViewModel : SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun closeKoin()
    {
        stopKoin()
    }

    @Before
    fun initializeViewModel()
    {
        fakeDataSource = FakeDataSource(listOfReminders.toMutableList())
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),fakeDataSource)

    }

    @Test
    fun validateEnteredData_validData_returnTrue()
    {
        //GIVEN a reminder item
        val reminder = ReminderDataItem("title","description","location",1.0,1.0)
        //WHEN
       val validDataItem = saveReminderViewModel.validateEnteredData(reminder)
        //THEN
        assertThat(validDataItem, `is` (true))
    }
    @Test
    fun savingReminder_setsShowLoading() {
        mainCoroutineRule.runBlockingTest {
            //GIVEN a reminder item
            val reminder = ReminderDataItem("title", "description", "location", 1.0, 1.0)
            //pause dispatcher
            mainCoroutineRule.pauseDispatcher()
            //WHEN saving a reminder
            saveReminderViewModel.saveReminder(reminder)
            // assert that showLoading is true
            assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))
            //resume dispatcher
            mainCoroutineRule.resumeDispatcher()
            // assert that showLoading is false
            assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))

        }
    }

        @Test
        fun saveReminder_ReminderSaved() {
            mainCoroutineRule.runBlockingTest {
                //GIVEN a reminder item
                val reminder = ReminderDataItem("title", "description", "location", 1.0, 1.0)
                //WHEN saving a reminder
                saveReminderViewModel.saveReminder(reminder)
                //THEN
                val savedReminder = fakeDataSource.reminders.last()
                assertThat(savedReminder.id, `is`(reminder.id))
                assertThat(savedReminder.description, `is`(reminder.description))
                assertThat(savedReminder.title, `is`(reminder.title))
                assertThat(savedReminder.location, `is`(reminder.location))
                assertThat(savedReminder.latitude, `is`(reminder.latitude))
                assertThat(savedReminder.longitude, `is`(reminder.longitude))
            }
        }

    @Test
    fun validateEnteredData_DataWithNoTitle_ShowCorrectSnackBarMessage()
    {
        //GIVEN a reminder item with null title
        val reminder = ReminderDataItem(null, "description", "location", 1.0, 1.0)
        //WHEN validate this item
        saveReminderViewModel.validateEnteredData(reminder)
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun getReminder_ShouldReturnError() {
        mainCoroutineRule.runBlockingTest {
            //GIVEN a reminder item
            val reminder = ReminderDataItem("title", "description", "location", 1.0, 1.0)
            fakeDataSource.setShouldReturnError(true)
            //WHEN getting a reminder
            val result = saveReminderViewModel.dataSource.getReminder(reminder.id)
            //THEN
            assertThat(result, `is`(Result.Error("Error: couldn't retrieve the reminders")))
        }
    }


}