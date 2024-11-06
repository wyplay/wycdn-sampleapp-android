/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.data

import androidx.media3.common.TrackGroup

data class TrackInfo(
    val mediaTrackGroup: TrackGroup,
    val index: Int,
    val height: Int,
    val width: Int
)
