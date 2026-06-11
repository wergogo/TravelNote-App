package com.wergogo.travelnote.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.repository.AuthRepository

class LoginViewModel(private val authRepo: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onEmailChange(v: String) {
        email = v
        errorMessage = null
    }

    fun onPasswordChange(v: String) {
        password = v
        errorMessage = null
    }

    // returns true on success so screen can navigate
    suspend fun login(): Boolean {
        isLoading = true
        errorMessage = null
        val result = authRepo.login(email.trim(), password)
        isLoading = false
        return if (result.isSuccess) {
            true
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
            false
        }
    }
}
