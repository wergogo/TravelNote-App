package com.wergogo.travelnote.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.ProfileRepository
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.data.source.firebase.FirebaseAuthRepository
import com.wergogo.travelnote.data.source.firebase.FirebaseDayRepository
import com.wergogo.travelnote.data.source.firebase.FirebaseProfileRepository
import com.wergogo.travelnote.data.source.firebase.FirebaseTripRepository

class AppContainer {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    val authRepository: AuthRepository = FirebaseAuthRepository(auth, firestore)
    val profileRepository: ProfileRepository = FirebaseProfileRepository(auth, firestore)
    val tripRepository: TripRepository = FirebaseTripRepository(auth, firestore)
    val dayRepository: DayRepository = FirebaseDayRepository(auth, firestore)
}
