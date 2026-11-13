@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.members

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MembersViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val trips = FakeTripRepository()
    private val days = FakeDayRepository()
    private val trip = Trip(id = "t1", startDate = "2025-05-01", endDate = "2025-05-03", ownerId = "me")

    private fun viewModel(): MembersViewModel {
        trips.tripsSource.value = listOf(trip)
        return MembersViewModel(auth, trips, days, "t1")
    }

    @Test
    fun `exposes the trip, its members and the current user`() = runTest {
        auth.currentUserId = "me"
        val members = listOf(Member(userId = "me", role = "owner"), Member(userId = "bob"))
        trips.setMembers("t1", members)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(trip, vm.trip.value)
        assertEquals(members, vm.members.value)
        assertEquals("me", vm.currentUserId)
    }

    @Test
    fun `member spending totals per author across days`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0, authorId = "me"), Expense(amount = 7.0, authorId = "bob")))
        days.setExpenses("t1", "2025-05-02", listOf(Expense(amount = 5.0, authorId = "me")))
        days.setEvents("t1", "2025-05-03", listOf(Event(cost = 30.0, authorId = "bob")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(mapOf("me" to 15.0, "bob" to 37.0), vm.memberSpending.value)
    }

    @Test
    fun `event costs and expenses of the same author on one day are added`() = runTest {
        days.setEvents("t1", "2025-05-01", listOf(Event(cost = 40.0, authorId = "me")))
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 2.5, authorId = "me")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(mapOf("me" to 42.5), vm.memberSpending.value)
    }

    @Test
    fun `free events do not create a spending entry`() = runTest {
        days.setEvents("t1", "2025-05-01", listOf(Event(cost = 0.0, authorId = "bob")))
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 6.0, authorId = "me")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(mapOf("me" to 6.0), vm.memberSpending.value)
    }

    @Test
    fun `spending follows later data emissions`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.memberSpending.value.isEmpty())

        days.setExpenses("t1", "2025-05-02", listOf(Expense(amount = 12.0, authorId = "bob")))
        advanceUntilIdle()

        assertEquals(mapOf("bob" to 12.0), vm.memberSpending.value)
    }

    @Test
    fun `shortening the trip drops spending of the removed day and ignores later changes to it`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0, authorId = "me")))
        days.setExpenses("t1", "2025-05-02", listOf(Expense(amount = 20.0, authorId = "bob")))
        days.setExpenses("t1", "2025-05-03", listOf(Expense(amount = 30.0, authorId = "me")))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(mapOf("me" to 40.0, "bob" to 20.0), vm.memberSpending.value)

        trips.tripsSource.value = listOf(trip.copy(endDate = "2025-05-02"))
        advanceUntilIdle()
        assertEquals(mapOf("me" to 10.0, "bob" to 20.0), vm.memberSpending.value)

        // The old day is no longer observed: new data there must not leak in.
        days.setExpenses("t1", "2025-05-03", listOf(Expense(amount = 999.0, authorId = "bob")))
        advanceUntilIdle()
        assertEquals(mapOf("me" to 10.0, "bob" to 20.0), vm.memberSpending.value)
    }

    @Test
    fun `trip dates becoming invalid reset the spending`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0, authorId = "me")))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(mapOf("me" to 10.0), vm.memberSpending.value)

        trips.tripsSource.value = listOf(trip.copy(startDate = "2025-05-03", endDate = "2025-05-01"))
        advanceUntilIdle()
        assertTrue(vm.memberSpending.value.isEmpty())

        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 55.0, authorId = "me")))
        advanceUntilIdle()
        assertTrue(vm.memberSpending.value.isEmpty())
    }

    @Test
    fun `trip with unparseable dates has no spending`() = runTest {
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0, authorId = "me")))
        trips.tripsSource.value = listOf(trip.copy(startDate = "bad", endDate = "bad"))

        val vm = MembersViewModel(auth, trips, days, "t1")
        advanceUntilIdle()

        assertTrue(vm.memberSpending.value.isEmpty())
    }

    @Test
    fun `shows only the members and spending of this trip`() = runTest {
        val mine = Member(userId = "me")
        trips.setMembers("t1", listOf(mine))
        trips.setMembers("other-trip", listOf(Member(userId = "stranger")))
        days.setExpenses("t1", "2025-05-01", listOf(Expense(amount = 10.0, authorId = "me")))
        days.setExpenses("other-trip", "2025-05-01", listOf(Expense(amount = 500.0, authorId = "stranger")))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(mine), vm.members.value)
        assertEquals(mapOf("me" to 10.0), vm.memberSpending.value)
    }

    @Test
    fun `regenerateJoinCode returns the new code for this trip`() = runTest {
        val vm = viewModel()
        trips.regenerateResult = Result.success("ZZZ999")

        val result = vm.regenerateJoinCode()

        assertEquals("ZZZ999", result.getOrNull())
        assertEquals(listOf("t1"), trips.regeneratedTripIds)
    }

    @Test
    fun `leaveTrip and deleteTrip delegate to the repository for this trip`() = runTest {
        val vm = viewModel()
        trips.deleteResult = Result.failure(Exception("Owner only"))

        assertTrue(vm.leaveTrip().isSuccess)
        assertEquals("Owner only", vm.deleteTrip().exceptionOrNull()?.message)

        assertEquals(listOf("t1"), trips.leftTripIds)
        assertEquals(listOf("t1"), trips.deletedTripIds)
    }

    @Test
    fun `leaveTrip failure is returned to the caller`() = runTest {
        val vm = viewModel()
        trips.leaveResult = Result.failure(Exception("Owners cannot leave"))

        assertEquals("Owners cannot leave", vm.leaveTrip().exceptionOrNull()?.message)
    }
}
