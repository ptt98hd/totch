package io.github.ptt98hd.totch

import android.app.Application
import android.content.Context
import io.github.ptt98hd.totch.locale.AppLocales
import io.github.ptt98hd.totch.service.DefaultAppContainer

class TotchApp : Application() {
    /** Platform object graph; also used directly by the services. */
    internal lateinit var platform: DefaultAppContainer
        private set

    /** Contract for the UI layer. */
    val container: AppContainer get() = platform

    /** Android 12: toasts and notifications use the chosen language (LG-2). */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocales.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        platform = DefaultAppContainer(this)
    }
}
