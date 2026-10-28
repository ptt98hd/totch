package io.github.ow3n.totch

import android.app.Application

class TotchApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Replaced by the platform layer with the real DefaultAppContainer.
        container = TODO("platform layer")
    }
}
