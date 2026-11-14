package com.wergogo.travelnote.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    var editNickname by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    private var profileJob: Job? = null

    /** Nickname last received from the repository; a field that differs from it holds unsaved edits. */
    private var syncedNickname = ""

    init {
        viewModelScope.launch {
            authRepo.authState().collect { isLoggedIn ->
                profileJob?.cancel()
                profileJob = null
                if (isLoggedIn) {
                    val uid = authRepo.currentUserId ?: return@collect
                    profileJob = viewModelScope.launch {
                        try {
                            profileRepo.observeProfile(uid).collect { profile ->
                                _profile.value = profile
                                val incoming = profile?.nickname ?: ""
                                // Follow the stored name only while the user isn't editing it.
                                if (editNickname == syncedNickname) editNickname = incoming
                                syncedNickname = incoming
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMessage = "Failed to load profile: ${e.message}"
                        }
                    }
                } else {
                    _profile.value = null
                }
            }
        }
    }

    suspend fun saveNickname(): Boolean {
        val nickname = editNickname.trim()
        if (nickname.isEmpty()) {
            errorMessage = "Display name can't be empty"
            return false
        }
        isLoading = true
        errorMessage = null
        val result = profileRepo.updateNickname(nickname)
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save"
            return false
        }
        propagateInBackground()
        successMessage = "Display name updated"
        return true
    }

    suspend fun changeAvatar(avatarId: String): Boolean {
        val result = profileRepo.updateAvatar(avatarId)
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to update avatar"
            return false
        }
        propagateInBackground()
        successMessage = "Profile photo updated"
        return true
    }

    /**
     * Copies the new name/avatar onto member records and older entries without making the user
     * wait (the repository finishes the work even if this screen closes).
     */
    private fun propagateInBackground() {
        viewModelScope.launch {
            profileRepo.propagateProfileUpdate().onFailure {
                errorMessage = "Saved, but some older entries still show your previous name or photo. " +
                    "Save again to retry."
            }
        }
    }

    fun logout() {
        profileJob?.cancel()
        profileJob = null
        authRepo.logoutSync()
    }
}
