package com.sdevprem.runtrack

import android.app.Application
import com.melody.map.gd_compose.utils.MapUtils as GdMapUtils
import com.sdevprem.runtrack.shared.background.notification.TrackingNotificationHelper
import com.sdevprem.runtrack.shared.di.AppModule
import com.sdevprem.runtrack.shared.di.PlatformModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.ksp.generated.module
import timber.log.Timber

class RunTrackApp : Application() {
    val notificationHelper: TrackingNotificationHelper by inject()
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        // Set map privacy agreement for Gaode/OmniMap (must be called immediately after user agrees to privacy policy)
        GdMapUtils.setMapPrivacy(this, true)
        startKoin {
            androidContext(this@RunTrackApp)
            androidLogger()
            modules(PlatformModule.module)
            modules(AppModule().module)
        }
        notificationHelper.createNotificationChannel()
    }
}