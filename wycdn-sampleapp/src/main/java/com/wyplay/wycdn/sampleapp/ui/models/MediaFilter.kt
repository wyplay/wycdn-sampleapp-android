/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import androidx.media3.common.MediaItem

/**
 * Filters offered as tabs on the channel list.
 *
 * Matching is done against the "format" metadata extra set on each [MediaItem] by [MediaRepository]
 * (see the V1P/V1F/V2P/V2F/CDN values produced there).
 *
 * @property label Human-readable label shown on the tab.
 * @property format The "format" extra a channel must have to match, or null for [ALL] (matches all).
 */
enum class MediaFilter(val label: String, val format: String?) {
    ALL("All", null),
    F2F_PROXY_V1("F2F proxy v1", "V1P"),
    F2F_FETCH_V1("F2F fetch v1", "V1F"),
    F2F_PROXY_V2("F2F proxy v2", "V2P"),
    F2F_FETCH_V2("F2F fetch v2", "V2F"),
    CDN("CDN", "CDN");

    /** Returns true if [mediaItem] belongs to this filter. */
    fun matches(mediaItem: MediaItem): Boolean {
        if (format == null) return true
        val itemFormat = mediaItem.mediaMetadata.extras?.getString("format")?.uppercase()
        return itemFormat == format
    }
}

/** Returns the media items matching [filter], preserving order. */
fun List<MediaItem>.filterBy(filter: MediaFilter): List<MediaItem> =
    filter(filter::matches)

/**
 * Returns the filters to show as tabs for this media list: [MediaFilter.ALL] is always present,
 * and each format-specific filter is included only if at least one item matches it (empty tabs are
 * hidden).
 */
fun List<MediaItem>.availableFilters(): List<MediaFilter> =
    MediaFilter.entries.filter { it == MediaFilter.ALL || any(it::matches) }
