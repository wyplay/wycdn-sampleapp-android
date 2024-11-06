/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.components

import android.util.EventLog
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.PlayerView
import com.wyplay.wycdn.sampleapp.ui.data.TrackInfo
import com.wyplay.wycdn.sampleapp.ui.models.ResolutionViewModel

/**
 * Composable function to display a media player component.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerComponent(
    mediaList: List<MediaItem>,
    mediaIndex: Int,
    modifier: Modifier = Modifier,
    onCurrentMediaMetadataChanged: (MediaMetadata) -> Unit = {},
    onVideoSizeChanged: (VideoSize) -> Unit = {},
    onPlaybackStateChanged: (Int) -> Unit = {}
) {
    val resolutionViewModel: ResolutionViewModel = viewModel()
    // Get current context
    val context = LocalContext.current

    // Initialize player variable using remember to retain its state across recompositions
    var player: Player? by remember { mutableStateOf(null) }

    // Initialize state variables to keep track of player state
    var playWhenReady by remember { mutableStateOf(true) }
    var mediaItemIndex by remember { mutableIntStateOf(mediaIndex) }

    // Initialize errorMessage state variable to display in an alert dialog
    var errorMessage by remember { mutableStateOf("") }

    // Create PlayerView using the current player instance
    val playerView = createPlayerView(player)
    playerView.useController = true
    playerView.controllerAutoShow = true

    val trackSelector = DefaultTrackSelector(context).apply {
        parameters = buildUponParameters()
            .setTunnelingEnabled(true)
            .build()
    }
    val resolutionControl = ResolutionControl(trackSelector)
    val selectedResolution by resolutionViewModel.selectedResolution.collectAsState()
    val trackInfoList by resolutionViewModel.trackInfoList.collectAsState()

    val eventLogger = EventLogger()

    LaunchedEffect(selectedResolution) {
        if (selectedResolution != null) {
            if (selectedResolution!!.first > 0) {
                val param = resolutionControl.setTrackByResolution(
                    selectedResolution!!.first,
                    selectedResolution!!.second,
                    trackInfoList
                )
                param?.let { player?.trackSelectionParameters = it }
            } else {
                // AUTO
                val param = resolutionControl.setAutoResolution()
                player?.trackSelectionParameters = param
            }
        }
    }

    /**
     * Initializes and returns a media player.
     */
    fun initializePlayer() {
        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setLiveTargetOffsetMs(0))
            .build().apply {
                // Set the media items to play
                setMediaItems(mediaList, mediaItemIndex, C.TIME_UNSET)
                // Start playing when ready
                this.playWhenReady = playWhenReady
                // Add a listener to observe metadata changes
                addListener(object : Player.Listener {
                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        // Invoke the callback with the new media metadata
                        onCurrentMediaMetadataChanged(mediaMetadata)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        // Auto-resume playback if a recoverable error occurs
                        when (error.errorCode) {
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                            PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                                Log.d("player", "PlaybackException: ${error.message}")
                                seekToDefaultPosition()
                                prepare()
                            }

                            else -> {
                                val rootCause =
                                    error.cause?.let { "Caused by: ${it::class.java.simpleName}: ${it.message}" }
                                        ?: ""
                                errorMessage =
                                    "${error.errorCodeName} (${error.errorCode})\nPlaybackException: ${error.message}\n$rootCause"
                            }
                        }
                    }

                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        // Invoke the callback with the new video size
                        onVideoSizeChanged(videoSize)
                    }

                    //Get all availiable resolutions for the playing media
                    override fun onTracksChanged(tracks: Tracks) {
                        for (trackGroup in tracks.groups) {
                            for (i in 0 until trackGroup.length) {
                                val trackFormat = trackGroup.getTrackFormat(i)
                                val isSelected = trackGroup.isSelected
                                if (trackFormat.height > 0 && isSelected) {
                                    val trackInfo = TrackInfo(
                                        trackGroup.mediaTrackGroup,
                                        i,
                                        trackFormat.height,
                                        trackFormat.width
                                    )
                                    resolutionViewModel.addTrackInfo(trackInfo)
                                    resolutionViewModel.addResolutionFormat(
                                        trackFormat.height,
                                        trackFormat.width
                                    )
                                }
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        onPlaybackStateChanged(playbackState)
                        Log.d("player", "playbackState: $playbackState")
                        when (playbackState) {
                            Player.STATE_READY -> {
                                resolutionViewModel.setLoaderFlag(false)
                            }
                        }
                    }
                })

                addAnalyticsListener(eventLogger)
                // Prepare the player
                prepare()
            }
    }

    // Function to release the player
    fun releasePlayer() {
        player?.let {
            // Save player state
            playWhenReady = it.playWhenReady
            mediaItemIndex = it.currentMediaItemIndex
            it.release()
        }
        player = null
    }

    // Hook Player and PlayerView lifecycle to composable lifecycle
    LifecycleEffect { _, event ->
        when (event) {
            // Initialize the player when activity starts or resume
            Lifecycle.Event.ON_START -> {
                initializePlayer()
                playerView.onResume()
            }
            // Release player resources when activity stops
            Lifecycle.Event.ON_STOP -> {
                playerView.onPause()
                releasePlayer()
            }

            else -> {}
        }
    }

    // Embed PlayerView into the Compose UI hierarchy using an AndroidView
    Box(modifier = Modifier.fillMaxSize()) {

        val buttonFocusRequester = remember { FocusRequester() }
        val playerFocusRequester = remember { FocusRequester() }
        val showResolutionMenuFlag by resolutionViewModel.menuFlagTV.collectAsState(initial = false)
        val showResolutionBox by resolutionViewModel.resolutionMenuFocus.collectAsState(initial = false)
        var boxIsFocused by remember {
            mutableStateOf(false)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {

            if (showResolutionBox) {

                Box(
                    modifier = Modifier
                        .focusRequester(buttonFocusRequester)
                        .focusable()
                        .background(
                            if (boxIsFocused) Color(0, 0, 255, 100)
                            else Transparent
                        )
                        .onFocusChanged { focusState ->
                            boxIsFocused = focusState.isFocused
                        }

                ) {

                    Button(
                        onClick = {
                            resolutionViewModel.setMenuFlagTV(!showResolutionMenuFlag)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Transparent),
                        modifier = Modifier
                            .padding(1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Resolution",
                            tint = White
                        )
                    }
                    if (showResolutionMenuFlag) {
                        ShowResolutionMenu()
                    }
                }

                LaunchedEffect(showResolutionBox) {
                    if (showResolutionBox) {
                        buttonFocusRequester.requestFocus()
                    }
                }
            }

            AndroidView(
                modifier = modifier
                    .focusRequester(playerFocusRequester)
                    .focusable()
                    .onFocusChanged { focusState ->
                        //Debug
                        if (focusState.isFocused) {
                            Log.d("focusscreen", "player focus")
                        } else {
                            Log.e("focusscreen", "player has no focus")
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            resolutionViewModel.setFocusOnResolutionMenu(true)
                            true
                        } else {
                            resolutionViewModel.setFocusOnResolutionMenu(false)
                            playerView.dispatchKeyEvent(keyEvent.nativeKeyEvent)
                        }
                    },
                factory = {
                    playerView
                }
            )
        }
    }

    // Display an AlertDialog for error messages
    if (errorMessage.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { errorMessage = "" },
            confirmButton = {
                TextButton(onClick = { errorMessage = "" }) {
                    Text("OK")
                }
            },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Warning") },
            title = { Text("Playback Error") },
            text = { Text(errorMessage) }
        )
    }
}

@Composable
fun ShowResolutionMenu() {
    val resolutionViewModel: ResolutionViewModel = viewModel()
    val formats by resolutionViewModel.formats.collectAsState(initial = mutableSetOf())
    val selectedResolutionStr by resolutionViewModel.formatStr.collectAsState(initial = null)

    formats.add(Pair(0, 0)) // AUTO

    var selectedResolution by remember {
        mutableStateOf<Pair<Int, Int>?>(null)
    }

    val handleResolutionSelect: (Int, Int, String) -> Unit = { height, width, resolutionStr ->
        selectedResolution = Pair(height, width)
        resolutionViewModel.setMenuFlagTV(false)
        resolutionViewModel.setFocusOnResolutionMenu(true)
        resolutionViewModel.setLoaderFlag(true)
        resolutionViewModel.setFocusOnResolutionMenu(false)
        resolutionViewModel.setSelectedResolution(Pair(height, width))
        resolutionViewModel.addResolutionFormatStr(resolutionStr)
    }
    var focusedIndex by remember {
        mutableStateOf(-1)
    }

    LaunchedEffect(Unit) {
        focusedIndex = -1
    }

    Column(
        modifier = Modifier
            .width(200.dp)
            .background(Color(0, 0, 255, 50))
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    resolutionViewModel.setFocusOnResolutionMenu(false)
                    resolutionViewModel.setMenuFlagTV(false)
                    true
                }else{
                    false
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .width(120.dp)
                .align(Alignment.End)
        ) {
            items(formats.toList()) { resolution ->
                val index = formats.indexOf(resolution)

                val backgroundcolor = if (index == focusedIndex) MaterialTheme.colorScheme.primary
                else Transparent

                val textColor = if (index == focusedIndex) MaterialTheme.colorScheme.onPrimary
                else Gray

                val resolutionString = if (resolution?.first == 0) {
                    "Auto"
                } else {
                    resolution?.first.toString() + "p "
                }
                Column (modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedIndex = index
                        }
                    }
                    .focusable()
                ){
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            handleResolutionSelect(
                                resolution?.first ?: 0,
                                resolution?.second ?: 0,
                                resolutionString
                            )
                        }
                        .padding(vertical = 1.dp)
                        .background(backgroundcolor)
                    ) {
                        Text(
                            text = resolutionString,
                            modifier = Modifier
                                .padding(10.dp)
                                .width(80.dp),
                            style = TextStyle(
                                color = textColor,
                                fontSize = 12.sp
                            )
                        )
                        if (resolutionString == selectedResolutionStr) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = White,
                                modifier = Modifier
                                    .size(10.dp)
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }


            }
        }
    }
}

/**
 * A side effect of composition that must run on lifecycle event changes.
 * It observes the lifecycle events of the provided [lifecycleOwner] and executes [onEvent] accordingly.
 *
 * @param lifecycleOwner [LifecycleOwner] whose state should be observed.
 * @param onEvent Callback to run when the [LifecycleOwner] changes state.
 */
@Composable
fun LifecycleEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
) {
    // Setup a disposable effect to handle the lifecycle of the provided lifecycleOwner
    DisposableEffect(lifecycleOwner) {
        // Define an observer for lifecycle events
        val observer = LifecycleEventObserver { source, event ->
            // Call the provided event handler with the source and event
            onEvent(source, event)
        }
        // Add the observer to the lifecycle of the owner
        lifecycleOwner.lifecycle.addObserver(observer)

        // Remove the observer when this effect is disposed
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Creates and returns a [PlayerView] for the given player.
 */
@UnstableApi
@Composable
fun createPlayerView(player: Player?): PlayerView {
    // Get the current context
    val context = LocalContext.current

    // Create a PlayerView and remember it to retain its state across recompositions
    val playerView = remember {
        PlayerView(context).apply {
            // Set the player for the PlayerView
            this.player = player
        }

    }

    // Establish a disposable effect to handle player disposal
    DisposableEffect(player) {
        // Set the player for the PlayerView
        playerView.player = player

        // Remove the player from the PlayerView when this composable is disposed
        onDispose {
            playerView.player = null
        }
    }
    return playerView
}

