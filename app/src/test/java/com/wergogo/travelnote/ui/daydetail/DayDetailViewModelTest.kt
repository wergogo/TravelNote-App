@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.daydetail

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.repository.FakeDayRepository
import com.wergogo.travelnote.repository.FakeDayRepository.Target
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DayDetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val trips = FakeTripRepository()
    private val days = FakeDayRepository()
    private val date = "2025-05-02"

    private fun viewModel() = DayDetailViewModel(trips, days, "t1", date)

    @Test
    fun `exposes trip, day, events, expenses and notes for the date`() = runTest {
        val trip = Trip(id = "t1", name = "Rome")
        val day = Day(date = date, stayName = "Hotel Roma", stayCost = 120.0)
        val events = listOf(Event(id = "e1", title = "Colosseum"))
        val expenses = listOf(Expense(id = "x1", amount = 8.0))
        val notes = listOf(Note(id = "n1", text = "Bring tickets"))
        trips.tripsSource.value = listOf(trip)
        days.setDay("t1", date, day)
        days.setEvents("t1", date, events)
        days.setExpenses("t1", date, expenses)
        days.setNotes("t1", date, notes)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(trip, vm.trip.value)
        assertEquals(day, vm.day.value)
        assertEquals(events, vm.events.value)
        assertEquals(expenses, vm.expenses.value)
        assertEquals(notes, vm.notes.value)
    }

    @Test
    fun `events are listed by time of day, not in the order they were added`() = runTest {
        days.setEvents(
            "t1", date,
            listOf(
                Event(id = "evening", time = "18:11", createdAt = 1),
                Event(id = "night", time = "21:22", createdAt = 2),
                Event(id = "morning", time = "9:03", createdAt = 3)   // saved before times were padded
            )
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("morning", "evening", "night"), vm.events.value.map { it.id })
    }

    @Test
    fun `trip stays null when it does not exist`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertNull(vm.trip.value)
    }

    @Test
    fun `state follows later emissions`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        days.setEvents("t1", date, listOf(Event(id = "e9")))
        days.setNotes("t1", date, listOf(Note(id = "n9")))
        advanceUntilIdle()

        assertEquals(listOf("e9"), vm.events.value.map { it.id })
        assertEquals(listOf("n9"), vm.notes.value.map { it.id })
    }

    @Test
    fun `shows only the data of the requested date`() = runTest {
        days.setEvents("t1", date, listOf(Event(id = "mine")))
        days.setEvents("t1", "2025-05-03", listOf(Event(id = "other-day")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("mine"), vm.events.value.map { it.id })
    }

    @Test
    fun `shows only the data of this trip`() = runTest {
        days.setEvents("t1", date, listOf(Event(id = "mine")))
        days.setEvents("other-trip", date, listOf(Event(id = "other-trip-event")))
        days.setExpenses("t1", date, listOf(Expense(id = "my-expense")))
        days.setExpenses("other-trip", date, listOf(Expense(id = "other-trip-expense")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("mine"), vm.events.value.map { it.id })
        assertEquals(listOf("my-expense"), vm.expenses.value.map { it.id })
    }

    @Test
    fun `deleteEvent targets this trip, date and event id`() = runTest {
        val vm = viewModel()

        val result = vm.deleteEvent("e1")

        assertTrue(result.isSuccess)
        assertEquals(listOf(Target("t1", date, "e1")), days.deletedEvents)
    }

    @Test
    fun `deleteExpense targets this trip, date and expense id`() = runTest {
        val vm = viewModel()

        val result = vm.deleteExpense("x1")

        assertTrue(result.isSuccess)
        assertEquals(listOf(Target("t1", date, "x1")), days.deletedExpenses)
    }

    @Test
    fun `deleteNote targets this trip, date and note id`() = runTest {
        val vm = viewModel()

        val result = vm.deleteNote("n1")

        assertTrue(result.isSuccess)
        assertEquals(listOf(Target("t1", date, "n1")), days.deletedNotes)
    }

    @Test
    fun `delete failures are returned to the caller`() = runTest {
        days.deleteEventResult = Result.failure(Exception("Not allowed: event"))
        days.deleteExpenseResult = Result.failure(Exception("Not allowed: expense"))
        days.deleteNoteResult = Result.failure(Exception("Not allowed: note"))
        val vm = viewModel()

        assertEquals("Not allowed: event", vm.deleteEvent("e1").exceptionOrNull()?.message)
        assertEquals("Not allowed: expense", vm.deleteExpense("x1").exceptionOrNull()?.message)
        assertEquals("Not allowed: note", vm.deleteNote("n1").exceptionOrNull()?.message)
    }
}
