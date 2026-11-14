package com.wergogo.travelnote.ui.changepassword

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.repository.AuthRepository

class ChangePasswordViewModel(
    private val authRepo: AuthRepository
) : ViewModel() {

    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
    var isSuccess by mutableStateOf(false)

    suspend fun changePassword(): Boolean {
        errorMessage = null

        if (currentPassword.isBlank()) {
            errorMessage = "Current password is required"
            return false
        }
        if (newPassword.length < 6) {
            errorMessage = "New password must be at least 6 characters"
            return false
        }
        if (newPassword != confirmPassword) {
            errorMessage = "New passwords do not match"
            return false
        }
        if (currentPassword == newPassword) {
            errorMessage = "New password must be different from current password"
            return false
        }

        isLoading = true
        val result = authRepo.changePassword(currentPassword, newPassword)
        isLoading = false

        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to change password"
            return false
        }

        // Stay signed in while the success message shows; finish() signs out when the screen is left,
        // so going back can never leave a signed-out user inside the app.
        isSuccess = true
        return true
    }

    /** Called when leaving the screen after a successful change: sign out so the user signs in again. */
    suspend fun finish() {
        authRepo.logout()
    }
}
