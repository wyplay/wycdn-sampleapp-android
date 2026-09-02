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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.wyplay.wycdn.sampleapp.MainActivity
import com.wyplay.wycdn.sampleapp.R
import com.wyplay.wycdn.sampleapp.ui.components.FocusableSelector
import com.wyplay.wycdn.sampleapp.ui.components.PlayerComponent
import com.wyplay.wycdn.sampleapp.ui.models.MediaListState
import com.wyplay.wycdn.sampleapp.ui.models.ResolutionViewModel
import com.wyplay.wycdn.sampleapp.ui.models.SettingsViewModel
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
 * @param playerInfoViewModel Player info view model.
 * @param modifier An optional [Modifier] for this composable.
 */
@Composable
fun PlayerScreen(
    mediaListState: MediaListState,
    mediaIndex: Int,
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
    modifier: Modifier = Modifier,
    playerInfoViewModel: PlayerInfoViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    wycdnViewModel: WycdnViewModel
) {
    var showSettingsMenu by remember { mutableStateOf(false) } // Toggle for settings menu visibility

    val resolutionViewModel: ResolutionViewModel = viewModel() // Ensure proper constructor usage
    val loaderFlag by resolutionViewModel.loaderFlag.collectAsState(initial = false)
    val debugMenuEnabled by settingsViewModel.debugMenuEnabled.collectAsState()
    val showsStreamResolution by settingsViewModel.showsStreamResolution.collectAsState()

    val playerFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    var isPlayerPositioned by remember { mutableStateOf(false) }
    var restoreFocusToSettingsButton by remember { mutableStateOf(false) }

    LaunchedEffect(
        showSettingsMenu,
        isPlayerPositioned,
        debugMenuEnabled,
        restoreFocusToSettingsButton
    ) {
        // Manage focus on either the player / settings button when the debug menu is closed
        if (!showSettingsMenu) {
            if (restoreFocusToSettingsButton && debugMenuEnabled) {
                // Restore focus on the settings button when closing the debug menu
                settingsFocusRequester.requestFocus()
            } else if (isPlayerPositioned) {
                // Ensure to request focus only when the player is positioned
                playerFocusRequester.requestFocus()
            }
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
                    restoreFocusToSettingsButton = false
                    showSettingsMenu = true
                }
            },
            mediaSourceFactory = mediaSourceFactory,
            playerFocusRequester = playerFocusRequester,
            modifier = Modifier
                .onGloballyPositioned { isPlayerPositioned = true }
                .then(
                    if (debugMenuEnabled) {
                        Modifier.focusProperties {
                            up = settingsFocusRequester
                            left = settingsFocusRequester
                        }
                    } else {
                        Modifier
                    }
                )
        )

        PlayerInfoOverlay(
            title = mediaTitle,
            peerId = wycdnViewModel.peerId,
            playerInfoViewModel = playerInfoViewModel,
            showsStreamResolution = showsStreamResolution,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(dimensionResource(R.dimen.padding_medium))
        )

        if (debugMenuEnabled) {
            SettingsButton(
                onSettingsClick = {
                    restoreFocusToSettingsButton = true
                    showSettingsMenu = !showSettingsMenu
                },
                settingsFocusRequester = settingsFocusRequester,
                playerFocusRequester = playerFocusRequester,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(dimensionResource(R.dimen.padding_medium))
            )

            if (showSettingsMenu) {
                SettingsMenu(
                    settingsViewModel = settingsViewModel,
                    wycdnViewModel = wycdnViewModel,
                    onDismiss = { showSettingsMenu = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(dimensionResource(R.dimen.padding_medium))
                )
            }
        }

        if (loaderFlag) {
            CircularProgressIndicator(modifier = Modifier.size(50.dp), color = White)
        }

    }
}

@Composable
private fun PlayerInfoOverlay(
    title: String,
    peerId: String,
    modifier: Modifier = Modifier,
    playerInfoViewModel: PlayerInfoViewModel = viewModel(),
    showsStreamResolution: Boolean = true
) {
    val playerInfo by playerInfoViewModel.playerInfo.collectAsState(initial = PlayerInfo())

    Column(
        modifier = modifier
            .background(
                color = Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = title,
            color = White,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (showsStreamResolution) {
            Text(
                text = playerInfo.resolution,
                color = LightGray,
                style = TextStyle(fontSize = 16.sp),
                modifier = Modifier.padding(top = 5.dp)
            )
            Log.d("PlayerInfoOverlay", "Resolution: ${playerInfo.resolution}")
        }
        Text(
            text = peerId,
            color = Gray,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
private fun PlayerInfoOverlayPreview() {
    val playerInfoViewModel = remember {
        PlayerInfoViewModel().apply { updateResolution("1920x1080") }
    }

    PlayerInfoOverlay(
        title = "Channel Title",
        peerId = "667cd3c8-a6b4-11f1-b0e9-f3fb924a0e58",
        playerInfoViewModel = playerInfoViewModel
    )
}

@Composable
private fun SettingsButton(
    onSettingsClick: () -> Unit,
    settingsFocusRequester: FocusRequester,
    playerFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    var gearFocused by remember { mutableStateOf(false) }
    Icon(
        imageVector = Icons.Filled.Settings,
        contentDescription = "Settings",
        tint = White,
        modifier = modifier
            .size(24.dp)
            .focusRequester(settingsFocusRequester)
            .focusProperties {
                down = playerFocusRequester
                right = playerFocusRequester
            }
            .onFocusChanged { gearFocused = it.isFocused }
            .background(
                color = if (gearFocused) ControlFocused else ControlUnfocused,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onSettingsClick() }
    )
}

@Composable
private fun SettingsMenu(
    modifier: Modifier = Modifier,
    wycdnViewModel: WycdnViewModel,
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val resolutionViewModel: ResolutionViewModel = viewModel()
    val showsStreamResolution by settingsViewModel.showsStreamResolution.collectAsState()

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
        // Show stream resolution switch
        var streamResolutionFocused by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { streamResolutionFocused = it.isFocused }
                .clickable {
                    settingsViewModel.setShowsStreamResolution(!showsStreamResolution)
                }
                .background(
                    color = if (streamResolutionFocused) ControlFocused else ControlUnfocused,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(dimensionResource(R.dimen.padding_small))
        ) {
            Text(
                text = stringResource(R.string.label_show_stream_resolution),
                color = White
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = showsStreamResolution,
                onCheckedChange = null
            )
        }

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
