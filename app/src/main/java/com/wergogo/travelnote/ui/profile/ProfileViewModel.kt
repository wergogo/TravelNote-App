package com.wergogo.travelnote.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    var editNickname by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    private var currentUid: String? = null

    init {
        viewModelScope.launch {
            authRepo.authState().collect { uid ->
                currentUid = uid
                if (uid != null) {
                    profileRepo.observeProfile(uid).collect { profile ->
                        _profile.value = profile
                        editNickname = profile?.nickname ?: ""
                    }
                } else {
                    _profile.value = null
                }
            }
        }
    }

    suspend fun saveNickname(): Boolean {
        val uid = currentUid ?: return false
        isLoading = true
        errorMessage = null
        val result = profileRepo.updateNickname(uid, editNickname.trim())
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save"
            return false
        }
        return true
    }

    suspend fun changeAvatar(avatarId: String): Boolean {
        val uid = currentUid ?: return false
        val result = profileRepo.updateAvatar(uid, avatarId)
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to update avatar"
            return false
        }
        return true
    }

    suspend fun logout(): Result<Unit> = authRepo.logout()
}
