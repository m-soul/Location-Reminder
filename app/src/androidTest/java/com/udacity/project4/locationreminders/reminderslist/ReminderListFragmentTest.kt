package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest(){
    val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)

    @Before
    fun koin() {
        stopKoin()
        val testModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    AndroidFakeDataSource(mutableListOf(reminder)) as ReminderDataSource
                )
            }
        }
        startKoin { modules(testModule) }
    }

    @Test
    fun clickAddReminderFab_navigateToSaveReminderFragment() {
        // GIVEN
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder(
            )
        )
    }

    @Test
    fun reminder_ShowedOnTheUi() {
        //WHEN
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        //THEN
        onView(withId(R.id.remindersRecyclerView)).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withText("title"))
            )
        )


    }
}