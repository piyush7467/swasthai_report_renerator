package com.piyush.swasthaireportgenerator

import android.app.Application
import android.util.Log
import com.piyush.swasthaireportgenerator.data.api.ApiClient

class SwasthAiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalCrashHandler()
        ApiClient.init(this)
    }

    private fun setupGlobalCrashHandler() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("SwasthAiApp", "FATAL UNCAUGHT EXCEPTION on thread [${thread.name}]", throwable)
            } catch (_: Throwable) {
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
