package com.wergogo.travelnote.data.repository

import com.wergogo.travelnote.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(userId: String): Flow<UserProfile?>
    suspend fun updateNickname(nickname: String): Result<Unit>
    suspend fun updateAvatar(avatarId: String): Result<Unit>
}
