package io.github.ow3n.totch

import android.app.Application
import io.github.ow3n.totch.service.DefaultAppContainer

class TotchApp : Application() {
    /** Platform object graph; also used directly by the services. */
    internal lateinit var platform: DefaultAppContainer
        private set

    /** Contract for the UI layer. */
    val container: AppContainer get() = platform

    override fun onCreate() {
        super.onCreate()
        platform = DefaultAppContainer(this)
    }
}
