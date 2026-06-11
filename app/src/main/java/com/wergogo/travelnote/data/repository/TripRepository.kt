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
    suspend fun findByJoinCode(code: String): Result<Trip?>
    suspend fun joinTrip(code: String): Result<String>      // returns tripId
    suspend fun leaveTrip(tripId: String): Result<Unit>
    suspend fun deleteTrip(tripId: String): Result<Unit>    // owner only
    suspend fun regenerateJoinCode(tripId: String): Result<String>
}
