/*
 * Copyright (C) 2026 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.wyplay.wycdn.WycdnDownloadClient
import com.wyplay.wycdn.WycdnDownloadRequest
import com.wyplay.wycdn.WycdnDownloadResponse
import java.io.IOException

@UnstableApi
class WycdnMediaDataSource(
    private val client: WycdnDownloadClient
) : BaseDataSource(true) {

    private var response: WycdnDownloadResponse? = null

    private fun getResponse(): WycdnDownloadResponse =
        checkNotNull(response) { "DataSource not opened yet" }

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)

        val request = WycdnDownloadRequest.Builder()
            .url(dataSpec.uri.toString())
            .headers(dataSpec.httpRequestHeaders)
            .build()

        response = client
            .newCall(request)
            .execute()

        transferStarted(dataSpec)

        if (!getResponse().isSuccessful()) {
            throw IOException("HTTP error code " + getResponse().code())
        }

        return getResponse().contentLength()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytesRead = getResponse().body().read(buffer, offset, length)
        if (bytesRead == -1) {
            return C.RESULT_END_OF_INPUT
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri = getResponse().url().toUri()

    override fun getResponseHeaders(): Map<String, List<String>> =
        getResponse().headers()
            .mapValues { (_, value) -> listOf(value) }

    override fun close() {
        response?.let {
            it.close()
            response = null
            transferEnded()
        }
    }
}