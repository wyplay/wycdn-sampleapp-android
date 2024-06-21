package com.wyplay.wycdn.sampleapp.ui.models

import android.content.res.AssetManager
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
            throw MediaDataSourceException("Cannot fetch the media list", e)
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
            val channelManifestUri = channelJson.getString("manifest")
            val channelName = Html.fromHtml(
                channelJson.getString("name"),
                Html.FROM_HTML_MODE_COMPACT
            ).toString()
            val channelUri = when {
                channelManifestUri.endsWith(".mpd") || channelManifestUri.endsWith("manifest")
                        || channelManifestUri.endsWith(".m3u8") || channelManifestUri.endsWith(".mp3") -> {
                    channelManifestUri.replace("http://", "https://")
                }
                else -> {
                    Log.w(
                        TAG,
                        "Media: $channelName => Unsupported manifest URI: $channelManifestUri"
                    )
                    continue
                }
            }

            Log.d(TAG, "Media: $channelName => $channelUri")

            val mediaItem = MediaItem.Builder()
                .setMediaId(channelId)
                .setUri(channelUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channelName)
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