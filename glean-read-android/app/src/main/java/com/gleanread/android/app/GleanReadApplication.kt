package com.gleanread.android.app

import android.app.Application
import android.content.Context
import com.gleanread.android.app.di.AppContainer
import com.gleanread.android.app.sync.WorkspaceSyncWorker

class GleanReadApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        appContainer.restoreDatabaseFromSession()
        runCatching {
            WorkspaceSyncWorker.schedule(this)
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GleanReadApplication).appContainer
