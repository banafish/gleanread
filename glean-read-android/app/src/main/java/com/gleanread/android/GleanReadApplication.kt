package com.gleanread.android

import android.app.Application
import android.content.Context
import com.gleanread.android.core.di.AppContainer

class GleanReadApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GleanReadApplication).appContainer
