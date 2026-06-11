package com.wergogo.travelnote.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(private val authRepo: AuthRepository) : ViewModel() {

    private val _destination = MutableStateFlow<Screen?>(null)
    val destination: StateFlow<Screen?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            // authState() emits Boolean: true = logged in, false = logged out
            val isLoggedIn = authRepo.authState().first()
            _destination.value = if (isLoggedIn) Screen.Home else Screen.Login
        }
    }
}
