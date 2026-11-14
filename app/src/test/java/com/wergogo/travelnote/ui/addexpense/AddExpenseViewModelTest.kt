@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.addexpense

import com.wergogo.travelnote.DefaultLocaleRule
import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.ExpenseDraft
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

// AddExpenseViewModel.fetchLocation needs an Android Context and is not covered here.
class AddExpenseViewModelTest {
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

    private fun addViewModel() = AddExpenseViewModel(days, trips, auth, "t1", date, null)
    private fun editViewModel(expenseId: String) = AddExpenseViewModel(days, trips, auth, "t1", date, expenseId)

    // ---- validation ----

    @Test
    fun `blank amount shows validation error and does not call repository`() = runTest {
        val vm = addViewModel()

        assertFalse(vm.save())

        assertEquals("Enter a valid amount", vm.errorMessage)
        assertEquals(0, days.addExpenseCalls)
    }

    @Test
    fun `non numeric, zero and negative amounts are rejected`() = runTest {
        val vm = addViewModel()

        for (bad in listOf("abc", "0", "0.0", "-5")) {
            vm.amount = bad
            assertFalse("amount '$bad'", vm.save())
            assertEquals("Enter a valid amount", vm.errorMessage)
        }
        assertEquals(0, days.addExpenseCalls)
    }

    @Test
    fun `small positive amount is accepted`() = runTest {
        val vm = addViewModel().apply { amount = "0.01" }

        assertTrue(vm.save())

        assertEquals(0.01, days.lastExpenseDraft!!.amount, 0.0)
    }

    // ---- saved draft ----

    @Test
    fun `draft carries amount, category, trimmed description and location`() = runTest {
        val vm = addViewModel().apply {
            amount = "12.5"
            category = ExpenseCategory.FOOD
            description = "  Pasta lunch "
            lat = 41.9
            lng = 12.5
            placeLabel = "41.9000, 12.5000"
        }

        assertTrue(vm.save())

        assertEquals(
            ExpenseDraft(
                category = ExpenseCategory.FOOD,
                amount = 12.5,
                description = "Pasta lunch",
                lat = 41.9,
                lng = 12.5,
                placeLabel = "41.9000, 12.5000",
                expenseId = null
            ),
            days.lastExpenseDraft
        )
        assertEquals(listOf(FakeDayRepository.Target("t1", date)), days.expenseWrites)
    }

    @Test
    fun `defaults to category other without location`() = runTest {
        val vm = addViewModel().apply { amount = "3" }

        vm.save()

        val draft = days.lastExpenseDraft!!
        assertEquals(ExpenseCategory.OTHER, draft.category)
        assertNull(draft.lat)
        assertNull(draft.lng)
    }

    @Test
    fun `failure surfaces the repository error message`() = runTest {
        days.addExpenseResult = Result.failure(Exception("Offline"))
        val vm = addViewModel().apply { amount = "3" }

        assertFalse(vm.save())

        assertEquals("Offline", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        days.addExpenseResult = Result.failure(Exception())
        val vm = addViewModel().apply { amount = "3" }

        vm.save()

        assertEquals("Failed to save expense", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while saving and false afterwards`() = runTest {
        val vm = addViewModel().apply { amount = "3" }
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

        vm.amount = "4"
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
        val vm = addViewModel().apply { amount = "3" }
        advanceUntilIdle()

        vm.save()

        val draft = days.lastExpenseDraft!!
        assertNull(draft.authorId)
        assertNull(draft.authorNickname)
        assertNull(draft.authorAvatarId)
    }

    @Test
    fun `choosing another member sets the author override in the draft`() = runTest {
        val vm = addViewModel().apply { amount = "3" }
        advanceUntilIdle()

        vm.selectPayer(bob)
        vm.save()

        val draft = days.lastExpenseDraft!!
        assertEquals("bob", draft.authorId)
        assertEquals("Bob", draft.authorNickname)
        assertEquals("avatar_03", draft.authorAvatarId)
    }

    @Test
    fun `choosing yourself leaves the author override null`() = runTest {
        val vm = addViewModel().apply { amount = "3" }
        advanceUntilIdle()

        vm.selectPayer(bob)
        vm.selectPayer(me)
        vm.save()

        val draft = days.lastExpenseDraft!!
        assertNull(draft.authorId)
        assertNull(draft.authorNickname)
        assertNull(draft.authorAvatarId)
    }

    // ---- edit mode ----

    @Test
    fun `add mode is not edit mode and saves with a null expense id`() = runTest {
        val vm = addViewModel().apply { amount = "3" }

        vm.save()

        assertFalse(vm.isEditMode)
        assertNull(days.lastExpenseDraft!!.expenseId)
    }

    @Test
    fun `edit mode pre-loads the expense and its payer`() = runTest {
        days.setExpenses("t1", date,
            listOf(
                Expense(id = "other", amount = 1.0),
                Expense(
                    id = "x1", category = ExpenseCategory.TRANSPORT, amount = 18.75, description = "Taxi",
                    lat = 41.8, lng = 12.2, placeLabel = "Airport",
                    authorId = "bob", authorNickname = "Bob", authorAvatarId = "avatar_03"
                )
            )
        )

        val vm = editViewModel("x1")
        advanceUntilIdle()

        assertTrue(vm.isEditMode)
        assertEquals(ExpenseCategory.TRANSPORT, vm.category)
        assertEquals("18.75", vm.amount)
        assertEquals("Taxi", vm.description)
        assertEquals(41.8, vm.lat!!, 0.0)
        assertEquals(12.2, vm.lng!!, 0.0)
        assertEquals("Airport", vm.placeLabel)
        assertEquals("bob", vm.selectedPayerId)
        assertEquals("Bob", vm.selectedPayerNickname)
        assertEquals("avatar_03", vm.selectedPayerAvatarId)
    }

    @Test
    fun `saving an edited expense keeps its id and the original payer override`() = runTest {
        days.setExpenses("t1", date,
            listOf(Expense(id = "x1", amount = 9.0, authorId = "bob", authorNickname = "Bob", authorAvatarId = "avatar_03"))
        )
        val vm = editViewModel("x1")
        advanceUntilIdle()

        assertTrue(vm.save())

        val draft = days.lastExpenseDraft!!
        assertEquals("x1", draft.expenseId)
        assertEquals("bob", draft.authorId)
        assertEquals("Bob", draft.authorNickname)
        assertEquals(9.0, draft.amount, 0.0)
    }

    @Test
    fun `editing an expense paid by the current user leaves the author override null`() = runTest {
        days.setExpenses("t1", date,
            listOf(Expense(id = "x1", amount = 9.0, authorId = "me", authorNickname = "Me", authorAvatarId = "avatar_02"))
        )
        val vm = editViewModel("x1")
        advanceUntilIdle()

        vm.save()

        assertNull(days.lastExpenseDraft!!.authorId)
    }

    @Test
    fun `edit mode with an unknown expense id leaves the form untouched`() = runTest {
        val vm = editViewModel("missing")
        advanceUntilIdle()

        assertEquals("", vm.amount)
        assertEquals("me", vm.selectedPayerId)
    }

    // ---- known bugs ----

    @Test
    fun `comma followed by one or two digits is the decimal separator`() = runTest {
        val vm = addViewModel()
        for ((input, expected) in listOf("12,50" to 12.5, "7,5" to 7.5)) {
            vm.amount = input

            assertTrue("'$input' should be accepted", vm.save())

            assertEquals("'$input'", expected, days.lastExpenseDraft!!.amount, 0.0)
        }
    }

    @Test
    fun `other commas are thousands separators`() = runTest {
        val vm = addViewModel()
        for ((input, expected) in listOf("1,500" to 1500.0, "1,500.25" to 1500.25)) {
            vm.amount = input

            assertTrue("'$input' should be accepted", vm.save())

            assertEquals("'$input'", expected, days.lastExpenseDraft!!.amount, 0.0)
        }
    }
}
