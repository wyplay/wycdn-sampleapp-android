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
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "MediaBuiltinDataSource"

/**
 * A data source for fetching media list from assets.
 *
 * Note: See `src/main/assets/channels.json` for the media list content.
 *
 * @property assets The [AssetManager] used to access the application's asset files.
 */
class MediaBuiltinDataSource(private val assets: AssetManager) : MediaDataSource {

    override suspend fun fetchMediaList(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            return@withContext buildMediaList()
        } catch (e: Exception) {
            throw MediaDataSourceException("Cannot fetch the media list (${e.message})", e)
        }
    }

    /**
     * Fetches the media list JSON and parse a list of [MediaItem] from it.
     *
     * @return A list of [MediaItem] objects.
     */
    private fun buildMediaList(): List<MediaItem> {
        // Fetch the JSON
        val fileName = "channels.json"
        val channelsJson = JSONObject(fetch(fileName)).getJSONArray("channels")

        // Build a list of MediaItem
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until channelsJson.length()) {
            val channelJson = channelsJson.getJSONObject(i)
            val channelId = channelJson.getString("id")
            val channelUri = channelJson.getString("manifest")
            val channelFormat = channelJson.optString("format", "cdn")
            val channelName =
                when (Build.VERSION.SDK_INT) {
                    in 1..24 -> Html.fromHtml(channelJson.getString("name")).toString()
                    else -> Html.fromHtml(channelJson.getString("name"), Html.FROM_HTML_MODE_COMPACT).toString()
                }

            Log.d(TAG, "Media: $channelName => $channelUri")

            val mediaItem = MediaItem.Builder()
                .setMediaId(channelId)
                .setUri(channelUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channelName)
                        .setExtras(Bundle().apply {
                            putString("format", channelFormat)
                        })
                        .build()
                )
                .build()
            mediaItems.add(mediaItem)
        }

        return mediaItems
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