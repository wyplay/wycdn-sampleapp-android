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
 * A data source for fetching WyCDN environment list from assets.
 *
 * Note: See `src/main/assets/environments.json` for the environment list content.
 *
 * @property assets The [AssetManager] used to access the application's asset files.
 */
class WycdnEnvDataSource(private val assets: AssetManager) {

    /**
     * Gets the WyCDN environment list.
     *
     * @return A [WycdnEnvList] containing the list of environment configurations and the default environment.
     * @throws WycdnEnvDataSourceException If there is an error fetching the environment list.
     */
    fun getEnvList(): WycdnEnvList {
        try {
            return buildEnvList()
        } catch (e: Exception) {
            throw WycdnEnvDataSourceException("Cannot fetch the environment list (${e.message})", e)
        }
    }

    /**
     * Fetches the environment list JSON and parse [WycdnEnvList] from it.
     *
     * @return The built [WycdnEnvList] instance.
     * @throws JSONException if there is an error in parsing the JSON data.
     * @throws NoSuchElementException if the default environment is not found in the list.
     */
    private fun buildEnvList(): WycdnEnvList {
        // Fetch the JSON
        val fileName = "environments.json"
        val envJson = JSONObject(fetch(fileName))

        // Build the list of environments
        val envArray = envJson.getJSONArray("environments")
        val envConfigList = mutableListOf<WycdnEnv>()
        for (i in 0 until envArray.length()) {
            val jsonObject = envArray.getJSONObject(i)
            val config = WycdnEnv(
                id = jsonObject.getString("id"),
                name = jsonObject.getString("name"),
                bootstrapHostname = jsonObject.getString("bootstrapHostname"),
                bootstrapPort = if (jsonObject.has("bootstrapPort")) jsonObject.getString("bootstrapPort") else null,
                customerId = if (jsonObject.has("CustomerId")) jsonObject.getString("CustomerId") else null,
                stunHostname = jsonObject.getString("stunHostname"),
                influxdbHostname = jsonObject.getString("influxdbHostname"),
                graylogHostname = jsonObject.getString("graylogHostname"),
                remoteConfigHostname = jsonObject.getString("remoteConfigHostname"),
                remoteConfigPeriodSec = jsonObject.getString("remoteConfigPeriodSec"),
            )
            envConfigList.add(config)
        }

        // Get default environment
        val defaultEnvId = envJson.getString("default")
        val defaultEnv = envConfigList.firstOrNull { it.id == defaultEnvId }
            ?: throw NoSuchElementException("Default environment with id \"$defaultEnvId\" not found")

        return WycdnEnvList(envConfigList, defaultEnv)
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
}

/**
 * Represents a list of environment configurations for the WyCDN service along with the default environment.
 *
 * @property envList The list of environment configurations.
 * @property defaultEnv The default environment configuration.
 */
data class WycdnEnvList(
    val envList: List<WycdnEnv>,
    val defaultEnv: WycdnEnv
)

/**
 * Defines an environment configuration for the WyCDN service.
 *
 * @property id Identifier of the environment.
 * @property name Descriptive name of the environment.
 * @property bootstrapHostname Hostname of the bootstrap node.
 * @property bootstrapPort Port of the bootstrap node.
 * @property customerId Customer ID of the environment.
 * @property stunHostname Hostname of the STUN server.
 * @property influxdbHostname Hostname of the InfluxDB Telegraf endpoint.
 * @property graylogHostname Hostname of the Graylog endpoint.
 * @property remoteConfigHostname Hostname of the remote config server.
 * @property remoteConfigPeriodSec Period of the remote config refresh in seconds (0 to disable).
 */
data class WycdnEnv(
    val id: String,
    val name: String,
    val bootstrapHostname: String,
    val bootstrapPort: String?,
    val customerId: String?,
    val stunHostname: String,
    val influxdbHostname: String,
    val graylogHostname: String,
    val remoteConfigHostname: String,
    val remoteConfigPeriodSec: String,
)

/**
 * Exception thrown when there is an error related to the [WycdnEnvDataSource].
 */
class WycdnEnvDataSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)
