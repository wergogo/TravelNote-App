@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wergogo.travelnote.ui.profile

import com.wergogo.travelnote.MainDispatcherRule
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.repository.FakeAuthRepository
import com.wergogo.travelnote.repository.FakeProfileRepository
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
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository()
    private val ann = UserProfile(id = "u1", email = "ann@example.com", nickname = "Ann", avatarId = "avatar_01")

    private fun loggedInViewModel(): ProfileViewModel {
        auth.currentUserId = "u1"
        auth.authStateSource.value = true
        profiles.setProfile("u1", ann)
        return ProfileViewModel(auth, profiles)
    }

    @Test
    fun `loads the profile of the current user when logged in`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()

        assertEquals(ann, vm.profile.value)
        assertEquals("Ann", vm.editNickname)
        assertEquals("u1", profiles.lastObservedUserId)
    }

    @Test
    fun `profile stays empty while logged out`() = runTest {
        auth.authStateSource.value = false
        profiles.setProfile("u1", ann)

        val vm = ProfileViewModel(auth, profiles)
        advanceUntilIdle()

        assertNull(vm.profile.value)
        assertNull(profiles.lastObservedUserId)
    }

    @Test
    fun `profile is cleared when the user logs out`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()

        auth.authStateSource.value = false
        advanceUntilIdle()

        assertNull(vm.profile.value)
    }

    @Test
    fun `saveNickname trims the name, updates it and propagates the change`() = runTest {
        val vm = loggedInViewModel()
        vm.editNickname = "  Annie  "

        val ok = vm.saveNickname()

        assertTrue(ok)
        assertEquals(listOf("Annie"), profiles.nicknameUpdates)
        assertEquals(1, profiles.propagateCalls)
        assertEquals("Display name updated", vm.successMessage)
        assertNull(vm.errorMessage)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `saveNickname failure surfaces the message and skips propagation`() = runTest {
        val vm = loggedInViewModel()
        profiles.updateNicknameResult = Result.failure(Exception("Permission denied"))
        vm.editNickname = "Annie"

        val ok = vm.saveNickname()

        assertFalse(ok)
        assertEquals("Permission denied", vm.errorMessage)
        assertNull(vm.successMessage)
        assertEquals(0, profiles.propagateCalls)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `saveNickname failure without a message falls back to a default`() = runTest {
        val vm = loggedInViewModel()
        profiles.updateNicknameResult = Result.failure(Exception())
        vm.editNickname = "Annie"

        vm.saveNickname()

        assertEquals("Failed to save", vm.errorMessage)
    }

    @Test
    fun `changeAvatar updates the avatar and propagates the change`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()

        val ok = vm.changeAvatar("avatar_05")

        assertTrue(ok)
        assertEquals(listOf("avatar_05"), profiles.avatarUpdates)
        assertEquals(1, profiles.propagateCalls)
        assertEquals("Profile photo updated", vm.successMessage)
        assertEquals("avatar_05", vm.profile.value?.avatarId)
    }

    @Test
    fun `changeAvatar failure surfaces the message and skips propagation`() = runTest {
        val vm = loggedInViewModel()
        profiles.updateAvatarResult = Result.failure(Exception("Offline"))

        val ok = vm.changeAvatar("avatar_05")

        assertFalse(ok)
        assertEquals("Offline", vm.errorMessage)
        assertEquals(0, profiles.propagateCalls)
    }

    @Test
    fun `changeAvatar failure without a message falls back to a default`() = runTest {
        val vm = loggedInViewModel()
        profiles.updateAvatarResult = Result.failure(Exception())

        vm.changeAvatar("avatar_05")

        assertEquals("Failed to update avatar", vm.errorMessage)
    }

    @Test
    fun `logout signs out synchronously`() = runTest {
        val vm = loggedInViewModel()

        vm.logout()

        assertEquals(1, auth.logoutSyncCalls)
    }

    @Test
    fun `profile load failure is reported in the error message`() = runTest {
        profiles.observeError = IllegalStateException("permission denied")

        val vm = loggedInViewModel()
        advanceUntilIdle()

        assertEquals("Failed to load profile: permission denied", vm.errorMessage)
        assertNull(vm.profile.value)
    }

    @Test
    fun `logged in without a current user id observes nothing`() = runTest {
        auth.currentUserId = null
        auth.authStateSource.value = true
        profiles.setProfile("u1", ann)

        val vm = ProfileViewModel(auth, profiles)
        advanceUntilIdle()

        assertNull(profiles.lastObservedUserId)
        assertNull(vm.profile.value)
    }

    @Test
    fun `switching to another user observes that user's profile`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        assertEquals("Ann", vm.profile.value?.nickname)

        auth.authStateSource.value = false
        advanceUntilIdle()
        assertNull(vm.profile.value)

        val bea = UserProfile(id = "u2", nickname = "Bea")
        profiles.setProfile("u2", bea)
        auth.currentUserId = "u2"
        auth.authStateSource.value = true
        advanceUntilIdle()

        assertEquals("u2", profiles.lastObservedUserId)
        assertEquals(bea, vm.profile.value)
        assertEquals("Bea", vm.editNickname)

        // The previous user's profile is no longer observed: its updates must not leak in.
        profiles.setProfile("u1", ann.copy(nickname = "Stale Ann"))
        advanceUntilIdle()

        assertEquals(bea, vm.profile.value)
        assertEquals("Bea", vm.editNickname)
    }

    @Test
    fun `isLoading is true while the nickname is being saved`() = runTest {
        val vm = loggedInViewModel()
        val gate = CompletableDeferred<Unit>()
        profiles.gate = gate
        vm.editNickname = "Annie"

        val job = launch { vm.saveNickname() }
        runCurrent()
        assertTrue(vm.isLoading)

        gate.complete(Unit)
        job.join()
        assertFalse(vm.isLoading)
    }

    @Test
    fun `logout stops collecting the profile`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        // Keep the auth state "logged in" so only logout() itself can stop the collection.
        auth.logoutSyncUpdatesState = false

        vm.logout()
        profiles.setProfile("u1", ann.copy(nickname = "Changed after logout"))
        advanceUntilIdle()

        assertEquals(1, auth.logoutSyncCalls)
        assertEquals("Ann", vm.profile.value?.nickname)
    }

    @Test
    fun `blank nickname shows validation error and does not call repository`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        vm.editNickname = "   "

        val ok = vm.saveNickname()

        assertFalse(ok)
        assertNotNull(vm.errorMessage)
        assertTrue(profiles.nicknameUpdates.isEmpty())
        assertEquals(0, profiles.propagateCalls)
    }

    @Test
    fun `unsaved nickname edit survives a new profile emission`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        vm.editNickname = "Draft name"

        // Changing the avatar makes the repository emit an updated profile.
        vm.changeAvatar("avatar_07")
        advanceUntilIdle()

        assertEquals("avatar_07", vm.profile.value?.avatarId)
        assertEquals("Draft name", vm.editNickname)
    }

    @Test
    fun `saving the nickname does not wait for older entries to be updated`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        profiles.propagateGate = CompletableDeferred()   // propagation is still running…
        vm.editNickname = "Annie"

        val save = launch { vm.saveNickname() }
        runCurrent()

        assertTrue("save should finish while propagation is still running", save.isCompleted)
        assertEquals("Display name updated", vm.successMessage)
        assertEquals(1, profiles.propagateCalls)
        profiles.propagateGate!!.complete(Unit)
    }

    @Test
    fun `a failed update of older entries is reported but the new name is kept`() = runTest {
        val vm = loggedInViewModel()
        advanceUntilIdle()
        profiles.propagateResult = Result.failure(Exception("offline"))
        vm.editNickname = "Annie"

        val ok = vm.saveNickname()
        advanceUntilIdle()

        assertTrue(ok)
        assertEquals(listOf("Annie"), profiles.nicknameUpdates)
        assertNotNull(vm.errorMessage)
    }
}
