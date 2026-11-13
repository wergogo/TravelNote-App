@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.splash

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.repository.FakeAuthRepository
import com.wergogo.travelnote.ui.navigation.Screen
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SplashViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()

    @Test
    fun `logged in user is sent to home`() = runTest {
        auth.authStateSource.value = true

        val vm = SplashViewModel(auth)
        advanceUntilIdle()

        assertEquals(Screen.Home, vm.destination.value)
    }

    @Test
    fun `logged out user is sent to login`() = runTest {
        auth.authStateSource.value = false

        val vm = SplashViewModel(auth)
        advanceUntilIdle()

        assertEquals(Screen.Login, vm.destination.value)
    }

    @Test
    fun `destination is decided once from the first auth state`() = runTest {
        auth.authStateSource.value = true
        val vm = SplashViewModel(auth)
        advanceUntilIdle()

        auth.authStateSource.value = false
        advanceUntilIdle()

        assertEquals(Screen.Home, vm.destination.value)
    }
}
