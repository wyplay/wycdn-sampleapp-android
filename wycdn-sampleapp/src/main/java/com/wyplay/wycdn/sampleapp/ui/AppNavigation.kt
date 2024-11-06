/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wyplay.wycdn.sampleapp.ui.models.MediaViewModel
import com.wyplay.wycdn.sampleapp.ui.models.SettingsViewModel
import com.wyplay.wycdn.sampleapp.ui.models.WycdnEnv
import com.wyplay.wycdn.sampleapp.ui.models.WycdnViewModel
import com.wyplay.wycdn.sampleapp.ui.screens.MediaChooserScreen
import com.wyplay.wycdn.sampleapp.ui.screens.PlayerInfoSender
import com.wyplay.wycdn.sampleapp.ui.screens.PlayerInfoViewModel
import com.wyplay.wycdn.sampleapp.ui.screens.PlayerScreen
import com.wyplay.wycdn.sampleapp.ui.screens.SettingsScreen

/**
 * Navigation routes available within the application.
 */
private enum class NavRoute() {
    SettingsScreen,
    MediaChooserScreen,
    PlayerScreen
}

/**
 * Defines the navigation graph, orchestrating the navigation between the application screens.
 */
@Composable
fun AppNavigation() {
    // Initialize the MediaViewModel
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModel.Factory)
    // Collect and observe the state of the media list
    val mediaListState by mediaViewModel.mediaListState.collectAsState()
    // Collect and observe the current index of the selected media item
    val mediaIndex by mediaViewModel.mediaIndexState.collectAsState()

    // Initialize the SettingsViewModel using a custom factory to inject dependencies
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    // Collect and observe the current WyCDN environment setting
    val currentWycdnEnv = settingsViewModel.wycdnEnvironment.collectAsState(
        initial = settingsViewModel.wycdnEnvironmentList.defaultEnv
    )

    // Initialize the WycdnViewModel using a custom factory to inject dependencies
    val wycdnViewModel: WycdnViewModel = viewModel(factory = WycdnViewModel.Factory)
    // Collect and observe the state of WyCDN debug info
    val wycdnDebugInfoState by wycdnViewModel.debugInfoState.collectAsState()

    // Create and remember a navigation controller to manage navigation between composable screens
    val navController: NavHostController = rememberNavController()

    val playerInfoSender: PlayerInfoSender = PlayerInfoSender(wycdnViewModel)
    val playerInfoViewModel: PlayerInfoViewModel = PlayerInfoViewModel(playerInfoSender)

    // Define the navigation graph for the application
    NavHost(
        navController = navController,
        startDestination = NavRoute.SettingsScreen.name, // Initial screen of the app
        modifier = Modifier.fillMaxSize()
    ) {
        // Navigation route for the SettingsScreen
        composable(route = NavRoute.SettingsScreen.name) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                wycdnViewModel = wycdnViewModel,
                peerId = wycdnViewModel.peerId,
                onStartButtonClick = {
                    // Navigate to the MediaChooserScreen when the "Start" button is clicked
                    navController.navigate(NavRoute.MediaChooserScreen.name)
                }
            )
        }
        // Navigation route for the MediaChooserScreen
        composable(route = NavRoute.MediaChooserScreen.name) {
            MediaChooserScreen(
                mediaListState = mediaListState, // Media list to display
                onMediaIndexSelected = {
                    // Update the selected media index in the ViewModel and navigate to the Player screen
                    mediaViewModel.setMediaIndex(it)
                    navController.navigate(NavRoute.PlayerScreen.name)
                },
                peerId = wycdnViewModel.peerId,
                currentWycdnEnvName = currentWycdnEnv.value.name
            )
        }
        // Navigation route for the PlayerScreen
        composable(route = NavRoute.PlayerScreen.name) {
            PlayerScreen(
                mediaListState = mediaListState, // Media list for zapping
                mediaIndex = mediaIndex, // Media to play
                debugInfoState = wycdnDebugInfoState, // Debug info to display
                playerInfoViewModel = playerInfoViewModel,
                settingsViewModel = settingsViewModel,
                wycdnViewModel = wycdnViewModel
            )
        }
    }
}
