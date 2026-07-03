/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.screens

import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.wyplay.wycdn.sampleapp.MainActivity
import com.wyplay.wycdn.sampleapp.R
import com.wyplay.wycdn.sampleapp.ui.components.FocusableSelector
import com.wyplay.wycdn.sampleapp.ui.components.PlayerComponent
import com.wyplay.wycdn.sampleapp.ui.models.MediaListState
import com.wyplay.wycdn.sampleapp.ui.models.ResolutionViewModel
import com.wyplay.wycdn.sampleapp.ui.models.SettingsViewModel
import com.wyplay.wycdn.sampleapp.ui.models.WycdnDebugInfoState
import com.wyplay.wycdn.sampleapp.ui.models.WycdnMediaDataSourceFactory
import com.wyplay.wycdn.sampleapp.ui.models.WycdnViewModel
import com.wyplay.wycdn.sampleapp.ui.theme.ControlFocused
import com.wyplay.wycdn.sampleapp.ui.theme.ControlUnfocused
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Media player screen responsible for rendering the ExoPlayer view.
 *
 * @param mediaListState State of the media list, encapsulating whether the media list is loading,
 *                       has encountered an error, or is ready for display.
 * @param mediaIndex Index of the currently selected media item within the media list.
 * @param debugInfoState State of WyCDN debug information, encapsulating whether the debug info is loading,
 *                       is unavailable because of an error, or is ready for display.
 * @param playerInfoViewModel Player info view model.
 * @param modifier An optional [Modifier] for this composable.
 */
@Composable
fun PlayerScreen(
    mediaListState: MediaListState,
    mediaIndex: Int,
    debugInfoState: WycdnDebugInfoState,
    playerInfoViewModel: PlayerInfoViewModel,
    settingsViewModel: SettingsViewModel,
    wycdnViewModel: WycdnViewModel,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? MainActivity)

    DisposableEffect(Unit) {
        // Enter full-screen mode when composable is initialized
        activity?.setFullScreenMode(true)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Exit full-screen when composable is disposed
        onDispose {
            activity?.setFullScreenMode(false)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    when (mediaListState) {
        is MediaListState.Loading -> {
            // Show loading indicator
            LoadingMessage(modifier)
        }

        is MediaListState.Error -> {
            // Error occurred
            ErrorMessage(mediaListState.e, modifier)
        }

        is MediaListState.Ready -> {
            // Show player
            PlayerSurface(
                mediaListState.mediaList,
                mediaIndex,
                debugInfoState,
                modifier,
                playerInfoViewModel,
                settingsViewModel,
                wycdnViewModel
            )
        }
    }

}

data class PlayerInfo(var resolution: String = "0x0", var state: Int = -1)

class PlayerInfoViewModel : ViewModel() {
    private val _playerInfo = MutableStateFlow(PlayerInfo("0x0", -1))
    val playerInfo: StateFlow<PlayerInfo> = _playerInfo.asStateFlow()

    fun updateResolution(resolution: String) {
        _playerInfo.value = _playerInfo.value.copy(resolution = resolution)
    }

    fun updateState(state: Int) {
        _playerInfo.value.state = state
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

@UnstableApi
@Composable
private fun PlayerSurface(
    mediaList: List<MediaItem>,
    mediaIndex: Int,
    debugInfoState: WycdnDebugInfoState,
    modifier: Modifier = Modifier,
    playerInfoViewModel: PlayerInfoViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    wycdnViewModel: WycdnViewModel
) {
    var showSettingsMenu by remember { mutableStateOf(false) } // Toggle for settings menu visibility

    val resolutionViewModel: ResolutionViewModel = viewModel() // Ensure proper constructor usage
    val loaderFlag by resolutionViewModel.loaderFlag.collectAsState(initial = false)
    val debugMenuEnabled by settingsViewModel.debugMenuEnabled.collectAsState(initial = false)

    // When the debug menu is disabled there is no gear, so focus falls back to the player (keeping
    // the MENU key working). When the debug menu is enabled the gear owns focus while the menu is
    // closed; that request lives in DebugInfoChip, co-located with the gear so it runs only after
    // the icon is attached. Requesting from a LaunchedEffect ensures focus is restored AFTER the
    // closed menu subtree leaves composition, otherwise Compose resets focus to the root.
    val playerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(showSettingsMenu, debugMenuEnabled) {
        if (!showSettingsMenu && !debugMenuEnabled) {
            playerFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .background(color = Black)
            .fillMaxSize()
            .focusable(false),
        contentAlignment = Alignment.Center
    ) {
        var mediaTitle by remember { mutableStateOf(mediaList[mediaIndex].mediaMetadata.title.toString()) }

        val mediaSourceFactory = remember(mediaIndex) {
            when (mediaList[mediaIndex].mediaMetadata.extras?.getString("format")?.uppercase()) {
                "V1F", "V2F" -> {
                    val wycdnMediaDataSource = WycdnMediaDataSourceFactory(wycdnViewModel.downloadClient)
                    DefaultMediaSourceFactory(wycdnMediaDataSource)
                }
                else -> DefaultMediaSourceFactory(DefaultHttpDataSource.Factory())
            }
        }

        // Player component
        PlayerComponent(
            mediaList = mediaList,
            mediaIndex = mediaIndex,
            onCurrentMediaMetadataChanged = { mediaMetadata ->
                mediaTitle = mediaMetadata.title.toString()
            },
            onVideoSizeChanged = { videoSize ->
                playerInfoViewModel.updateResolution("${videoSize.width}x${videoSize.height}")
                wycdnViewModel.updatePlayerResolutionInfo(videoSize.width, videoSize.height);
            },
            onPlaybackStateChanged = { state ->
                playerInfoViewModel.updateState(state)
            },
            onMenuKey = {
                if (debugMenuEnabled) {
                    showSettingsMenu = true
                }
            },
            mediaSourceFactory = mediaSourceFactory,
            playerFocusRequester = playerFocusRequester
        )

        // Title chip and optional Debug info chip
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
        ) {
            TitleChip(
                title = mediaTitle,
                modifier = Modifier.align(Alignment.End)
            )
            DebugInfoChip(
                debugInfoState = debugInfoState,
                modifier = Modifier.align(Alignment.End),
                playerInfoViewModel = playerInfoViewModel,
                debugMenuEnabled = debugMenuEnabled,
                settingsMenuOpen = showSettingsMenu,
                onSettingsClick = { showSettingsMenu = !showSettingsMenu }
            )
        }

        if (debugMenuEnabled) {
            // Settings Menu
            if (showSettingsMenu) {
                SettingsMenu(
                    settingsViewModel = settingsViewModel,
                    wycdnViewModel = wycdnViewModel,
                    onDismiss = { showSettingsMenu = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        if (loaderFlag) {
            CircularProgressIndicator(modifier = Modifier.size(50.dp), color = White)
        }

    }
}

@Composable
private fun TitleChip(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = White, // Text color for visibility
        modifier = modifier
            .background(
                color = Black.copy(alpha = 0.5f), // Lightly transparent background
                shape = RoundedCornerShape(50.dp) // Rounded corners for the chip
            )
            .padding( // Padding inside the chip
                horizontal = dimensionResource(R.dimen.padding_small),
                vertical = dimensionResource(R.dimen.padding_extra_small)
            ),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
fun TitleChipPreview() {
    Box(
        modifier = Modifier.size(200.dp, 100.dp)
    ) {
        TitleChip(
            title = "Title Chip Preview",
            modifier = Modifier
                .align(Alignment.TopEnd) // Align to the top end corner
                .padding(dimensionResource(R.dimen.padding_medium)) // Padding from the edges of the Box
        )
    }
}

@Composable
fun DebugInfoChip(
    debugInfoState: WycdnDebugInfoState,
    modifier: Modifier = Modifier,
    playerInfoViewModel: PlayerInfoViewModel = viewModel(),
    debugMenuEnabled: Boolean = false,
    settingsMenuOpen: Boolean = false,
    gearFocusRequester: FocusRequester = remember { FocusRequester() },
    onSettingsClick: () -> Unit
) {
    val playerInfo by playerInfoViewModel.playerInfo.collectAsState(initial = PlayerInfo())

    Row(
        modifier = Modifier
            .background(
                color = Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(5.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Resolution: ${playerInfo.resolution}",
            color = White,
            style = TextStyle(fontSize = 16.sp)
        )
        Log.d("DebugInfoChip", "Resolution: ${playerInfo.resolution}")
        if (debugMenuEnabled) {
            // Keep the gear focused whenever the debug menu is closed so the D-pad always has a
            // target. Co-located with the gear here so the request runs after the icon is attached.
            LaunchedEffect(settingsMenuOpen) {
                if (!settingsMenuOpen) {
                    gearFocusRequester.requestFocus()
                }
            }
            var gearFocused by remember { mutableStateOf(false) }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = White,
                modifier = Modifier
                    .size(24.dp)
                    .focusRequester(gearFocusRequester)
                    .onFocusChanged { gearFocused = it.isFocused }
                    .background(
                        color = if (gearFocused) ControlFocused else ControlUnfocused,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSettingsClick() }
            )
        }
    }
}

@Composable
fun SettingsMenu(
    modifier: Modifier = Modifier,
    wycdnViewModel: WycdnViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val resolutionViewModel: ResolutionViewModel = viewModel()

    val wycdnMode by wycdnViewModel.wycdnMode.collectAsState()
    var selectedMode by remember { mutableStateOf(wycdnMode) }
    var selectedLogLevel by remember { mutableStateOf("info") }

    // Resolution options derived from the formats reported by the player.
    val formats by resolutionViewModel.formats.collectAsState()
    val selectedResolutionStr by resolutionViewModel.formatStr.collectAsState()

    // Build label -> (height, width) pairs. Height 0 means "Auto".
    val resolutionOptions: List<Pair<String, Pair<Int, Int>>> = remember(formats) {
        val opts = mutableListOf<Pair<String, Pair<Int, Int>>>()
        opts.add("Auto" to Pair(0, 0))
        formats.filterNotNull()
            .filter { it.first > 0 }
            .sortedByDescending { it.first }
            .forEach { pair -> opts.add("${pair.first}p" to pair) }
        opts
    }
    val resolutionLabels = resolutionOptions.map { it.first }
    val currentResolutionLabel = selectedResolutionStr.ifEmpty { "Auto" }

    // Update UI when wycdnMode changes
    LaunchedEffect(wycdnMode) {
        selectedMode = wycdnMode
    }

    // Grab focus when the menu opens so the D-pad drives it immediately.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusGroup() // requestFocus() on this group delegates to the first focusable child
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Back) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
            .background(
                color = Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        // Resolution selector
        FocusableSelector(
            label = "Resolution",
            items = resolutionLabels,
            selected = currentResolutionLabel,
            onSelect = { label ->
                val option = resolutionOptions.firstOrNull { it.first == label }
                if (option != null) {
                    val (height, width) = option.second
                    resolutionViewModel.setMenuFlagMobile(false)
                    resolutionViewModel.setLoaderFlag(true)
                    resolutionViewModel.setSelectedResolution(Pair(height, width))
                    resolutionViewModel.addResolutionFormatStr(label)
                }
            }
        )

        // Log Level selector
        FocusableSelector(
            label = "Log Level",
            items = listOf("off", "error", "info", "warn", "debug"),
            selected = selectedLogLevel,
            onSelect = {
                selectedLogLevel = it
                wycdnViewModel.updateWycdnLogLevel(it)
            }
        )

        // Frog2Frog Mode selector
        FocusableSelector(
            label = "Frog2Frog Mode",
            items = listOf("full", "lite", "cdn"),
            selected = selectedMode,
            onSelect = {
                selectedMode = it
                wycdnViewModel.updateWycdnMode(it)
                settingsViewModel.setWycdnMode(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            var closeFocused by remember { mutableStateOf(false) }
            Button(
                onClick = { onDismiss() },
                modifier = Modifier.onFocusChanged { closeFocused = it.isFocused },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (closeFocused) ControlFocused else ControlUnfocused,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Close")
            }
        }
    }
}
