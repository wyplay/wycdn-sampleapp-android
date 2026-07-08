/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import com.wyplay.wycdn.sampleapp.SampleApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "MediaViewModel"

/**
 * ViewModel providing access to the media list and current media selection.
 *
 * @property repository The [MediaRepository] instance accessed by this ViewModel for settings data operations.
 * @companion Factory Provides a factory for creating [MediaViewModel] instances, encapsulating dependency injection.
 */
class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

    // Backing property for media list state, initially set to Loading.
    private val _mediaListState = MutableStateFlow<MediaListState>(MediaListState.Loading)

    /**
     * Property providing [MediaListState] as a [StateFlow], allowing the UI to observe changes to the media list's state.
     * This flow starts in the Loading state and updates as the media list is loaded or if an error occurs.
     */
    val mediaListState: StateFlow<MediaListState> = _mediaListState.asStateFlow()

    // Backing property for the index of the currently selected media item, initialized to 0.
    private val _mediaIndex = MutableStateFlow<Int>(0)

    /**
     * Property providing the index as a [StateFlow] of the currently selected media item.
     * Allows the UI to observe changes to the current media selection.
     */
    val mediaIndexState: StateFlow<Int> = _mediaIndex.asStateFlow()

    /**
     * Sets the current media item index to the provided [index].
     *
     * This method updates the state of the currently selected media item in the list,
     * allowing observers of [mediaIndexState] to react to the change.
     *
     * @param index The new index of the currently selected media item. Must be within the bounds of the media list.
     *              It's the caller's responsibility to ensure the provided index is valid.
     */
    fun setMediaIndex(index: Int) {
        _mediaIndex.value = index
    }

    // Backing property for the active channel-list filter, initialized to no filter.
    private val _filter = MutableStateFlow(MediaFilter.ALL)

    /**
     * Property providing the active [MediaFilter] as a [StateFlow].
     *
     * The filter is applied to the channel list shown by the media chooser and, transitively, to
     * the playlist handed to the player, so channel zapping stays within the active filter.
     * Note: [mediaIndexState] is an index into the *filtered* list for the current filter.
     */
    val filterState: StateFlow<MediaFilter> = _filter.asStateFlow()

    /**
     * Sets the active channel-list [filter].
     *
     * [mediaIndexState] is intentionally left untouched: it is only meaningful once the user picks
     * a channel from the filtered list (which sets a fresh, valid index), and resetting it here
     * would move focus into the list when the user merely switches tabs.
     */
    fun setFilter(filter: MediaFilter) {
        _filter.value = filter
    }

    init {
        // Trigger the initial loading of the media list
        initMediaList()
    }

    /**
     * Initializes the media list by fetching the media items asynchronously.
     */
    private fun initMediaList() {
        viewModelScope.launch {
            try {
                _mediaListState.value = MediaListState.Ready(repository.fetchMediaList())
            } catch (e: Exception) {
                Log.e(TAG, "Cannot fetch the media list", e)
                _mediaListState.value = MediaListState.Error(e)
            }
        }
    }

    companion object {
        /**
         * A factory for creating instances of [MediaViewModel] with required dependencies.
         *
         * To use this factory, pass it as an argument to the `viewModel()` function in a Composable context.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            // We use the implementation suggested from:
            // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories#creationextras
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SampleApp
                MediaViewModel(repository = app.mediaRepository)
            }
        }
    }
}

/**
 * Represents the state of a media list, encapsulating different states for UI rendering.
 */
sealed interface MediaListState {
    data object Loading : MediaListState
    data class Error(val e: Exception) : MediaListState
    data class Ready(val mediaList: List<MediaItem>) : MediaListState
}
