@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.login

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

class LoginViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private lateinit var vm: LoginViewModel

    @Before
    fun setUp() {
        vm = LoginViewModel(auth)
    }

    @Test
    fun `successful login returns true and passes trimmed email and raw password`() = runTest {
        vm.onEmailChange("  me@example.com ")
        vm.onPasswordChange(" secret ")

        val ok = vm.login()

        assertTrue(ok)
        assertNull(vm.errorMessage)
        assertEquals("me@example.com" to " secret ", auth.lastLogin)
        assertEquals(1, auth.loginCalls)
    }

    @Test
    fun `failed login returns false and surfaces the repository error message`() = runTest {
        auth.loginResult = Result.failure(Exception("Wrong password"))

        val ok = vm.login()

        assertFalse(ok)
        assertEquals("Wrong password", vm.errorMessage)
    }

    @Test
    fun `failed login without a message falls back to a default message`() = runTest {
        auth.loginResult = Result.failure(Exception())

        vm.login()

        assertEquals("Login failed", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while logging in and false afterwards`() = runTest {
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate

        val job = launch { vm.login() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `isLoading is reset after a failed login`() = runTest {
        auth.loginResult = Result.failure(Exception("nope"))

        vm.login()

        assertFalse(vm.isLoading)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest {
        auth.loginResult = Result.failure(Exception("Wrong password"))
        vm.login()
        assertEquals("Wrong password", vm.errorMessage)

        vm.onPasswordChange("x")
        assertNull(vm.errorMessage)

        vm.login()
        assertEquals("Wrong password", vm.errorMessage)

        vm.onEmailChange("a@b.c")
        assertNull(vm.errorMessage)
    }

    @Test
    fun `a new login attempt clears the previous error on success`() = runTest {
        auth.loginResult = Result.failure(Exception("Wrong password"))
        vm.login()
        auth.loginResult = Result.success(Unit)

        val ok = vm.login()

        assertTrue(ok)
        assertNull(vm.errorMessage)
    }
}
