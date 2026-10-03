package com.hellacious.spirecart

import android.app.Application
import com.hellacious.spirecart.core.di.AppContainer
import com.hellacious.spirecart.core.di.DefaultAppContainer

class SpireCartApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
