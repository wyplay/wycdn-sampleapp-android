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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * A [Flow] of [WycdnEnv] representing the current WyCDN environment setting. This flow emits
     * the current environment value stored in the settings, allowing observers to react to changes.
     *
     * If the environment value does not exist or is invalid, [WycdnEnv.default] is emitted as a fallback.
     */
    val wycdnEnvironment: Flow<WycdnEnv> = dataStore.data.map { preferences ->
        enumValueOrNull<WycdnEnv>(preferences[WycdnEnv.preferenceKey]) ?: WycdnEnv.default
    }

    /**
     * Updates the WyCDN environment setting.
     *
     * @param env The [WycdnEnv] value to be stored as the new environment setting.
     */
    suspend fun setWycdnEnvironment(env: WycdnEnv) {
        dataStore.edit { preferences ->
            preferences[WycdnEnv.preferenceKey] = env.name
        }
    }
}

/**
 * Defines the environment configurations for the WyCDN service.
 *
 * @property label Descriptive name of the environment.
 * @property bootstrapHostname Hostname of the bootstrap node.
 * @property stunHostname Hostname of the STUN server.
 * @property influxdbHostname Hostname of the InfluxDB Telegraf endpoint.
 * @property graylogHostname Hostname of the Graylog endpoint.
 */
enum class WycdnEnv(val label: String,
                    val bootstrapHostname: String,
                    val stunHostname: String,
                    val influxdbHostname: String,
                    val graylogHostname: String) {
    PUBLIC("Public (qa0)",
        "node0.qa0.wycdn.wyplay.com",
        "stun.qa0.wycdn.wyplay.com",
        "telegraf.qa0.wycdn.wyplay.com",
        "graylog.qa0.wycdn.wyplay.com");

    companion object {
        /**
         * Default value used when no WyCDN environment setting is stored.
         * The default is `PUBLIC`.
         */
        val default = PUBLIC

        /**
         * [Preferences.Key] used to store and retrieve the WyCDN environment setting.
         */
        val preferenceKey = stringPreferencesKey("wycdn_environment")
    }
}

/**
 * Retrieves an enum value of type [T] by its name, ignoring case sensitivity.
 *
 * @param T The enum class from which the value is to be retrieved. This can be inferred from the call.
 * @param name The name of the enum constant to be retrieved. This can be `null`, in which case `null` will be immediately returned.
 * @return The enum constant of type [T] if a match is found; `null` otherwise.
 */
inline fun <reified T : Enum<T>> enumValueOrNull(name: String?): T? {
    return T::class.java.enumConstants?.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
