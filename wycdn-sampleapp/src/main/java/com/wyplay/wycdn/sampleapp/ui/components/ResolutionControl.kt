/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.components

import androidx.annotation.OptIn
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.wyplay.wycdn.sampleapp.ui.data.TrackInfo

@OptIn(UnstableApi::class)
class ResolutionControl(private val trackSelector: DefaultTrackSelector) {

    // Convert resolution to track info
    fun setTrackByResolution(
        height: Int,
        width: Int,
        trackInfoList: MutableList<TrackInfo>?
    ): DefaultTrackSelector.Parameters? {
        for (trackInfo in trackInfoList!!) {
            if (trackInfo.height == height && trackInfo.width == width) {
                return forceThisTrack(trackInfo.mediaTrackGroup, trackInfo.index)
            }
        }
        return null
    }

    // Set the X resolution
    private fun forceThisTrack(
        mediaTrackGroup: TrackGroup,
        index: Int
    ): DefaultTrackSelector.Parameters {
        return trackSelector.buildUponParameters()
            .setOverrideForType(TrackSelectionOverride(mediaTrackGroup, index))
            .build()
    }

    // Auto resolution mode
    fun setAutoResolution(): DefaultTrackSelector.Parameters {
        return trackSelector.buildUponParameters().build()
    }

}