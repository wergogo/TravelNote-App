package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProfileRepository : ProfileRepository {
    private val _profile = MutableStateFlow<UserProfile?>(null)

    var updateNicknameResult: Result<Unit> = Result.success(Unit)
    var updateAvatarResult: Result<Unit> = Result.success(Unit)

    fun setProfile(profile: UserProfile?) { _profile.value = profile }

    override fun observeProfile(userId: String): Flow<UserProfile?> = _profile

    override suspend fun updateNickname(nickname: String): Result<Unit> {
        if (updateNicknameResult.isSuccess) {
            _profile.value = _profile.value?.copy(nickname = nickname)
        }
        return updateNicknameResult
    }

    override suspend fun updateAvatar(avatarId: String): Result<Unit> {
        if (updateAvatarResult.isSuccess) {
            _profile.value = _profile.value?.copy(avatarId = avatarId)
        }
        return updateAvatarResult
    }
}
