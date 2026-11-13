@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.edittrip

import com.wergogo.travelnote.B7_RULE
import com.wergogo.travelnote.DefaultLocaleRule
import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.repository.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class EditTripViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val localeRule = DefaultLocaleRule(Locale.US)

    private val trips = FakeTripRepository()
    private val existing = Trip(
        id = "t1",
        name = "Iceland",
        destination = "Reykjavik",
        startDate = "2025-06-01",
        endDate = "2025-06-08",
        totalBudget = 2000.0,
        currency = "EUR",
        visibility = TripVisibility.LOCKED,
        coverId = "cover_beach"
    )

    private fun viewModel(): EditTripViewModel {
        trips.tripsSource.value = listOf(existing)
        return EditTripViewModel(trips, "t1")
    }

    @Test
    fun `pre-loads the existing trip into the form`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals("Iceland", vm.name)
        assertEquals("Reykjavik", vm.destination)
        assertEquals("2025-06-01", vm.startDate)
        assertEquals("2025-06-08", vm.endDate)
        assertEquals("2000.0", vm.totalBudget)
        assertEquals("EUR", vm.currency)
        assertEquals(TripVisibility.LOCKED, vm.visibility)
        assertEquals("cover_beach", vm.coverId)
    }

    @Test
    fun `form keeps its defaults when the trip does not exist`() = runTest {
        trips.tripsSource.value = emptyList()

        val vm = EditTripViewModel(trips, "missing")
        advanceUntilIdle()

        assertEquals("", vm.name)
        assertEquals("USD", vm.currency)
    }

    @Test
    fun `saving unchanged form sends the original values for that trip id`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val ok = vm.saveChanges()

        assertTrue(ok)
        assertEquals(
            "t1" to TripDraft(
                "Iceland", "Reykjavik", "2025-06-01", "2025-06-08", 2000.0, "EUR",
                TripVisibility.LOCKED, "cover_beach"
            ),
            trips.lastUpdate
        )
    }

    @Test
    fun `edited fields are trimmed and sent`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.name = "  Iceland Ring Road "
        vm.destination = " Akureyri "
        vm.totalBudget = "2500"

        vm.saveChanges()

        val draft = trips.lastUpdate!!.second
        assertEquals("Iceland Ring Road", draft.name)
        assertEquals("Akureyri", draft.destination)
        assertEquals(2500.0, draft.totalBudget, 0.0)
    }

    @Test
    fun `blank name shows validation error and does not call repository`() = runTest {
        val vm = viewModel()
        vm.name = " "

        assertFalse(vm.saveChanges())

        assertEquals("Name is required", vm.errorMessage)
        assertEquals(0, trips.updateCalls)
    }

    @Test
    fun `missing dates show validation error and do not call repository`() = runTest {
        val vm = viewModel()
        vm.endDate = ""

        assertFalse(vm.saveChanges())

        assertEquals("Dates are required", vm.errorMessage)
        assertEquals(0, trips.updateCalls)
    }

    @Test
    fun `failure surfaces the repository error message`() = runTest {
        val vm = viewModel()
        trips.updateResult = Result.failure(Exception("Only the owner can edit"))

        assertFalse(vm.saveChanges())

        assertEquals("Only the owner can edit", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        val vm = viewModel()
        trips.updateResult = Result.failure(Exception())

        vm.saveChanges()

        assertEquals("Failed to save", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while saving and false afterwards`() = runTest {
        val vm = viewModel()
        val gate = CompletableDeferred<Unit>()
        trips.gate = gate

        val job = launch { vm.saveChanges() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Ignore("Bug B8: EditTripViewModel accepts an end date before the start date (CreateTripViewModel rejects it)")
    @Test
    fun `end date before start date shows validation error and does not call repository`() = runTest {
        val vm = viewModel()
        vm.startDate = "2025-06-10"
        vm.endDate = "2025-06-01"

        assertFalse(vm.saveChanges())

        assertNotNull(vm.errorMessage)
        assertEquals(0, trips.updateCalls)
    }

    @Ignore(B7_RULE)
    @Test
    fun `comma followed by one or two digits is the decimal separator`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        for ((input, expected) in listOf("12,50" to 12.5, "7,5" to 7.5)) {
            vm.totalBudget = input

            assertTrue("'$input' should be accepted", vm.saveChanges())

            assertEquals("'$input'", expected, trips.lastUpdate!!.second.totalBudget, 0.0)
        }
    }

    @Ignore(B7_RULE)
    @Test
    fun `other commas are thousands separators`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        for ((input, expected) in listOf("1,500" to 1500.0, "1,500.25" to 1500.25)) {
            vm.totalBudget = input

            assertTrue("'$input' should be accepted", vm.saveChanges())

            assertEquals("'$input'", expected, trips.lastUpdate!!.second.totalBudget, 0.0)
        }
    }

    @Ignore("Bug B7: an unparseable budget is silently saved as 0.0 instead of showing a validation error")
    @Test
    fun `unparseable budget shows validation error and does not save zero`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.totalBudget = "abc"

        assertFalse(vm.saveChanges())

        assertNotNull(vm.errorMessage)
        assertNull(trips.lastUpdate)
    }
}
