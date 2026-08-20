package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Enterprise Application class responsible for core application initialization,
 * background crash telemetry monitoring, and Firebase Crashlytics setup.
 */
class FinanceApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeCrashlytics()
    }

    private fun initializeCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(true)
            crashlytics.log("FinanceApplication initialized: Production crash monitoring active.")
            Log.i("FinanceApplication", "Firebase Crashlytics successfully initialized.")
        } catch (e: Exception) {
            Log.w("FinanceApplication", "Firebase Crashlytics initialization notice: ${e.localizedMessage}")
        }
    }
}
