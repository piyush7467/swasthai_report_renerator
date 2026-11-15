package com.piyush.swasthaireportgenerator

import android.app.Application
import com.piyush.swasthaireportgenerator.data.api.ApiClient

class SwasthAiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}
