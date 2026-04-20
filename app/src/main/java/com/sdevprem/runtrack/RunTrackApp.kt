package com.sdevprem.runtrack

import android.app.Application
import android.content.pm.PackageManager
import com.melody.map.gd_compose.utils.MapUtils as GdMapUtils
import com.sdevprem.runtrack.shared.background.notification.TrackingNotificationHelper
import com.sdevprem.runtrack.shared.di.AppModule
import com.sdevprem.runtrack.shared.di.PlatformModule
import com.sdevprem.runtrack.shared.diagnostics.buildAmapDiagnosticReport
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
        logCurrentSHA1() // Prompt if SHA1 mismatch with Amap console key binding (from reference keystore)
        logAmapDiagnostics()
        startKoin {
            androidContext(this@RunTrackApp)
            androidLogger()
            modules(PlatformModule.module)
            modules(AppModule().module)
        }
        notificationHelper.createNotificationChannel()
    }
    @Suppress("DEPRECATION")
    private fun logCurrentSHA1() {
        try {
            val signatures = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
            signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA1")
                md.update(signature.toByteArray())
                val sha1 = md.digest().joinToString(":") { String.format("%02X", it) }
                Timber.i("Current App SHA1: $sha1 - Update in Amap console (lbs.amap.com) if map is white or location fails")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get SHA1")
        }
    }

    private fun logAmapDiagnostics() {
        runCatching {
            val report = buildAmapDiagnosticReport(this)
            val tag = if (report.hasError) "AmapDiagnostics ERROR" else "AmapDiagnostics OK"
            Timber.i("$tag\n${report.lines.joinToString("\n")}")
        }.onFailure { e ->
            Timber.e(e, "高德自检失败（不应静默）")
        }
    }
}