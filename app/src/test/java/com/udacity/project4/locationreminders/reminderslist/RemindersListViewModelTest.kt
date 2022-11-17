package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private val reminder1 = ReminderDTO("title1", "description1", "location1", 1.0, 1.0)
    private val reminder2 = ReminderDTO("title2", "description2", "location2", 2.0, 2.0)

    private var listOfReminders = listOf(reminder1,reminder2).sortedBy { it.id }
    lateinit var fakeDataSource: FakeDataSource
    lateinit var reminderListViewModel : RemindersListViewModel

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
        reminderListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(),fakeDataSource)

    }

    @Test
    fun loadReminders_loadRemindersFromDataSource()
    {
        //GIVEN the fake reminder data source
        //WHEN we call loadReminders()
        reminderListViewModel.loadReminders()
        //THEN
        assertThat(reminderListViewModel.remindersList.getOrAwaitValue(),`is`(fakeDataSource.reminders?.map {
            ReminderDataItem(
                it.title,
                it.description,
                it.location,
                it.latitude,
                it.longitude,
                it.id
            )
        }))
    }

    @Test
    fun loadReminders_ShouldReturnError_snackBarShowingTheErrorMessage()
    {
        //GIVEN the fake datasource that should return error
        fakeDataSource.setShouldReturnError(true)
        //WHEN we call loadReminders()
        reminderListViewModel.loadReminders()
        //THEN snackBar value is updated with the error message
        assertThat(reminderListViewModel.showSnackBar.getOrAwaitValue(),`is`("Error: couldn't retrieve the reminders"))
    }

    @Test
    fun invalidateShowNoData_NoData_ShowNoDataIsTrue()
    {
        mainCoroutineRule.runBlockingTest {
            //GIVEN an empty data source
            fakeDataSource.deleteAllReminders()
            //WHEN we load the reminders
            reminderListViewModel.loadReminders()
            //THEN
            assertThat(reminderListViewModel.showNoData.getOrAwaitValue(),`is`(true))
        }

    }


}