/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wyplay.wycdn.sampleapp.SampleApp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel providing access to the application settings for the UI.
 *
 * @property repository The [SettingsRepository] instance accessed by this ViewModel for settings data operations.
 * @companion Factory Provides a factory for creating [SettingsViewModel] instances, encapsulating dependency injection.
 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    /**
     * Gets the application configuration.
     **/
    val appConfig: AppConfig = repository.appConfig

    /**
     * Gets the environment to use for WyCDN configuration.
     */
    val wycdnEnvironment: StateFlow<WycdnEnv> = repository.wycdnEnvironment

    /**
     * Sets the environment to use for WyCDN configuration.
     *
     * @param env The new environment value to set.
     */
    fun setWycdnEnvironment(env: WycdnEnv) {
        viewModelScope.launch {
            repository.setWycdnEnvironment(env)
        }
    }

    /**
     * Gets the current value of whether WyCDN download metrics is enabled.
     */
    val wycdnDownloadMetricsEnabled: StateFlow<Boolean> = repository.wycdnDownloadMetricsEnabled

    /**
     * Sets the value of whether WyCDN download metrics is enabled.
     *
     * @param enable The new Boolean value to set.
     */
    fun setWycdnDownloadMetricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setWycdnDownloadMetricsEnabled(enabled)
        }
    }

    /**
     * Gets the current value of whether channel URLs are shown in the channel list.
     */
    val showsChannelsUrls: StateFlow<Boolean> = repository.showsChannelsUrls

    /**
     * Sets the value of whether channel URLs are shown in the channel list.
     *
     * @param show The new Boolean value to set.
     */
    fun setShowsChannelsUrls(show: Boolean) {
        viewModelScope.launch {
            repository.setShowsChannelsUrls(show)
        }
    }

    /**
     * Gets the current value of whether the current stream resolution is shown.
     */
    val showsStreamResolution: StateFlow<Boolean> = repository.showsStreamResolution

    /**
     * Sets the value of whether the current stream resolution is shown.
     *
     * @param show The new Boolean value to set.
     */
    fun setShowsStreamResolution(show: Boolean) {
        viewModelScope.launch {
            repository.setShowsStreamResolution(show)
        }
    }

    /** Expose Debug Menu state as a StateFlow */
    val debugMenuEnabled: StateFlow<Boolean> = repository.wycdnDebugMenuEnabled

    /**
     * Sets the value of whether the debug menu is enabled.
     *
     * @param enable The new Boolean value to set.
     */
    fun setDebugMenuEnabled(enable: Boolean) {
        viewModelScope.launch {
            repository.setWycdnDebugMenuEnabled(enable)
        }
    }

    /** Expose Mode state as a StateFlow */
    val wycdnMode: StateFlow<String> = repository.wycdnMode

    /**
     * Sets the value of the WyCDN mode.
     *
     * @param mode The new mode value to set.
     */
    fun setWycdnMode(mode: String) {
        viewModelScope.launch {
            repository.setWycdnMode(mode)
        }
    }

    /** Expose Log Level state as a StateFlow */
    val wycdnLogLevel: StateFlow<String> = repository.wycdnLogLevel

    /**
     * Sets the value of the WyCDN log level.
     *
     * @param logLevel The new log level value to set.
     */
    fun setWycdnLogLevel(logLevel: String) {
        viewModelScope.launch {
            repository.setWycdnLogLevel(logLevel)
        }
    }

    companion object {
        /**
         * A factory for creating instances of [SettingsViewModel] with required dependencies.
         *
         * To use this factory, pass it as an argument to the `viewModel()` function in a Composable context.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            // We use the implementation suggested from:
            // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories#creationextras
            initializer {
                val app = this[APPLICATION_KEY] as SampleApp
                SettingsViewModel(repository = app.settingsRepository)
            }
        }
    }
}
