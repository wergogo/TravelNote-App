@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.jointrip

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class JoinTripViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val trips = FakeTripRepository()
    private lateinit var vm: JoinTripViewModel

    @Before
    fun setUp() {
        vm = JoinTripViewModel(trips)
    }

    @Test
    fun `code is upper-cased as the user types`() {
        vm.onCodeChange("abc123")

        assertEquals("ABC123", vm.joinCode)
    }

    @Test
    fun `code is limited to six characters`() {
        vm.onCodeChange("abcdefghi")

        assertEquals("ABCDEF", vm.joinCode)
    }

    @Test
    fun `changing the code clears the previous error`() = runTest {
        vm.joinTrip()
        assertEquals("Enter a 6-character code", vm.errorMessage)

        vm.onCodeChange("A")

        assertNull(vm.errorMessage)
    }

    @Test
    fun `short code shows validation error and does not call repository`() = runTest {
        vm.onCodeChange("ABC")

        assertNull(vm.joinTrip())

        assertEquals("Enter a 6-character code", vm.errorMessage)
        assertEquals(0, trips.joinCalls)
    }

    @Test
    fun `empty code shows validation error and does not call repository`() = runTest {
        assertNull(vm.joinTrip())

        assertEquals("Enter a 6-character code", vm.errorMessage)
        assertEquals(0, trips.joinCalls)
    }

    @Test
    fun `success returns the trip id and joins with the normalised code`() = runTest {
        trips.joinResult = Result.success("trip-42")
        vm.onCodeChange("k7m2qz")

        val id = vm.joinTrip()

        assertEquals("trip-42", id)
        assertEquals("K7M2QZ", trips.lastJoinCode)
        assertNull(vm.errorMessage)
    }

    @Test
    fun `failure surfaces the repository error message and returns null`() = runTest {
        trips.joinResult = Result.failure(Exception("Invalid join code"))
        vm.onCodeChange("ABC123")

        assertNull(vm.joinTrip())

        assertEquals("Invalid join code", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        trips.joinResult = Result.failure(Exception())
        vm.onCodeChange("ABC123")

        vm.joinTrip()

        assertEquals("Failed to join trip", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while joining and false afterwards`() = runTest {
        vm.onCodeChange("ABC123")
        val gate = CompletableDeferred<Unit>()
        trips.gate = gate

        val job = launch { vm.joinTrip() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }
}
