/*
 * Copyright (C) 2026 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.wyplay.wycdn.WycdnDownloadClient

@UnstableApi
class WycdnMediaDataSourceFactory(
    private val client: WycdnDownloadClient
) : DataSource.Factory {

    override fun createDataSource(): DataSource {
        return WycdnMediaDataSource(client)
    }
}