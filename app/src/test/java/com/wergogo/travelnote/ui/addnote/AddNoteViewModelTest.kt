@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.addnote

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.NoteDraft
import com.wergogo.travelnote.repository.FakeDayRepository
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

class AddNoteViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val days = FakeDayRepository()
    private lateinit var vm: AddNoteViewModel

    @Before
    fun setUp() {
        vm = AddNoteViewModel(days, "t1", "2025-05-02")
    }

    @Test
    fun `blank note shows validation error and does not call repository`() = runTest {
        vm.text = "   "

        assertFalse(vm.save())

        assertEquals("Note cannot be empty", vm.errorMessage)
        assertEquals(0, days.addNoteCalls)
    }

    @Test
    fun `success saves the trimmed text for this trip and date`() = runTest {
        vm.text = "  Buy tickets  "

        assertTrue(vm.save())

        assertNull(vm.errorMessage)
        assertEquals(NoteDraft("Buy tickets"), days.lastNoteDraft)
        assertEquals(listOf(FakeDayRepository.Target("t1", "2025-05-02")), days.noteWrites)
    }

    @Test
    fun `failure surfaces the repository error message`() = runTest {
        days.addNoteResult = Result.failure(Exception("Offline"))
        vm.text = "hello"

        assertFalse(vm.save())

        assertEquals("Offline", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        days.addNoteResult = Result.failure(Exception())
        vm.text = "hello"

        vm.save()

        assertEquals("Failed to save note", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while saving and false afterwards`() = runTest {
        vm.text = "hello"
        val gate = CompletableDeferred<Unit>()
        days.gate = gate

        val job = launch { vm.save() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `a previous validation error is cleared by a successful save`() = runTest {
        vm.save()
        vm.text = "now valid"

        assertTrue(vm.save())

        assertNull(vm.errorMessage)
    }
}
