@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.register

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

class RegisterViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private lateinit var vm: RegisterViewModel

    @Before
    fun setUp() {
        vm = RegisterViewModel(auth)
    }

    private fun fillValidForm() {
        vm.onEmailChange(" new@example.com ")
        vm.onNicknameChange("  Ann ")
        vm.onPasswordChange("secret1")
        vm.onConfirmPasswordChange("secret1")
    }

    @Test
    fun `blank nickname shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.onNicknameChange("   ")

        assertFalse(vm.register())

        assertEquals("Nickname is required", vm.errorMessage)
        assertEquals(0, auth.registerCalls)
    }

    @Test
    fun `mismatched passwords show validation error and do not call repository`() = runTest {
        fillValidForm()
        vm.onConfirmPasswordChange("different")

        assertFalse(vm.register())

        assertEquals("Passwords do not match", vm.errorMessage)
        assertEquals(0, auth.registerCalls)
    }

    @Test
    fun `short password shows validation error and does not call repository`() = runTest {
        fillValidForm()
        vm.onPasswordChange("12345")
        vm.onConfirmPasswordChange("12345")

        assertFalse(vm.register())

        assertEquals("Password must be at least 6 characters", vm.errorMessage)
        assertEquals(0, auth.registerCalls)
    }

    @Test
    fun `password of exactly six characters is accepted`() = runTest {
        fillValidForm()
        vm.onPasswordChange("123456")
        vm.onConfirmPasswordChange("123456")

        assertTrue(vm.register())
    }

    @Test
    fun `successful registration passes trimmed email and nickname`() = runTest {
        fillValidForm()

        val ok = vm.register()

        assertTrue(ok)
        assertNull(vm.errorMessage)
        assertEquals(Triple("new@example.com", "secret1", "Ann"), auth.lastRegister)
    }

    @Test
    fun `failed registration surfaces the repository error message`() = runTest {
        fillValidForm()
        auth.registerResult = Result.failure(Exception("Email already in use"))

        val ok = vm.register()

        assertFalse(ok)
        assertEquals("Email already in use", vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `failed registration without a message falls back to a default message`() = runTest {
        fillValidForm()
        auth.registerResult = Result.failure(Exception())

        vm.register()

        assertEquals("Registration failed", vm.errorMessage)
    }

    @Test
    fun `isLoading is true while registering and false afterwards`() = runTest {
        fillValidForm()
        val gate = CompletableDeferred<Unit>()
        auth.gate = gate

        val job = launch { vm.register() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `editing any field clears the previous error`() = runTest {
        val edits = listOf<Pair<String, (String) -> Unit>>(
            "email" to vm::onEmailChange,
            "password" to vm::onPasswordChange,
            "confirm password" to vm::onConfirmPasswordChange,
            "nickname" to vm::onNicknameChange
        )
        for ((field, edit) in edits) {
            vm.onNicknameChange("")
            vm.register()
            assertEquals("error before editing $field", "Nickname is required", vm.errorMessage)

            edit("x")

            assertNull("error after editing $field", vm.errorMessage)
        }
    }
}
