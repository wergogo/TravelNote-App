@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.home

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    // Standard dispatcher: init coroutines only run when the test advances them.
    @get:Rule val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val trips = FakeTripRepository()

    @Test
    fun `stays loading until the first trips emission arrives`() = runTest {
        val firstEmission = CompletableDeferred<Unit>()
        trips.tripsEmissionGate = firstEmission
        val vm = HomeViewModel(trips)

        advanceUntilIdle()
        assertTrue("still loading, nothing emitted yet", vm.isLoading.value)

        firstEmission.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `exposes the observed trips and follows later emissions`() = runTest {
        val a = Trip(id = "a", name = "Alps")
        val b = Trip(id = "b", name = "Bali")
        trips.tripsSource.value = listOf(a)
        val vm = HomeViewModel(trips)
        advanceUntilIdle()
        assertEquals(listOf(a), vm.trips.value)

        trips.tripsSource.value = listOf(a, b)
        advanceUntilIdle()

        assertEquals(listOf(a, b), vm.trips.value)
    }

    @Test
    fun `backfills join codes exactly once on init`() = runTest {
        HomeViewModel(trips)
        advanceUntilIdle()
        assertEquals(1, trips.ensureJoinCodesCalls)

        trips.tripsSource.value = listOf(Trip(id = "x"))
        advanceUntilIdle()

        assertEquals(1, trips.ensureJoinCodesCalls)
    }

    @Test
    fun `failed join code backfill does not break trip loading`() = runTest {
        trips.ensureJoinCodesResult = Result.failure(Exception("offline"))
        trips.tripsSource.value = listOf(Trip(id = "a"))

        val vm = HomeViewModel(trips)
        advanceUntilIdle()

        assertEquals(1, vm.trips.value.size)
        assertFalse(vm.isLoading.value)
    }
}
