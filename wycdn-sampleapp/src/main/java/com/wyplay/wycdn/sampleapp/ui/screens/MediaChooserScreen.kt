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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.wyplay.wycdn.sampleapp.R
import com.wyplay.wycdn.sampleapp.ui.models.MediaFilter
import com.wyplay.wycdn.sampleapp.ui.models.MediaListState
import com.wyplay.wycdn.sampleapp.ui.models.availableFilters
import com.wyplay.wycdn.sampleapp.ui.models.filterBy
import com.wyplay.wycdn.sampleapp.ui.theme.ControlFocused
import com.wyplay.wycdn.sampleapp.ui.theme.ControlUnfocused
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

/**
 * Media chooser screen allowing to select a media item from a list.
 *
 * @param mediaListState State of the media list, encapsulating whether the media list is loading,
 *                       has encountered an error, or is ready for display. This is the full,
 *                       unfiltered list; the filter tabs and displayed list are derived from it.
 * @param mediaFilter The active channel-list filter.
 * @param onMediaFilterSelected Action to be taken when a filter tab is selected.
 * @param onMediaIndexSelected Action to be taken when a media item is selected from the list.
 *                             The index refers to a position within the filtered list.
 * @param peerId Peer ID to display in the screen title.
 * @param currentWycdnEnvLabel Current WyCDN environment label to display in the screen title.
 * @param modifier An optional [Modifier] for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaChooserScreen(
    mediaListState: MediaListState,
    mediaIndex: Int,
    mediaFilter: MediaFilter,
    onMediaFilterSelected: (MediaFilter) -> Unit,
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
                val fullList = mediaListState.mediaList
                val filters = remember(fullList) { fullList.availableFilters() }
                val initialPage = filters.indexOf(mediaFilter).coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialPage) { filters.size }
                val scope = rememberCoroutineScope()

                // Keep the ViewModel filter in sync with the current page. This drives the
                // filtered list handed to the player, so zapping follows the chosen tab.
                LaunchedEffect(pagerState, filters) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        onMediaFilterSelected(filters[page])
                    }
                }

                // Filter tabs on top of a swipeable pager (one page per filter). D-pad LEFT/RIGHT
                // flips pages from anywhere in the list; touch can swipe pages or tap a tab.
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                        true
                                    } else false
                                }

                                Key.DirectionRight -> {
                                    if (pagerState.currentPage < filters.lastIndex) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                        true
                                    } else false
                                }

                                else -> false
                            }
                        }
                ) {
                    FilterTabs(
                        filters = filters,
                        selectedFilter = filters[pagerState.currentPage],
                        onFilterSelected = { scope.launch { pagerState.animateScrollToPage(filters.indexOf(it)) } }
                    )
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        MediaList(
                            mediaList = fullList.filterBy(filters[page]),
                            mediaIndex = mediaIndex,
                            onMediaIndexSelected = onMediaIndexSelected,
                            isActive = page == pagerState.currentPage
                        )
                    }
                }
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
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val listState = rememberLazyListState()

    val focusRequesters = remember(mediaList.size) {
        List(mediaList.size) { FocusRequester() }
    }

    var focusedIndex by remember {
        mutableStateOf(-1)
    }

    // Focus a channel when this page is the active one. The index is coerced into range so that,
    // after flipping to a page where the previously selected index no longer exists, focus still
    // lands on a valid row (keeping D-pad LEFT/RIGHT working). Only the active page grabs focus so
    // neighbouring pages composed mid-swipe don't fight over it.
    LaunchedEffect(isActive, mediaIndex, mediaList.size) {
        if (!isActive || mediaList.isEmpty()) return@LaunchedEffect
        val target = mediaIndex.coerceIn(0, mediaList.lastIndex)
        listState.scrollToItem(target)
        awaitFrame()
        // The page may still be settling (mid-animation); ignore if its node isn't attached yet.
        runCatching { focusRequesters[target].requestFocus() }
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

/**
 * Horizontal, D-pad-focusable row of filter tabs.
 *
 * The highlighted tab tracks the pager's current page. Tapping a tab animates to its page; touch
 * swipe and D-pad LEFT/RIGHT change pages too. Tabs are touch targets only (not D-pad focusable),
 * so the remote drives filters via LEFT/RIGHT rather than by focusing the row.
 */
@Composable
private fun FilterTabs(
    filters: List<MediaFilter>,
    selectedFilter: MediaFilter,
    onFilterSelected: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = filters.indexOf(selectedFilter).coerceAtLeast(0)

    // Keep the highlighted tab on screen when the filter changes (swipe / D-pad / tap), but only
    // scroll when it is actually off-screen or clipped, so already-visible tabs don't jump around.
    LaunchedEffect(selectedIndex) {
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val fullyVisible = visible != null &&
            visible.offset >= 0 &&
            visible.offset + visible.size <= info.viewportEndOffset
        if (!fullyVisible) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_small)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        items(filters) { filter ->
            FilterTab(
                filter = filter,
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterTab(
    filter: MediaFilter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = filter.label,
        color = White,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(
                // Highlight the active filter. Tabs aren't D-pad focusable (LEFT/RIGHT flips
                // pages), so this is purely a selection indicator.
                color = if (selected) ControlFocused else ControlUnfocused,
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(filter) { detectTapGestures { onClick() } }
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.padding_small)
            )
    )
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
        mediaFilter = MediaFilter.ALL,
        onMediaFilterSelected = { },
        onMediaIndexSelected = { },
        peerId = "generic-123abc",
        currentWycdnEnvName = "Default"
    )
}
