/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp

import android.app.Application
import com.wyplay.wycdn.sampleapp.ui.models.AppConfigDataSource
import com.wyplay.wycdn.sampleapp.ui.models.MediaBuiltinDataSource
import com.wyplay.wycdn.sampleapp.ui.models.MediaRemoteDataSource
import com.wyplay.wycdn.sampleapp.ui.models.MediaRepository
import com.wyplay.wycdn.sampleapp.ui.models.SettingsRepository
import com.wyplay.wycdn.sampleapp.ui.models.dataStore

/**
 * Application singleton instantiated by Android.
 *
 * The purpose of this singleton is to facilitate manual dependency injection by providing
 * centralized access to shared dependencies to ViewModel factories.
 */
class SampleApp : Application() {

    /**
     * Instance of [SettingsRepository] providing access to application settings.
     */
    val settingsRepository by lazy {
        SettingsRepository(dataStore, AppConfigDataSource(this.assets))
    }

    /**
     * Instance of [MediaRepository] providing access to media content.
     */
    val mediaRepository by lazy {
        val remoteMediaListUrl = null // Update to fetch media list from a remote data source
        MediaRepository(
            MediaBuiltinDataSource(this.assets),
            MediaRemoteDataSource(remoteMediaListUrl),
            settingsRepository.appConfig.settings
        )
    }
}
