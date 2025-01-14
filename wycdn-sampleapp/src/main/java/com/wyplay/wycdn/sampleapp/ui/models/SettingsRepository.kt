/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.SupervisorJob

// See: https://developer.android.com/topic/libraries/architecture/datastore

/**
 * A property extension on [Context] to easily access the shared [DataStore] of [Preferences].
 *
 * @receiver Context The context from which the DataStore is accessed, typically an Activity or Application context.
 * @return [DataStore<Preferences>] An instance of DataStore configured for preferences.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * A repository for managing application settings.
 *
 * @property dataStore The [DataStore] instance used to persist and retrieve settings.
 * This DataStore is expected to be retrieved from [Context.dataStore].
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val wycdnEnvDataSource: WycdnEnvDataSource
) {

    /** WyCDN environment list. */
    val wycdnEnvironmentList: WycdnEnvList by lazy {
        wycdnEnvDataSource.getEnvList()
    }

    // Backing property for the WyCDN environment setting, initially set to the default env.
    private val _wycdnEnvironment = MutableStateFlow(wycdnEnvironmentList.defaultEnv)

    /**
     * A [Flow] of [WycdnEnv] representing the current WyCDN environment setting. This flow emits
     * the current environment value stored in the settings, allowing observers to react to changes.
     *
     * If the environment value does not exist or is invalid, a default value is emitted as a fallback.
     */
    val wycdnEnvironment: StateFlow<WycdnEnv> = _wycdnEnvironment.asStateFlow()

    /**
     * Updates the WyCDN environment setting.
     *
     * @param env The [WycdnEnv] value to be stored as the new environment setting.
     */
    suspend fun setWycdnEnvironment(env: WycdnEnv) {
        _wycdnEnvironment.value = env
        dataStore.edit { preferences ->
            preferences[WYCDN_ENVIRONMENT_KEY] = env.id
        }
    }

    // Backing property for the WyCDN download metrics enabled setting, initially set to false.
    private val _wycdnDownloadMetricsEnabled = MutableStateFlow(false)

    /**
     * A [Flow] of Boolean representing whether to enable WyCDN download metrics. This flow emits
     * the current value stored in the settings, allowing observers to react to changes.
     *
     * If the value does not exist, false is emitted as a fallback.
     */
    val wycdnDownloadMetricsEnabled: StateFlow<Boolean> = _wycdnDownloadMetricsEnabled.asStateFlow()

    /**
     * Updates the WyCDN download metrics enabled setting.
     *
     * @param enable The Boolean value to be stored as the new setting.
     */
    suspend fun setWycdnDownloadMetricsEnabled(enable: Boolean) {
        _wycdnDownloadMetricsEnabled.value = enable
        dataStore.edit { preferences ->
            preferences[WYCDN_DOWNLOAD_METRICS_ENABLED] = enable
        }
    }

    // Backing property for the WyCDN debug info enabled setting, initially set to false.
    private val _wycdnDebugInfoEnabled = MutableStateFlow(false)

    /**
     * A [Flow] of Boolean representing whether to enable WyCDN debug info. This flow emits
     * the current value stored in the settings, allowing observers to react to changes.
     *
     * If the value does not exist, false is emitted as a fallback.
     */
    val wycdnDebugInfoEnabled: StateFlow<Boolean> = _wycdnDebugInfoEnabled.asStateFlow()

    /**
     * Updates the WyCDN debug info enabled setting.
     *
     * @param enable The Boolean value to be stored as the new setting.
     */
    suspend fun setWycdnDebugInfoEnabled(enable: Boolean) {
        _wycdnDebugInfoEnabled.value = enable
        dataStore.edit { preferences ->
            preferences[WYCDN_DEBUG_INFO_ENABLED_KEY] = enable
        }
    }

    // Backing property for the WyCDN debug menu enabled setting, initially set to false.
    private val _wycdnDebugMenuEnabled = MutableStateFlow(false)

    /**
     * A [Flow] of Boolean representing whether to enable WyCDN debug menu. This flow emits
     */
    val wycdnDebugMenuEnabled: Flow<Boolean> = _wycdnDebugMenuEnabled.asStateFlow()

    /**
     * Updates the WyCDN debug menu enabled setting.
     *
     * @param enable The Boolean value to be stored as the new setting.
     */
    suspend fun setWycdnDebugMenuEnabled(enable: Boolean) {
        _wycdnDebugMenuEnabled.value = enable
        dataStore.edit { preferences ->
            preferences[WYCDN_DEBUG_MENU_ENABLED_KEY] = enable
        }
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _wycdnMode = MutableStateFlow("full")
    /**
     * A [Flow] of String representing the WyCDN mode. This flow emits
     */
    val wycdnMode: StateFlow<String> = _wycdnMode.asStateFlow()

    /**
     * Updates the WyCDN mode setting.
     * Note: This will persist the mode but it will reset to "full" on app restart
     *
     * @param mode The String value to be stored as the new setting.
     */
    suspend fun setWycdnMode(mode: String) {
        _wycdnMode.value = mode
        dataStore.edit { preferences ->
            preferences[WYCDN_MODE_KEY] = mode
        }
    }

    /**
     * Forces reset to default "full" mode
     */
    suspend fun resetToDefaultMode() {
        setWycdnMode("full")
    }

    private val _wycdnLogLevel = MutableStateFlow("info")
    /**
     * A [Flow] of String representing the WyCDN log level. This flow emits
     */
    val wycdnLogLevel: StateFlow<String> = _wycdnLogLevel.asStateFlow()

    /**
     * Updates the WyCDN log level setting.
     *
     * @param logLevel The String value to be stored as the new setting.
     */
    suspend fun setWycdnLogLevel(logLevel: String) {
        _wycdnLogLevel.value = logLevel
        dataStore.edit { preferences ->
            preferences[WYCDN_LOG_LEVEL_KEY] = logLevel
        }
    }

    init {
        // Load settings from the DataStore when application starts
        coroutineScope.launch {
            val preferences = dataStore.data.first()
            
            // Don't load the saved mode, always start with "full"
            _wycdnMode.value = "full"
            
            // Load other preferences...
            preferences[WYCDN_ENVIRONMENT_KEY]?.let { envId ->
                _wycdnEnvironment.value = wycdnEnvironmentList.envList.firstOrNull { it.id == envId }
                    ?: wycdnEnvironmentList.defaultEnv
            }
            preferences[WYCDN_DOWNLOAD_METRICS_ENABLED]?.let { enabled ->
                _wycdnDownloadMetricsEnabled.value = enabled
            }
            preferences[WYCDN_DEBUG_INFO_ENABLED_KEY]?.let { enabled ->
                _wycdnDebugInfoEnabled.value = enabled
            }
            preferences[WYCDN_DEBUG_MENU_ENABLED_KEY]?.let { enabled ->
                _wycdnDebugMenuEnabled.value = enabled
            }
            preferences[WYCDN_MODE_KEY]?.let { mode ->
                _wycdnMode.value = mode
            }
            preferences[WYCDN_LOG_LEVEL_KEY]?.let { logLevel ->
                _wycdnLogLevel.value = logLevel
            }
        }
    }

    companion object {
        /** [Preferences.Key] used to store and retrieve the WyCDN environment setting. */
        internal val WYCDN_ENVIRONMENT_KEY = stringPreferencesKey("wycdn_environment")

        /** [Preferences.Key] used to store and retrieve the WyCDN download metrics enabled setting. */
        internal val WYCDN_DOWNLOAD_METRICS_ENABLED = booleanPreferencesKey("wycdn_download_metrics_enabled")

        /** [Preferences.Key] used to store and retrieve the WyCDN debug info enabled setting. */
        internal val WYCDN_DEBUG_INFO_ENABLED_KEY = booleanPreferencesKey("wycdn_debug_info_enabled")

        /** [Preferences.Key] used to store and retrieve the WyCDN debug menu enabled setting. */
        internal val WYCDN_DEBUG_MENU_ENABLED_KEY = booleanPreferencesKey("wycdn_debug_menu_enabled")

        /** [Preferences.Key] used to store and retrieve the WyCDN mode setting. */
        internal val WYCDN_MODE_KEY = stringPreferencesKey("wycdn_mode")

        /** [Preferences.Key] used to store and retrieve the WyCDN log level setting. */
        internal val WYCDN_LOG_LEVEL_KEY = stringPreferencesKey("wycdn_log_level")
    }
}
