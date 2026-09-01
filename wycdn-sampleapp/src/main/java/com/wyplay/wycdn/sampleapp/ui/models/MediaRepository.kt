/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONObject
import java.net.URLEncoder

// See: https://developer.android.com/topic/architecture/data-layer

/**
 * A repository for managing media.
 *
 * @property mediaBuiltinDataSource The [MediaDataSource] instance used to fetch a media list from assets.
 * @property mediaRemoteDataSource The [MediaDataSource] instance used to fetch a media list from a remote location.
 * @property settings The [AppSettings] instance used to know which channels must be included to the lineup.
 */
class MediaRepository(
    private val mediaBuiltinDataSource: MediaDataSource,
    private val mediaRemoteDataSource: MediaDataSource,
    private val settings: AppSettings
) {

    /**
     * Fetches a list of media items from the data source.
     *
     * @return A list of [MediaItem] objects.
     * @throws MediaDataSourceException If there is an error fetching the media list.
     */
    suspend fun fetchMediaList(): List<MediaItem> {
        // Fetch the remote media list
        var fetchedMediaList = mediaRemoteDataSource.fetchMediaList()

        // If empty, fallback on the builtin media list
        if (fetchedMediaList.isEmpty())
            fetchedMediaList = mediaBuiltinDataSource.fetchMediaList()

        return fetchedMediaList.flatMap(::buildChannelVariants)
    }

    /**
     * Builds all variants of a channel (CDN/V1F/V1P/etc)
     *
     * @param mediaItem Original mediaItem
     * @return A list of [MediaItem] objects.
     **/
    private fun buildChannelVariants(mediaItem: MediaItem): List<MediaItem> {
        val channelId = mediaItem.mediaId
        val mediaUri = mediaItem.localConfiguration!!.uri
        val sourceFormat = mediaItem.mediaMetadata.extras
            ?.getString("format")?.uppercase().orEmpty().ifBlank { "CDN" }
        val mediaType = mediaItem.mediaMetadata.extras?.getString("type")
        val mediaTitle = mediaItem.mediaMetadata.title?.toString().orEmpty().let { title ->
            if (!mediaType.isNullOrEmpty()) "$title ($mediaType)" else title
        }

        val variants = buildList {
            when (sourceFormat) {
                "CDN" -> {
                    if (settings.includeChannelsCdn)
                        add(ChannelVariant("CDN", "CDN", mediaUri))
                    if (settings.includeChannelsV1)
                        addWycdnV1Variants(toWycdnUriV1(mediaUri, channelId))
                    if (settings.includeChannelsV2)
                        addWycdnV2Variants(toWycdnUriV2(mediaUri, channelId))
                }
                "V1" -> if (settings.includeChannelsV1) {
                    addWycdnV1Variants(mediaUri)
                }
                "V2" -> if (settings.includeChannelsV2) {
                    addWycdnV2Variants(mediaUri)
                }
            }
        }

        return variants.map { variant ->
            val title = if (settings.showsChannelsType)
                "$mediaTitle (${variant.title})"
            else
                mediaTitle

            MediaItem.Builder()
                .setMediaId("$channelId-${variant.format}")
                .setUri(variant.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setExtras(Bundle().apply {
                            putString("format", variant.format)
                            putString("type", mediaType)
                        })
                        .build()
                )
                .build()
        }
    }

    private data class ChannelVariant(
        val format: String,
        val title: String,
        val uri: Uri
    )

    /**
     * Adds the Proxy/Fetch variants for a WyCDN V1 URI.
     **/
    private fun MutableList<ChannelVariant>.addWycdnV1Variants(uri: Uri) {
        if (settings.includeChannelsProxy)
            add(ChannelVariant("V1P", "V1 - Proxy", uri))
        if (settings.includeChannelsFetch)
            add(ChannelVariant("V1F", "V1 - Fetch", uri))
    }

    /**
     * Adds the Proxy/Fetch variants for a WyCDN V2 URI.
     **/
    private fun MutableList<ChannelVariant>.addWycdnV2Variants(uri: Uri) {
        if (settings.includeChannelsProxy)
            add(ChannelVariant("V2P", "V2 - Proxy", uri))
        if (settings.includeChannelsFetch)
            add(ChannelVariant("V2F", "V2 - Fetch", uri))
    }

    /**
     * Converts an URI to a WyCDN v1 URI.
     *
     * @param uri The original URI.
     * @return The converted URI.
     */
    private fun toWycdnUriV1(uri: Uri, channelId: String, mode: String = "auto"): Uri {
        val port = if (uri.port > 0) uri.port else if (uri.scheme == "http") 80 else 443

        val baseUri = Uri.Builder()
            .scheme(uri.scheme)
            .encodedAuthority("${uri.host}:${port}")
            .build()

        val originalPath = uri.encodedPath
        val encodedBaseUrl = URLEncoder.encode(baseUri.toString(), "UTF-8")

        return Uri.Builder()
            .scheme("http")
            .encodedAuthority("127.0.0.1:8000")
            .encodedPath("/wycdn/v1/get/${channelId}/${mode}/${encodedBaseUrl}${originalPath}")
            .encodedQuery(uri.query)
            .build()
    }

    /**
     * Converts an URI to a WyCDN v2 URI.
     *
     * @param uri The original URI.
     * @return The converted URI.
     */
    private fun toWycdnUriV2(uri: Uri, channelId: String, mode: String = "auto"): Uri {
        val port = if (uri.port > 0) uri.port else if (uri.scheme == "http") 80 else 443

        val baseUri = Uri.Builder()
            .scheme(uri.scheme)
            .encodedAuthority("${uri.host}:${port}")
            .build()

        val extraParams = JSONObject()
            .put("mode", mode)
            .toString()

        val originalPath = uri.encodedPath
        val encodedBaseUrl = URLEncoder.encode(baseUri.toString(), "UTF-8")
        val encodedChannelId = URLEncoder.encode(channelId, "UTF-8")
        val encodedExtraParams = URLEncoder.encode(extraParams, "UTF-8")

        return Uri.Builder()
            .scheme("http")
            .encodedAuthority("127.0.0.1:8000")
            .encodedPath("/wycdn/v2/get/${encodedChannelId}/${encodedExtraParams}/${encodedBaseUrl}${originalPath}")
            .encodedQuery(uri.query)
            .build()
    }
}
