package com.wergogo.travelnote.data.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    fun authState(): Flow<Boolean>
    suspend fun register(email: String, password: String, nickname: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
}
