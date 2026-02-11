/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.wyplay.wycdn.sampleapp.R
import com.wyplay.wycdn.sampleapp.ui.models.MediaListState
import kotlinx.coroutines.android.awaitFrame

/**
 * Media chooser screen allowing to select a media item from a list.
 *
 * @param mediaListState State of the media list, encapsulating whether the media list is loading,
 *                       has encountered an error, or is ready for display.
 * @param onMediaIndexSelected Action to be taken when a media item is selected from the list.
 * @param peerId Peer ID to display in the screen title.
 * @param currentWycdnEnvLabel Current WyCDN environment label to display in the screen title.
 * @param modifier An optional [Modifier] for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaChooserScreen(
    mediaListState: MediaListState,
    mediaIndex: Int,
    onMediaIndexSelected: (Int) -> Unit,
    peerId: String,
    currentWycdnEnvName: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    stringResource(
                        R.string.title_media_chooser_screen,
                        peerId,
                        currentWycdnEnvName
                    )
                )
            })
        },
        modifier = modifier
    ) { innerPadding ->
        when (mediaListState) {
            is MediaListState.Loading -> {
                // Show loading indicator
                LoadingMessage(Modifier.padding(innerPadding))
            }

            is MediaListState.Error -> {
                // Error occurred
                ErrorMessage(mediaListState.e, Modifier.padding(innerPadding))
            }

            is MediaListState.Ready -> {
                // Show media list
                MediaList(
                    mediaList = mediaListState.mediaList,
                    mediaIndex = mediaIndex,
                    onMediaIndexSelected = onMediaIndexSelected,
                    Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun LoadingMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.padding_small)))
        Text(stringResource(R.string.msg_fetching_media_list))
    }
}

@Composable
private fun ErrorMessage(e: Exception, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.msg_error, e.message ?: ""))
    }
}

@Composable
private fun MediaList(
    mediaList: List<MediaItem>,
    mediaIndex: Int,
    onMediaIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val focusRequesters = remember(mediaList.size) {
        List(mediaList.size) { FocusRequester() }
    }

    var focusedIndex by remember {
        mutableStateOf(-1)
    }

    LaunchedEffect(mediaIndex) {
        if (mediaIndex in mediaList.indices) {
            listState.scrollToItem(mediaIndex)
            awaitFrame()
            focusRequesters[mediaIndex].requestFocus()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(mediaList) { index, mediaItem ->
            val backgroundColor = if (index == focusedIndex) MaterialTheme.colorScheme.primary
            else Transparent

            val textColor = if (index == focusedIndex) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedIndex = index
                        }
                    }
                    .focusable()
            ) {
                Text(
                    text = mediaItem.mediaMetadata.title.toString(),
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.padding_medium))
                        .clickable { onMediaIndexSelected(index) },
                    color = textColor
                )
            }
        }
    }
}

@Preview
@Composable
private fun MediaChooserScreenLoadingPreview() {
    MediaChooserScreenPreview(MediaListState.Loading)
}

@Preview
@Composable
private fun MediaChooserScreenErrorPreview() {
    MediaChooserScreenPreview(MediaListState.Error(Exception("Message")))
}

@Preview
@Composable
private fun MediaChooserScreenMediaListPreview() {
    val mediaList = mutableListOf<MediaItem>()
    for (i in 1..10) {
        val mediaItem = MediaItem.Builder()
            .setMediaId("media_id_$i")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Title $i")
                    .build()
            )
            .build()
        mediaList.add(mediaItem)
    }
    MediaChooserScreenPreview(MediaListState.Ready(mediaList))
}

@Composable
private fun MediaChooserScreenPreview(
    mediaListState: MediaListState
) {
    MediaChooserScreen(
        mediaListState = mediaListState,
        mediaIndex = -1,
        onMediaIndexSelected = { },
        peerId = "generic-123abc",
        currentWycdnEnvName = "Default"
    )
}
