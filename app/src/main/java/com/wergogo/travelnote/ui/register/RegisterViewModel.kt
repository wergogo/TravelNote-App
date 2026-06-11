package com.wergogo.travelnote.ui.register

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.repository.AuthRepository

class RegisterViewModel(private val authRepo: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var nickname by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onEmailChange(v: String) { email = v; errorMessage = null }
    fun onPasswordChange(v: String) { password = v; errorMessage = null }
    fun onConfirmPasswordChange(v: String) { confirmPassword = v; errorMessage = null }
    fun onNicknameChange(v: String) { nickname = v; errorMessage = null }

    suspend fun register(): Boolean {
        if (nickname.isBlank()) { errorMessage = "Nickname is required"; return false }
        if (password != confirmPassword) { errorMessage = "Passwords do not match"; return false }
        if (password.length < 6) { errorMessage = "Password must be at least 6 characters"; return false }
        isLoading = true
        errorMessage = null
        val result = authRepo.register(email.trim(), password, nickname.trim())
        isLoading = false
        return if (result.isSuccess) {
            true
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Registration failed"
            false
        }
    }
}
