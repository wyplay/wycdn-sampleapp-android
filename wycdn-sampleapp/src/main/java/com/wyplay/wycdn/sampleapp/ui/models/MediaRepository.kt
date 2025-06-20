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
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

// See: https://developer.android.com/topic/architecture/data-layer

/**
 * A repository for managing media.
 *
 * @property mediaBuiltinDataSource The [MediaDataSource] instance used to fetch a media list from assets.
 * @property mediaRemoteDataSource The [MediaDataSource] instance used to fetch a media list from a remote location.
 */
class MediaRepository(private val mediaBuiltinDataSource: MediaDataSource, private val mediaRemoteDataSource: MediaDataSource) {

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

        // Duplicate media item for CDN and WyCDN accesses
        val mediaList = mutableListOf<MediaItem>()
        for (mediaItem in fetchedMediaList) {
            val mediaId = mediaItem.mediaId
            val mediaUri = mediaItem.localConfiguration!!.uri
            val mediaTitle = mediaItem.mediaMetadata.title.toString()
            val mediaFormat = mediaItem.mediaMetadata.extras?.getString("format")?.uppercase()

            val directMediaItem = MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(mediaUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("$mediaTitle ($mediaFormat)")
                        .build()
                )
                .build()
            mediaList.add(directMediaItem)

            if (mediaFormat == "CDN") {
                // Convert to V0
                val wycdnMediaItem = MediaItem.Builder()
                    .setMediaId(mediaId)
                    .setUri(toWycdnURI(mediaUri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("$mediaTitle (V0)")
                            .build()
                    )
                    .build()
                mediaList.add(wycdnMediaItem)
            }
        }

        return mediaList
    }

    /**
     * Converts an URI to a WyCDN URI.
     *
     * @param uri The original URI.
     * @return The converted URI.
     */
    private fun toWycdnURI(uri: Uri): Uri {
        return Uri.Builder()
            .scheme("http")
            .encodedAuthority("127.0.0.1:8000")
            .encodedPath("/wycdn/https/${uri.host}${uri.encodedPath}")
            .encodedQuery(uri.query)
            .build()
    }
}
