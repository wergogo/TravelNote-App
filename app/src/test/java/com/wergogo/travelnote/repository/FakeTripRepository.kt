package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTripRepository : TripRepository {
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    private val _members = MutableStateFlow<List<Member>>(emptyList())

    var createResult: Result<Trip> = Result.success(Trip(id = "trip-1", joinCode = "ABC123"))
    var joinResult: Result<String> = Result.success("trip-1")
    var findByCodeResult: Result<Trip?> = Result.success(null)

    fun setTrips(trips: List<Trip>) { _trips.value = trips }
    fun setMembers(members: List<Member>) { _members.value = members }

    override fun observeMyTrips(): Flow<List<Trip>> = _trips
    override fun observeTrip(tripId: String): Flow<Trip?> =
        MutableStateFlow(_trips.value.find { it.id == tripId })
    override fun observeMembers(tripId: String): Flow<List<Member>> = _members

    override suspend fun createTrip(draft: TripDraft): Result<Trip> = createResult
    override suspend fun updateTrip(tripId: String, draft: TripDraft): Result<Unit> = Result.success(Unit)
    override suspend fun findByJoinCode(code: String): Result<Trip?> = findByCodeResult
    override suspend fun joinTrip(code: String): Result<String> = joinResult
    override suspend fun leaveTrip(tripId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
    override suspend fun regenerateJoinCode(tripId: String): Result<String> = Result.success("NEW123")
}
