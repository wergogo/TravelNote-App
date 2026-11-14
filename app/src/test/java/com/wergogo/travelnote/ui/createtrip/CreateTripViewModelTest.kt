@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.createtrip

import com.wergogo.travelnote.DefaultLocaleRule
import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class CreateTripViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val localeRule = DefaultLocaleRule(Locale.US)

    private val trips = FakeTripRepository()
    private lateinit var vm: CreateTripViewModel

    @Before
    fun setUp() {
        vm = CreateTripViewModel(trips)
    }

    private fun fillValidForm() {
        vm.name = "  Japan 2025 "
        vm.destination = " Tokyo "
        vm.startDate = "2025-04-01"
        vm.endDate = "2025-04-10"
        vm.totalBudget = "1500"
        vm.currency = "JPY"
        vm.visibility = TripVisibility.LOCKED
        vm.coverId = "cover_beach"
    }

    @Test
    fun `blank name shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.name = "   "

        assertNull(vm.createTrip())

        assertEquals("Name is required", vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }

    @Test
    fun `missing start date shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.startDate = ""

        assertNull(vm.createTrip())

        assertEquals("Dates are required", vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }

    @Test
    fun `missing end date shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.endDate = ""

        assertNull(vm.createTrip())

        assertEquals("Dates are required", vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }

    @Test
    fun `end date before start date shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.startDate = "2025-04-10"
        vm.endDate = "2025-04-01"

        assertNull(vm.createTrip())

        assertEquals("End date cannot be before start date", vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }

    @Test
    fun `malformed date shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.startDate = "04/01/2025"

        assertNull(vm.createTrip())

        assertEquals("Invalid date format", vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }

    @Test
    fun `single day trip is allowed`() = runTest {
        fillValidForm()
        vm.startDate = "2025-04-01"
        vm.endDate = "2025-04-01"

        assertEquals("trip-1", vm.createTrip())
    }

    @Test
    fun `success returns the new trip id and passes a trimmed draft`() = runTest {
        fillValidForm()

        val id = vm.createTrip()

        assertEquals("trip-1", id)
        assertNull(vm.errorMessage)
        assertEquals(
            TripDraft(
                name = "Japan 2025",
                destination = "Tokyo",
                startDate = "2025-04-01",
                endDate = "2025-04-10",
                totalBudget = 1500.0,
                currency = "JPY",
                visibility = TripVisibility.LOCKED,
                coverId = "cover_beach"
            ),
            trips.lastCreatedDraft
        )
    }

    @Test
    fun `blank budget is saved as zero`() = runTest {
        fillValidForm()
        vm.totalBudget = ""

        assertNotNull(vm.createTrip())

        assertEquals(0.0, trips.lastCreatedDraft!!.totalBudget, 0.0)
    }

    @Test
    fun `decimal budget with a dot is parsed`() = runTest {
        fillValidForm()
        vm.totalBudget = "99.5"

        vm.createTrip()

        assertEquals(99.5, trips.lastCreatedDraft!!.totalBudget, 0.0)
    }

    @Test
    fun `failure surfaces the repository error message and returns null`() = runTest {
        fillValidForm()
        trips.createResult = Result.failure(Exception("Network down"))

        assertNull(vm.createTrip())

        assertEquals("Network down", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        fillValidForm()
        trips.createResult = Result.failure(Exception())

        vm.createTrip()

        assertEquals("Failed to create trip", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while creating and false afterwards`() = runTest {
        fillValidForm()
        val gate = CompletableDeferred<Unit>()
        trips.gate = gate

        val job = launch { vm.createTrip() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `comma followed by one or two digits is the decimal separator`() = runTest {
        for ((input, expected) in listOf("12,50" to 12.5, "7,5" to 7.5)) {
            fillValidForm()
            vm.totalBudget = input

            assertNotNull("'$input' should be accepted", vm.createTrip())

            assertEquals("'$input'", expected, trips.lastCreatedDraft!!.totalBudget, 0.0)
        }
    }

    @Test
    fun `other commas are thousands separators`() = runTest {
        for ((input, expected) in listOf("1,500" to 1500.0, "1,500.25" to 1500.25)) {
            fillValidForm()
            vm.totalBudget = input

            assertNotNull("'$input' should be accepted", vm.createTrip())

            assertEquals("'$input'", expected, trips.lastCreatedDraft!!.totalBudget, 0.0)
        }
    }

    @Test
    fun `parsing rule does not depend on the default locale`() = runTest {
        Locale.setDefault(Locale.GERMANY) // restored by localeRule
        for ((input, expected) in listOf("12,50" to 12.5, "7,5" to 7.5, "1,500" to 1500.0, "1,500.25" to 1500.25)) {
            fillValidForm()
            vm.totalBudget = input

            assertNotNull("'$input' should be accepted", vm.createTrip())

            assertEquals("'$input'", expected, trips.lastCreatedDraft!!.totalBudget, 0.0)
        }
    }

    @Test
    fun `unparseable budget shows validation error and does not save zero`() = runTest {
        fillValidForm()
        vm.totalBudget = "abc"

        assertNull(vm.createTrip())

        assertNotNull(vm.errorMessage)
        assertEquals(0, trips.createCalls)
    }
}
