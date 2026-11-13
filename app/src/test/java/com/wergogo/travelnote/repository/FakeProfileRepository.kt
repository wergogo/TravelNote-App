package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Controllable [ProfileRepository]. Profiles are keyed by user id; successful updates are reflected in them. */
class FakeProfileRepository : ProfileRepository {
    private val profiles = mutableMapOf<String, MutableStateFlow<UserProfile?>>()

    private fun profileFlow(userId: String) = profiles.getOrPut(userId) { MutableStateFlow(null) }

    fun setProfile(userId: String, profile: UserProfile?) { profileFlow(userId).value = profile }

    var updateNicknameResult: Result<Unit> = Result.success(Unit)
    var updateAvatarResult: Result<Unit> = Result.success(Unit)

    /** When set, the observed profile flow throws this instead of emitting. */
    var observeError: Throwable? = null

    /** When set, [updateNickname] waits for it, so tests can observe loading flags mid-call. */
    var gate: CompletableDeferred<Unit>? = null

    var lastObservedUserId: String? = null
        private set
    val nicknameUpdates = mutableListOf<String>()
    val avatarUpdates = mutableListOf<String>()
    var propagateCalls = 0
        private set

    override fun observeProfile(userId: String): Flow<UserProfile?> {
        lastObservedUserId = userId
        val error = observeError
        return if (error != null) flow { throw error } else profileFlow(userId)
    }

    /** Successful updates apply to the profile of the user observed last (the "current user"). */
    private fun updateObserved(change: (UserProfile) -> UserProfile) {
        val flow = profiles[lastObservedUserId] ?: return
        flow.value = flow.value?.let(change)
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> {
        nicknameUpdates += nickname
        gate?.await()
        if (updateNicknameResult.isSuccess) {
            updateObserved { it.copy(nickname = nickname) }
        }
        return updateNicknameResult
    }

    override suspend fun updateAvatar(avatarId: String): Result<Unit> {
        avatarUpdates += avatarId
        if (updateAvatarResult.isSuccess) {
            updateObserved { it.copy(avatarId = avatarId) }
        }
        return updateAvatarResult
    }

    override suspend fun propagateProfileUpdate(): Result<Unit> {
        propagateCalls++
        return Result.success(Unit)
    }
}
