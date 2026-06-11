package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow(false)

    override var currentUserId: String? = null
        private set

    var registerResult: Result<Unit> = Result.success(Unit)
    var loginResult: Result<Unit> = Result.success(Unit)

    override fun authState(): Flow<Boolean> = _authState

    override suspend fun register(email: String, password: String, nickname: String): Result<Unit> {
        if (registerResult.isSuccess) {
            currentUserId = "test-user-id"
            _authState.value = true
        }
        return registerResult
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        if (loginResult.isSuccess) {
            currentUserId = "test-user-id"
            _authState.value = true
        }
        return loginResult
    }

    override suspend fun logout() {
        currentUserId = null
        _authState.value = false
    }
}
