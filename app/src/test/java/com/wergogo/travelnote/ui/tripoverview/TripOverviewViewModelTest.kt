@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.tripoverview

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.repository.FakeAuthRepository
import com.wergogo.travelnote.repository.FakeDayRepository
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TripOverviewViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val trips = FakeTripRepository()
    private val days = FakeDayRepository()
    private val trip = Trip(id = "t1", name = "Rome", startDate = "2025-05-01", endDate = "2025-05-03")

    private fun viewModel(): TripOverviewViewModel {
        trips.tripsSource.value = listOf(trip)
        return TripOverviewViewModel(auth, trips, days, "t1")
    }

    @Test
    fun `exposes the trip, its members and the current user`() = runTest {
        auth.currentUserId = "me"
        val members = listOf(Member(userId = "me", nickname = "Me"), Member(userId = "bob", nickname = "Bob"))
        trips.setMembers("t1", members)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(trip, vm.trip.value)
        assertEquals(members, vm.members.value)
        assertEquals("me", vm.currentUserId)
    }

    @Test
    fun `has one summary per day of the trip`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf("2025-05-01", "2025-05-02", "2025-05-03"), vm.daySummaries.value.keys.toList())
        assertEquals(0.0, vm.spent.value, 0.0)
    }

    @Test
    fun `day total is expenses plus event costs`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(id = "x1", amount = 10.0), Expense(id = "x2", amount = 5.5)))
        days.setEvents("t1", "2025-05-01", listOf(Event(id = "e1", time = "10:00", cost = 20.0)))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(35.5, vm.daySummaries.value.getValue("2025-05-01").totalExpense, 0.0)
        assertEquals(0.0, vm.daySummaries.value.getValue("2025-05-02").totalExpense, 0.0)
    }

    @Test
    fun `spent sums expenses and event costs across all days`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0)))
        days.setEvents("t1", "2025-05-01", listOf(Event(time = "09:00", cost = 100.0)))
        days.setExpenses("t1", "2025-05-03", listOf(Expense(amount = 4.0)))
        days.setEvents("t1", "2025-05-02", listOf(Event(time = "09:00", cost = 1.0)))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(115.0, vm.spent.value, 0.0)
    }

    @Test
    fun `day summary events are sorted by time`() = runTest {
        days.setEvents("t1", "2025-05-01",
            listOf(
                Event(id = "late", time = "18:00"),
                Event(id = "early", time = "08:15"),
                Event(id = "noon", time = "12:00")
            )
        )

        val vm = viewModel()
        advanceUntilIdle()

        val ids = vm.daySummaries.value.getValue("2025-05-01").events.map { it.id }
        assertEquals(listOf("early", "noon", "late"), ids)
    }

    @Test
    fun `summaries and spent follow later data emissions`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(0.0, vm.spent.value, 0.0)

        days.setExpenses("t1", "2025-05-02", listOf(Expense(amount = 42.0)))
        advanceUntilIdle()

        assertEquals(42.0, vm.spent.value, 0.0)
        assertEquals(42.0, vm.daySummaries.value.getValue("2025-05-02").totalExpense, 0.0)
    }

    @Test
    fun `shortening the trip drops the removed day and ignores later changes to it`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0)))
        days.setExpenses("t1", "2025-05-02", listOf(Expense(amount = 20.0)))
        days.setExpenses("t1", "2025-05-03", listOf(Expense(amount = 30.0)))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(60.0, vm.spent.value, 0.0)
        assertEquals(3, vm.daySummaries.value.size)

        trips.tripsSource.value = listOf(trip.copy(endDate = "2025-05-02"))
        advanceUntilIdle()

        assertEquals(listOf("2025-05-01", "2025-05-02"), vm.daySummaries.value.keys.toList())
        assertEquals(30.0, vm.spent.value, 0.0)

        // The old day is no longer observed: new data there must not leak in.
        days.setExpenses("t1", "2025-05-03", listOf(Expense(amount = 999.0)))
        advanceUntilIdle()

        assertEquals(listOf("2025-05-01", "2025-05-02"), vm.daySummaries.value.keys.toList())
        assertEquals(30.0, vm.spent.value, 0.0)
    }

    @Test
    fun `extending the trip adds the new days`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        days.setExpenses("t1", "2025-05-05", listOf(Expense(amount = 7.0)))
        trips.tripsSource.value = listOf(trip.copy(endDate = "2025-05-05"))
        advanceUntilIdle()

        assertEquals(5, vm.daySummaries.value.size)
        assertEquals(7.0, vm.spent.value, 0.0)
    }

    @Test
    fun `trip dates becoming invalid reset spent and summaries`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0)))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(10.0, vm.spent.value, 0.0)

        trips.tripsSource.value = listOf(trip.copy(startDate = "2025-05-03", endDate = "2025-05-01"))
        advanceUntilIdle()

        assertEquals(0.0, vm.spent.value, 0.0)
        assertTrue(vm.daySummaries.value.isEmpty())

        // And nothing from the previous range is still being observed.
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 55.0)))
        advanceUntilIdle()
        assertEquals(0.0, vm.spent.value, 0.0)
        assertTrue(vm.daySummaries.value.isEmpty())
    }

    @Test
    fun `trip with unparseable dates has no summaries and nothing spent`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0)))
        trips.tripsSource.value = listOf(trip.copy(startDate = "", endDate = ""))

        val vm = TripOverviewViewModel(auth, trips, days, "t1")
        advanceUntilIdle()

        assertTrue(vm.daySummaries.value.isEmpty())
        assertEquals(0.0, vm.spent.value, 0.0)
    }

    @Test
    fun `shows only the members of this trip`() = runTest {
        val mine = Member(userId = "me", nickname = "Me")
        trips.setMembers("t1", listOf(mine))
        trips.setMembers("other-trip", listOf(Member(userId = "stranger")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(mine), vm.members.value)
    }

    @Test
    fun `counts only the expenses of this trip`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0)))
        days.setExpenses("other-trip", "2025-05-01", listOf(Expense(amount = 500.0)))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(10.0, vm.spent.value, 0.0)
        assertEquals(10.0, vm.daySummaries.value.getValue("2025-05-01").totalExpense, 0.0)
    }

    @Test
    fun `trip stays null when it does not exist`() = runTest {
        val vm = TripOverviewViewModel(auth, trips, days, "missing")
        advanceUntilIdle()

        assertNull(vm.trip.value)
        assertTrue(vm.daySummaries.value.isEmpty())
    }

    @Test
    fun `deleteTrip delegates to the repository for this trip`() = runTest {
        val vm = viewModel()
        trips.deleteResult = Result.failure(Exception("Owner only"))

        val result = vm.deleteTrip()

        assertEquals(listOf("t1"), trips.deletedTripIds)
        assertEquals("Owner only", result.exceptionOrNull()?.message)
    }

    @Test
    fun `leaveTrip delegates to the repository for this trip`() = runTest {
        val vm = viewModel()

        val result = vm.leaveTrip()

        assertEquals(listOf("t1"), trips.leftTripIds)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `leaveTrip failure is returned to the caller`() = runTest {
        val vm = viewModel()
        trips.leaveResult = Result.failure(Exception("Owners cannot leave"))

        assertEquals("Owners cannot leave", vm.leaveTrip().exceptionOrNull()?.message)
    }

    @Test
    fun `unpadded stored time sorts chronologically before later times`() = runTest {
        days.setEvents(
            "t1", "2025-05-01",
            listOf(Event(id = "ten", time = "10:00"), Event(id = "nine-thirty", time = "9:30"))
        )

        val vm = viewModel()
        advanceUntilIdle()

        val ids = vm.daySummaries.value.getValue("2025-05-01").events.map { it.id }
        assertEquals(listOf("nine-thirty", "ten"), ids)
    }
}
