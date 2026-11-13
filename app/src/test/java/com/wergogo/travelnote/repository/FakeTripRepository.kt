package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Controllable [TripRepository]. [observeTrip] is derived from [tripsSource]. */
class FakeTripRepository : TripRepository {
    val tripsSource = MutableStateFlow<List<Trip>>(emptyList())
    private val members = mutableMapOf<String, MutableStateFlow<List<Member>>>()

    private fun membersFlow(tripId: String) = members.getOrPut(tripId) { MutableStateFlow(emptyList()) }

    fun setMembers(tripId: String, list: List<Member>) { membersFlow(tripId).value = list }

    /** When set, [observeMyTrips] emits nothing until it completes (simulates a slow first load). */
    var tripsEmissionGate: CompletableDeferred<Unit>? = null

    var createResult: Result<Trip> = Result.success(Trip(id = "trip-1", joinCode = "ABC123"))
    var updateResult: Result<Unit> = Result.success(Unit)
    var joinResult: Result<String> = Result.success("trip-1")
    var leaveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var regenerateResult: Result<String> = Result.success("NEW123")
    var ensureJoinCodesResult: Result<Unit> = Result.success(Unit)

    /** When set, suspending calls wait for it, so tests can observe loading flags mid-call. */
    var gate: CompletableDeferred<Unit>? = null

    var createCalls = 0
        private set
    var lastCreatedDraft: TripDraft? = null
        private set
    var updateCalls = 0
        private set
    var lastUpdate: Pair<String, TripDraft>? = null // tripId, draft
        private set
    var joinCalls = 0
        private set
    var lastJoinCode: String? = null
        private set
    val leftTripIds = mutableListOf<String>()
    val deletedTripIds = mutableListOf<String>()
    val regeneratedTripIds = mutableListOf<String>()
    var ensureJoinCodesCalls = 0
        private set

    override fun observeMyTrips(): Flow<List<Trip>> = flow {
        tripsEmissionGate?.await()
        emitAll(tripsSource)
    }
    override fun observeTrip(tripId: String): Flow<Trip?> =
        tripsSource.map { trips -> trips.find { it.id == tripId } }
    override fun observeMembers(tripId: String): Flow<List<Member>> = membersFlow(tripId)

    override suspend fun createTrip(draft: TripDraft): Result<Trip> {
        createCalls++
        lastCreatedDraft = draft
        gate?.await()
        return createResult
    }

    override suspend fun updateTrip(tripId: String, draft: TripDraft): Result<Unit> {
        updateCalls++
        lastUpdate = tripId to draft
        gate?.await()
        return updateResult
    }

    override suspend fun joinTrip(code: String): Result<String> {
        joinCalls++
        lastJoinCode = code
        gate?.await()
        return joinResult
    }

    override suspend fun leaveTrip(tripId: String): Result<Unit> {
        leftTripIds += tripId
        return leaveResult
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        deletedTripIds += tripId
        return deleteResult
    }

    override suspend fun regenerateJoinCode(tripId: String): Result<String> {
        regeneratedTripIds += tripId
        return regenerateResult
    }

    override suspend fun ensureJoinCodes(): Result<Unit> {
        ensureJoinCodesCalls++
        return ensureJoinCodesResult
    }
}
