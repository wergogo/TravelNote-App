package com.wergogo.travelnote

import android.app.Application
import com.wergogo.travelnote.data.AppContainer

class TravelNoteApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}
