/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.app.Application
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wyplay.wycdn.WycdnDownloadClient
import com.wyplay.wycdn.WycdnServiceConnection
import com.wyplay.wycdn.sampleapp.SampleApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "WycdnViewModel"

/**
 * ViewModel managing the WyCDN service connection.
 *
 * @property application The [Application] instance to use providing the Android context.
 * @companion Factory Provides a factory for creating [WycdnViewModel] instances, encapsulating dependency injection.
 */
class WycdnViewModel(application: Application) : AndroidViewModel(application) {

    /** Connection to the WyCDN service. */
    private val wycdn = WycdnServiceConnection(getApplication())

    // Property providing the Android application instance.
    private val app: SampleApp = getApplication()

    /** WyCDN download client used to download media manifests/segments */
    val downloadClient = WycdnDownloadClient(wycdn)

    /** Identifier to use for our peer. */
    val peerId: String by lazy {
        val app: SampleApp = getApplication()
        val androidId = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID)
        "${Build.PRODUCT}-$androidId"
    }

    /* _wycdnMode is a private mutable state flow representing the current WyCDN mode. */
    private val _wycdnMode = MutableStateFlow("full")
    /* a public read-only state flow providing the current WyCDN mode. */
    val wycdnMode: StateFlow<String> = _wycdnMode.asStateFlow()

    init {
        viewModelScope.launch {
            // Always start with "full" mode
            app.settingsRepository.resetToDefaultMode()
        }
    }

    override fun onCleared() {
        // Stop the WyCDN service when the view model is destroyed
        wycdn.unbindService()
        super.onCleared()
    }

    /**
     * Restarts the WyCDN service using current settings.
     */
    fun restartService() {
        viewModelScope.launch {
            val app: SampleApp = getApplication()

            // Collect settings values
            val wycdnEnv = app.settingsRepository.wycdnEnvironment.value
            val wycdnConfig = app.settingsRepository.appConfig.wycdnConfig
            val wycdnDownloadMetricsEnabled = app.settingsRepository.wycdnDownloadMetricsEnabled.value

            // Stop the service
            wycdn.unbindService()

            // Set common configuration
            wycdn.setConfigProperty("wycdn.agent.peer_id", peerId)
            wycdn.setConfigProperties(wycdnConfig)

            // Set environment configuration
            wycdn.setConfigProperties(wycdnEnv.config)

            // Set the download metrics enabled property
            wycdn.setConfigProperty(
                "wycdn.metrics.debug.send_download_metrics",
                if (wycdnDownloadMetricsEnabled) 1 else 0
            )

            // Start the service
            wycdn.bindService()
        }
    }

    /**
     * Updates the WyCDN mode at all configuration levels
     */
    fun updateWycdnMode(mode: String) {
        viewModelScope.launch {
            _wycdnMode.value = mode
            wycdn.setMode(mode)
            // Update settings repository to keep UI in sync
            app.settingsRepository.setWycdnMode(mode)
        }
    }

    fun updateWycdnLogLevel(logLevel: String) {
        Log.d(TAG, "Updating WyCDN log level to $logLevel")
        wycdn.setLogLevel(logLevel)
    }

    fun updatePlayerResolutionInfo(width: Int, height: Int) {
        wycdn.setPlayerResolutionInfo(width, height)
    }

    companion object {
        /**
         * A factory for creating instances of [WycdnViewModel] with required dependencies.
         *
         * To use this factory, pass it as an argument to the `viewModel()` function in a Composable context.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            // We use the implementation suggested from:
            // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories#creationextras
            initializer {
                WycdnViewModel(this[APPLICATION_KEY] as SampleApp)
            }
        }
    }
}
