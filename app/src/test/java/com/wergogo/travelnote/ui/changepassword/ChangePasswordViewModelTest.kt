@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.changepassword

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.repository.FakeAuthRepository
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

class ChangePasswordViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private lateinit var vm: ChangePasswordViewModel

    @Before
    fun setUp() {
        vm = ChangePasswordViewModel(auth)
    }

    private fun fill(current: String = "oldpass", new: String = "newpass1", confirm: String = new) {
        vm.currentPassword = current
        vm.newPassword = new
        vm.confirmPassword = confirm
    }

    private fun assertRejected(message: String) {
        assertEquals(message, vm.errorMessage)
        assertEquals(0, auth.changePasswordCalls)
        assertEquals(0, auth.logoutCalls)
        assertFalse(vm.isSuccess)
    }

    @Test
    fun `blank current password shows validation error`() = runTest {
        fill(current = "  ")

        assertFalse(vm.changePassword())

        assertRejected("Current password is required")
    }

    @Test
    fun `short new password shows validation error`() = runTest {
        fill(new = "abc12")

        assertFalse(vm.changePassword())

        assertRejected("New password must be at least 6 characters")
    }

    @Test
    fun `mismatched confirmation shows validation error`() = runTest {
        fill(new = "newpass1", confirm = "newpass2")

        assertFalse(vm.changePassword())

        assertRejected("New passwords do not match")
    }

    @Test
    fun `new password equal to current shows validation error`() = runTest {
        fill(current = "samepass", new = "samepass")

        assertFalse(vm.changePassword())

        assertRejected("New password must be different from current password")
    }

    @Test
    fun `success passes both passwords, marks success and logs the user out`() = runTest {
        fill()

        val ok = vm.changePassword()

        assertTrue(ok)
        assertTrue(vm.isSuccess)
        assertNull(vm.errorMessage)
        assertEquals("oldpass" to "newpass1", auth.lastChangePassword)
        assertEquals(1, auth.logoutCalls)
    }

    @Test
    fun `failure surfaces the repository message and does not log out`() = runTest {
        fill()
        auth.changePasswordResult = Result.failure(Exception("Current password is incorrect"))

        val ok = vm.changePassword()

        assertFalse(ok)
        assertEquals("Current password is incorrect", vm.errorMessage)
        assertFalse(vm.isSuccess)
        assertFalse(vm.isLoading)
        assertEquals(0, auth.logoutCalls)
    }

    @Test
    fun `failure without a message falls back to a default message`() = runTest {
        fill()
        auth.changePasswordResult = Result.failure(Exception())

        vm.changePassword()

        assertEquals("Failed to change password", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while the request is in flight`() = runTest {
        fill()
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate

        val job = launch { vm.changePassword() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `a previous error is cleared on the next attempt`() = runTest {
        fill(current = "")
        vm.changePassword()
        fill()

        assertTrue(vm.changePassword())

        assertNull(vm.errorMessage)
    }
}
