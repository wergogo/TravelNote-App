package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val profile = snapshot?.takeIf { it.exists() }?.let { doc ->
                    UserProfile(
                        id = doc.getString("id") ?: userId,
                        email = doc.getString("email") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        avatarId = doc.getString("avatarId") ?: "avatar_01",
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }
                trySend(profile)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        firestore.collection("users").document(uid)
            .update("nickname", nickname)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateAvatar(avatarId: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        firestore.collection("users").document(uid)
            .update("avatarId", avatarId)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
