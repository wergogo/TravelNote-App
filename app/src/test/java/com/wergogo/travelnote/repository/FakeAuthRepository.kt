package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Controllable [AuthRepository]: configure results, push auth state, inspect recorded calls. */
class FakeAuthRepository : AuthRepository {
    val authStateSource = MutableStateFlow(false)

    override var currentUserId: String? = null

    var registerResult: Result<Unit> = Result.success(Unit)
    var loginResult: Result<Unit> = Result.success(Unit)
    var changePasswordResult: Result<Unit> = Result.success(Unit)

    /** When set, suspending calls wait for it, so tests can observe loading flags mid-call. */
    var gate: CompletableDeferred<Unit>? = null

    /** When false, [logoutSync] only records the call, so it cannot hide ViewModel cleanup via authState. */
    var logoutSyncUpdatesState = true

    var loginCalls = 0
        private set
    var lastLogin: Pair<String, String>? = null // email, password
        private set
    var registerCalls = 0
        private set
    var lastRegister: Triple<String, String, String>? = null // email, password, nickname
        private set
    var changePasswordCalls = 0
        private set
    var lastChangePassword: Pair<String, String>? = null // current, new
        private set
    var logoutCalls = 0
        private set
    var logoutSyncCalls = 0
        private set

    override fun authState(): Flow<Boolean> = authStateSource

    override suspend fun register(email: String, password: String, nickname: String): Result<Unit> {
        registerCalls++
        lastRegister = Triple(email, password, nickname)
        gate?.await()
        return registerResult
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        loginCalls++
        lastLogin = email to password
        gate?.await()
        return loginResult
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        changePasswordCalls++
        lastChangePassword = currentPassword to newPassword
        gate?.await()
        return changePasswordResult
    }

    override suspend fun logout() {
        logoutCalls++
        currentUserId = null
        authStateSource.value = false
    }

    override fun logoutSync() {
        logoutSyncCalls++
        if (logoutSyncUpdatesState) {
            currentUserId = null
            authStateSource.value = false
        }
    }
}
