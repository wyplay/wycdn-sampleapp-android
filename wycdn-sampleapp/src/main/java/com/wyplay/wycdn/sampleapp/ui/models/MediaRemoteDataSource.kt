package com.wyplay.wycdn.sampleapp.ui.models

import android.text.Html
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private const val TAG = "MediaRemoteDataSource"

/**
 * A data source for fetching media list from a remote location.
 *
 * Note: See `src/main/assets/channels.json` for an example of the media list format.
 *
 * @property url The URL of the media list to fetch (ie: https://server/channels.json).
 *               If the URL is null, remote fetching is disabled.
 */
class MediaRemoteDataSource(private val url: String?) : MediaDataSource {

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
        if (url.isNullOrEmpty())
            return mutableListOf<MediaItem>()

        // Fetch the JSON
        val channelsJson = JSONObject(fetch(url)).getJSONArray("channels")

        // Build a list of MediaItem
        val mediaItems = mutableListOf<MediaItem>()
        for (i in 0 until channelsJson.length()) {
            val channelJson = channelsJson.getJSONObject(i)
            val channelId = channelJson.getString("id")
            val channelManifestUri = channelJson.getString("manifest")
            val channelName = Html.fromHtml(channelJson.getString("name"), Html.FROM_HTML_MODE_COMPACT).toString()
            val channelUri = when {
                channelManifestUri.endsWith(".mpd") || channelManifestUri.endsWith("manifest")
                        || channelManifestUri.endsWith(".m3u8") || channelManifestUri.endsWith(".mp3") -> {
                    channelManifestUri.replace("http://", "https://")
                }
                else -> {
                    Log.w(TAG,"Media: $channelName => Unsupported manifest URI: $channelManifestUri")
                    continue
                }
            }

            Log.d(TAG,"Media: $channelName => $channelUri")

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
     * Fetches the content from the specified URL.
     *
     * This function establishes an HTTPS connection to the provided URL,
     * using a pre-configured SSL context for secure communication.
     * It retrieves the content as a string.
     *
     * @param url The URL to fetch the content from.
     * @return The content retrieved from the URL as a string.
     * @throws IOException If an I/O error occurs while reading from the URL.
     * @throws MalformedURLException If the provided URL is not valid.
     * @throws SSLException If an SSL error occurs during the connection.
     */
    private fun fetch(url: String): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}