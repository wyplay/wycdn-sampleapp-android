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
import com.wyplay.wycdn.sampleapp.ui.data.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ResolutionViewModel : ViewModel() {
    // Send resolution selection to player
    private val _selectedResolution = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedResolution: StateFlow<Pair<Int, Int>?> = _selectedResolution

    fun setSelectedResolution(resolution: Pair<Int, Int>) {
        _selectedResolution.value = resolution
    }

    // Send the track info from player to UI
    private val _trackInfoList = MutableStateFlow<MutableList<TrackInfo>>(mutableListOf())
    val trackInfoList: StateFlow<MutableList<TrackInfo>> = _trackInfoList

    // Send resolution to the UI to poplulate the list
    private val _formats = MutableStateFlow<MutableSet<Pair<Int, Int>?>>(mutableSetOf())
    val formats: StateFlow<MutableSet<Pair<Int, Int>?>> = _formats

    private val _formatStr = MutableStateFlow<String>("")
    val formatStr: StateFlow<String> = _formatStr

    private val _loaderFlag = MutableStateFlow(false)
    val loaderFlag: StateFlow<Boolean> = _loaderFlag

    private val _showMenuFlagTV = MutableStateFlow(false)
    val menuFlagTV: StateFlow<Boolean> = _showMenuFlagTV

    private val _showMenuFlagMobile = MutableStateFlow(false)
    val menuFlagMobile: StateFlow<Boolean> = _showMenuFlagMobile

    private val _resolutionMenuFocus = MutableStateFlow(false)
    val resolutionMenuFocus: StateFlow<Boolean> = _resolutionMenuFocus

    init {
        _trackInfoList.value = mutableListOf()
        _formats.value = mutableSetOf()
    }

    fun addTrackInfo(trackInfo: TrackInfo) {
        _trackInfoList.value.add(trackInfo)
        _trackInfoList.value = _trackInfoList.value //trigger live data update
    }

    fun addResolutionFormat(height: Int, width: Int) {
        _formats.value.add(Pair(height, width))
        _formats.value = _formats.value
    }

    fun addResolutionFormatStr(selectedResolutionStr: String) {
        _formatStr.value = selectedResolutionStr
        Log.d("resolutionViewModel", "addResolutionFormatStr: ${_trackInfoList.value}")
    }

    fun setLoaderFlag(flag: Boolean) {
        _loaderFlag.value = flag
    }

    fun setMenuFlagTV(flag: Boolean) {
        _showMenuFlagTV.value = flag
    }

    fun setMenuFlagMobile(flag: Boolean) {
        _showMenuFlagMobile.value = flag
    }

    fun setFocusOnResolutionMenu(flag: Boolean) {
        _resolutionMenuFocus.value = flag
    }

}