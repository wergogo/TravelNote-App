@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.addevent

import com.wergogo.travelnote.B7_RULE
import com.wergogo.travelnote.DefaultLocaleRule
import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.repository.FakeAuthRepository
import com.wergogo.travelnote.repository.FakeDayRepository
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

class AddEventViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val localeRule = DefaultLocaleRule(Locale.US)

    private val auth = FakeAuthRepository().apply { currentUserId = "me" }
    private val trips = FakeTripRepository()
    private val days = FakeDayRepository()
    private val date = "2025-05-02"

    private val me = Member(userId = "me", nickname = "Me", avatarId = "avatar_02")
    private val bob = Member(userId = "bob", nickname = "Bob", avatarId = "avatar_03")

    init {
        trips.setMembers("t1", listOf(me, bob))
    }

    private fun addViewModel() = AddEventViewModel(days, trips, auth, "t1", date, null)
    private fun editViewModel(eventId: String) = AddEventViewModel(days, trips, auth, "t1", date, eventId)

    private fun AddEventViewModel.fillValidActivity() {
        eventType = EventType.ACTIVITY
        time = "14:30"
        title = "Colosseum"
    }

    private fun AddEventViewModel.fillValidFlight() {
        eventType = EventType.FLIGHT
        time = "08:05"
        depAirport = "pek"
        arrAirport = "fco"
        flightNumber = "ca 959"
    }

    // ---- validation ----

    @Test
    fun `blank time shows validation error and does not call repository`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); time = " " }

        assertFalse(vm.save())

        assertEquals("Time is required", vm.errorMessage)
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `time that is not HH colon mm shows format error`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }

        for (bad in listOf("1430", "ab:cd", "12:5", "12:345", "12-30")) {
            vm.time = bad
            assertFalse("time '$bad'", vm.save())
            assertEquals("Time must be in HH:mm format (e.g. 09:30)", vm.errorMessage)
        }
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `out of range hours or minutes show range error`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }

        for (bad in listOf("24:00", "99:00", "12:60")) {
            vm.time = bad
            assertFalse("time '$bad'", vm.save())
            assertEquals("Invalid time — hours 0-23, minutes 0-59", vm.errorMessage)
        }
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `boundary times 00 00 and 23 59 are accepted`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }

        vm.time = "00:00"
        assertTrue(vm.save())
        vm.time = "23:59"
        assertTrue(vm.save())
    }

    @Test
    fun `non numeric cost shows validation error and does not call repository`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); cost = "abc" }

        assertFalse(vm.save())

        assertEquals("Cost must be a valid number", vm.errorMessage)
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `blank cost is saved as zero`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); cost = "" }

        assertTrue(vm.save())

        assertEquals(0.0, days.lastEventDraft!!.cost, 0.0)
    }

    @Test
    fun `flight without airports shows validation error`() = runTest {
        val vm = addViewModel().apply { fillValidFlight(); arrAirport = "" }

        assertFalse(vm.save())
        assertEquals("Airports are required for flights", vm.errorMessage)

        vm.arrAirport = "FCO"
        vm.depAirport = " "
        assertFalse(vm.save())
        assertEquals("Airports are required for flights", vm.errorMessage)
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `flight does not require a title`() = runTest {
        val vm = addViewModel().apply { fillValidFlight(); title = "" }

        assertTrue(vm.save())
    }

    @Test
    fun `activity without title shows validation error`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); title = "  " }

        assertFalse(vm.save())

        assertEquals("Title is required for activities", vm.errorMessage)
        assertEquals(0, days.upsertEventCalls)
    }

    @Test
    fun `activity does not require airports`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); depAirport = ""; arrAirport = "" }

        assertTrue(vm.save())
    }

    // ---- saved draft ----

    @Test
    fun `activity draft carries trimmed values and targets this trip and date`() = runTest {
        val vm = addViewModel().apply {
            fillValidActivity()
            title = "  Colosseum tour "
            locationName = " Piazza del Colosseo "
            cost = "25.5"
        }

        assertTrue(vm.save())

        assertEquals(
            EventDraft(
                type = EventType.ACTIVITY,
                time = "14:30",
                cost = 25.5,
                title = "Colosseum tour",
                locationName = "Piazza del Colosseo",
                eventId = null
            ),
            days.lastEventDraft
        )
        assertEquals(listOf(FakeDayRepository.Target("t1", date)), days.eventWrites)
    }

    @Test
    fun `flight draft upper-cases and trims airports and flight number`() = runTest {
        val vm = addViewModel().apply { fillValidFlight(); flightNumber = " ca 959 " }

        assertTrue(vm.save())

        val draft = days.lastEventDraft!!
        assertEquals(EventType.FLIGHT, draft.type)
        assertEquals("PEK", draft.depAirport)
        assertEquals("FCO", draft.arrAirport)
        assertEquals("CA 959", draft.flightNumber)
    }

    @Test
    fun `failure surfaces the repository error message`() = runTest {
        days.upsertEventResult = Result.failure(Exception("Offline"))
        val vm = addViewModel().apply { fillValidActivity() }

        assertFalse(vm.save())

        assertEquals("Offline", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        days.upsertEventResult = Result.failure(Exception())
        val vm = addViewModel().apply { fillValidActivity() }

        vm.save()

        assertEquals("Failed to save event", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while saving and false afterwards`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
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
    fun `a successful save clears an earlier validation error`() = runTest {
        val vm = addViewModel()
        vm.save()
        assertNotNull(vm.errorMessage)

        vm.fillValidActivity()
        assertTrue(vm.save())

        assertNull(vm.errorMessage)
    }

    // ---- payer selection ----

    @Test
    fun `default payer is the current user and the member list is exposed`() = runTest {
        val vm = addViewModel()
        advanceUntilIdle()

        assertEquals("me", vm.selectedPayerId)
        assertEquals("Me", vm.selectedPayerNickname)
        assertEquals("avatar_02", vm.selectedPayerAvatarId)
        assertEquals(listOf(me, bob), vm.members.value)
    }

    @Test
    fun `default payer saves without author override`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
        advanceUntilIdle()

        vm.save()

        val draft = days.lastEventDraft!!
        assertNull(draft.authorId)
        assertNull(draft.authorNickname)
        assertNull(draft.authorAvatarId)
    }

    @Test
    fun `choosing another member sets the author override in the draft`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
        advanceUntilIdle()

        vm.selectPayer(bob)
        vm.save()

        val draft = days.lastEventDraft!!
        assertEquals("bob", draft.authorId)
        assertEquals("Bob", draft.authorNickname)
        assertEquals("avatar_03", draft.authorAvatarId)
    }

    @Test
    fun `choosing yourself leaves the author override null`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
        advanceUntilIdle()

        vm.selectPayer(bob)
        vm.selectPayer(me)
        vm.save()

        val draft = days.lastEventDraft!!
        assertNull(draft.authorId)
        assertNull(draft.authorNickname)
        assertNull(draft.authorAvatarId)
    }

    // ---- edit mode ----

    @Test
    fun `add mode is not edit mode and saves with a null event id`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }

        vm.save()

        assertFalse(vm.isEditMode)
        assertNull(days.lastEventDraft!!.eventId)
    }

    @Test
    fun `edit mode pre-loads the event and its payer`() = runTest {
        days.setEvents("t1", date,
            listOf(
                Event(id = "other", title = "Other"),
                Event(
                    id = "e1", type = EventType.FLIGHT, time = "07:45", cost = 320.0,
                    flightNumber = "BA 12", depAirport = "LHR", arrAirport = "JFK",
                    title = "t", locationName = "Terminal 5",
                    authorId = "bob", authorNickname = "Bob", authorAvatarId = "avatar_03"
                )
            )
        )

        val vm = editViewModel("e1")
        advanceUntilIdle()

        assertTrue(vm.isEditMode)
        assertEquals(EventType.FLIGHT, vm.eventType)
        assertEquals("07:45", vm.time)
        assertEquals("320.0", vm.cost)
        assertEquals("BA 12", vm.flightNumber)
        assertEquals("LHR", vm.depAirport)
        assertEquals("JFK", vm.arrAirport)
        assertEquals("t", vm.title)
        assertEquals("Terminal 5", vm.locationName)
        assertEquals("bob", vm.selectedPayerId)
        assertEquals("Bob", vm.selectedPayerNickname)
        assertEquals("avatar_03", vm.selectedPayerAvatarId)
    }

    @Test
    fun `saving an edited event keeps its id and the original payer override`() = runTest {
        days.setEvents("t1", date,
            listOf(Event(id = "e1", title = "Dinner", time = "19:00", authorId = "bob", authorNickname = "Bob", authorAvatarId = "avatar_03"))
        )
        val vm = editViewModel("e1")
        advanceUntilIdle()

        assertTrue(vm.save())

        val draft = days.lastEventDraft!!
        assertEquals("e1", draft.eventId)
        assertEquals("bob", draft.authorId)
        assertEquals("Bob", draft.authorNickname)
    }

    @Test
    fun `editing an event paid by the current user leaves the author override null`() = runTest {
        days.setEvents("t1", date,
            listOf(Event(id = "e1", title = "Dinner", time = "19:00", authorId = "me", authorNickname = "Me", authorAvatarId = "avatar_02"))
        )
        val vm = editViewModel("e1")
        advanceUntilIdle()

        vm.save()

        assertNull(days.lastEventDraft!!.authorId)
        assertNull(days.lastEventDraft!!.authorNickname)
    }

    @Test
    fun `edit mode with an unknown event id leaves the form untouched`() = runTest {
        val vm = editViewModel("missing")
        advanceUntilIdle()

        assertEquals("", vm.time)
        assertEquals("me", vm.selectedPayerId)
    }

    // ---- known bugs ----

    @Ignore(B7_RULE)
    @Test
    fun `comma followed by one or two digits is the decimal separator`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
        for ((input, expected) in listOf("12,50" to 12.5, "7,5" to 7.5)) {
            vm.cost = input

            assertTrue("'$input' should be accepted", vm.save())

            assertEquals("'$input'", expected, days.lastEventDraft!!.cost, 0.0)
        }
    }

    @Ignore(B7_RULE)
    @Test
    fun `other commas are thousands separators`() = runTest {
        val vm = addViewModel().apply { fillValidActivity() }
        for ((input, expected) in listOf("1,500" to 1500.0, "1,500.25" to 1500.25)) {
            vm.cost = input

            assertTrue("'$input' should be accepted", vm.save())

            assertEquals("'$input'", expected, days.lastEventDraft!!.cost, 0.0)
        }
    }

    @Ignore("Bug B14: time \"9:30\" is accepted unnormalised and sorts after \"10:00\"; fix = normalise the saved time to \"09:30\" in AddEventViewModel.save()")
    @Test
    fun `single digit hour is normalised to zero padded HH colon mm`() = runTest {
        val vm = addViewModel().apply { fillValidActivity(); time = "9:30" }

        assertTrue(vm.save())

        assertEquals("09:30", days.lastEventDraft!!.time)
    }
}
