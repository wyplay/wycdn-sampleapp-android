/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.content.res.AssetManager
import org.json.JSONObject

/**
 * A data source for fetching the application configuration from assets.
 *
 * Note: See `src/main/assets/config.json` for the application configuration.
 *
 * @property assets The [AssetManager] used to access the application's asset files.
 */
class AppConfigDataSource(private val assets: AssetManager) {

    /**
     * Gets the application configuration.
     *
     * @return The [AppConfig] parsed from the application assets.
     * @throws AppConfigDataSourceException If the configuration cannot be fetched or parsed.
     */
    fun getAppConfig(): AppConfig {
        try {
            return buildAppConfig()
        } catch (e: Exception) {
            throw AppConfigDataSourceException("Cannot fetch the application configuration (${e.message})", e)
        }
    }

    /**
     * Fetches and parses the application configuration JSON.
     *
     * @return The built [AppConfig] instance.
     * @throws JSONException if there is an error in parsing the JSON data.
     * @throws NoSuchElementException if the default environment is not found in the list.
     */
    private fun buildAppConfig(): AppConfig {
        // Fetch the JSON
        val fileName = "config.json"
        val configJson = JSONObject(fetch(fileName))

        // Build the list of environments
        val environmentArray = configJson.getJSONArray(KEY_ENV_LIST)
        val environments = mutableListOf<WycdnEnv>()
        for (i in 0 until environmentArray.length()) {
            val jsonObject = environmentArray.getJSONObject(i)
            val environment = WycdnEnv(
                id = jsonObject.getString("id"),
                name = jsonObject.getString("name"),
                config = jsonObject.getJSONObject("config")
            )
            environments.add(environment)
        }

        // Get default environment
        val defaultEnvironmentId = configJson.getString(KEY_ENV_DEFAULT)
        val defaultEnvironment = environments.firstOrNull { it.id == defaultEnvironmentId }
            ?: throw NoSuchElementException(
                "Default environment with id \"$defaultEnvironmentId\" not found"
            )

        val wycdnConfig = configJson.getJSONObject(KEY_WYCDN_CONFIG)
        val settings = buildAppSettings(configJson.optJSONObject(KEY_APP_CONFIG) ?: JSONObject())

        return AppConfig(
            environments = environments,
            defaultEnvironment = defaultEnvironment,
            wycdnConfig = wycdnConfig,
            settings = settings
        )
    }

    /**
     * Builds application specific settings from the `appConfig` JSON section.
     *
     * @param configJson JSON object
     **/
    private fun buildAppSettings(configJson: JSONObject): AppSettings {
        return AppSettings(
            showsDebugMenu = configJson.optBoolean(KEY_SHOWS_DEBUG_MENU, false),
            showsStreamResolution = configJson.optBoolean(KEY_SHOWS_STREAM_RESOLUTION, true)
        )
    }

    /**
     * Fetches the JSON content from the specified file of the assets.
     *
     * This function reads the file content as a string.
     *
     * @param fileName The name of the file to fetch the content from.
     * @return The content retrieved from the file as a string.
     * @throws IOException If an I/O error occurs while reading from the file.
     */
    private fun fetch(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private companion object {
        const val KEY_ENV_DEFAULT = "default"
        const val KEY_ENV_LIST = "environments"
        const val KEY_APP_CONFIG = "appConfig"
        const val KEY_WYCDN_CONFIG = "config"
        const val KEY_SHOWS_DEBUG_MENU = "showsDebugMenu"
        const val KEY_SHOWS_STREAM_RESOLUTION = "showsStreamResolution"
    }
}

/**
 * Represents the complete application configuration loaded from `config.json`.
 *
 * @property environments The available WyCDN environments.
 * @property defaultEnvironment The default WyCDN environment.
 * @property wycdnConfig Common WyCDN properties.
 * @property settings Application settings.
 */
data class AppConfig(
    val environments: List<WycdnEnv>,
    val defaultEnvironment: WycdnEnv,
    val wycdnConfig: JSONObject,
    val settings: AppSettings
)

/**
 * Application settings loaded from the `appConfig` JSON section.
 *
 * @property showsDebugMenu Whether the debug menu is shown.
 * @property showsStreamResolution Whether the current stream resolution is shown.
 */
data class AppSettings(
    val showsDebugMenu: Boolean,
    val showsStreamResolution: Boolean
)

/**
 * Defines an environment configuration for the WyCDN service.
 *
 * @property id Identifier of the environment.
 * @property name Descriptive name of the environment.
 * @property config WyCDN properties for the environment.
 */
data class WycdnEnv(
    val id: String,
    val name: String,
    val config: JSONObject
)

/**
 * Exception thrown when there is an error related to the [AppConfigDataSource].
 */
class AppConfigDataSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
