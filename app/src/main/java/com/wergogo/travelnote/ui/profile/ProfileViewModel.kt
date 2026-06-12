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
                                editNickname = profile?.nickname ?: ""
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
        isLoading = true
        errorMessage = null
        val result = profileRepo.updateNickname(editNickname.trim())
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save"
            return false
        }
        // Propagate new nickname to all member docs, events, expenses, notes
        profileRepo.propagateProfileUpdate()
        successMessage = "Display name updated"
        return true
    }

    suspend fun changeAvatar(avatarId: String): Boolean {
        val result = profileRepo.updateAvatar(avatarId)
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to update avatar"
            return false
        }
        // Propagate new avatar to all member docs, events, expenses, notes
        profileRepo.propagateProfileUpdate()
        successMessage = "Profile photo updated"
        return true
    }

    fun logout() {
        profileJob?.cancel()
        profileJob = null
        authRepo.logoutSync()
    }
}
