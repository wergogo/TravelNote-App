package com.wergogo.travelnote.data.repository

import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeMyTrips(): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    fun observeMembers(tripId: String): Flow<List<Member>>
    suspend fun createTrip(draft: TripDraft): Result<Trip>
    suspend fun updateTrip(tripId: String, draft: TripDraft): Result<Unit>
    suspend fun joinTrip(code: String): Result<String>      // returns tripId
    suspend fun leaveTrip(tripId: String): Result<Unit>
    suspend fun deleteTrip(tripId: String): Result<Unit>    // owner only
    suspend fun regenerateJoinCode(tripId: String): Result<String>

    /**
     * Makes sure every trip the current user belongs to has a join-code lookup record, so codes of
     * trips created before 1.0.2 keep working. Cheap to call repeatedly: it runs once per user per
     * process.
     */
    suspend fun ensureJoinCodes(): Result<Unit>
}
