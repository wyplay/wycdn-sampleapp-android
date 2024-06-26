/*
 * Copyright (C) 2024 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wyplay.wycdn.sampleapp.BuildConfig
import com.wyplay.wycdn.sampleapp.R
import com.wyplay.wycdn.sampleapp.ui.models.SettingsViewModel
import com.wyplay.wycdn.sampleapp.ui.models.WycdnEnv

/**
 * Settings screen allowing to update application settings.
 *
 * @param settingsViewModel Instance of the [SettingsViewModel].
 * @param peerId Peer ID to display on the screen.
 * @param onStartButtonClick Action to be taken when the start button is clicked.
 * @param modifier An optional [Modifier] for this composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    peerId: String,
    onStartButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect the current environment as state for composable to react to changes
    val currentEnv by settingsViewModel.wycdnEnvironment.collectAsState(initial = WycdnEnv.default)

    // Local state to store the selected environment, initialized with currentEnv
    var selectedEnv by remember { mutableStateOf(currentEnv) }

    // Observe changes in currentEnv and update selectedEnv accordingly
    // This is needed as it currentEnv is collected asynchronously
    LaunchedEffect(currentEnv) {
        selectedEnv = currentEnv
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("${stringResource(R.string.title_settings_screen)} ${BuildConfig.VERSION_NAME}") })
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Peer ID label
            Text(
                text = stringResource(R.string.label_peerid, peerId),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // Environment selector
            DropdownSettingSelector(
                label = stringResource(R.string.label_wycdn_environment),
                items = WycdnEnv.entries.map { it.label },
                selectedValue = selectedEnv.label,
                onValueChange = { selectedLabel ->
                    // Find the WycdnEnv value corresponding to the selected label and store it
                    WycdnEnv.entries.find { it.label == selectedLabel }?.let { env ->
                        selectedEnv = env
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f)) // This pushes the button to the bottom

            Button(
                onClick = {
                    // Update the settings with the selected environment
                    settingsViewModel.setWycdnEnvironment(selectedEnv)
                    // Notify start button has been clicked
                    onStartButtonClick()
                },
                modifier = Modifier
                    .fillMaxWidth() // Make the button fill the width of its container
                    .padding(16.dp) // Add some padding around the button
            ) {
                Text(stringResource(R.string.action_start))
            }
        }
    }
}

@Composable
fun DropdownSettingSelector(
    label: String,
    items: List<String>,
    selectedValue: String?,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val anchor = Modifier
        .fillMaxWidth()
        .clickable { expanded = true }
        .padding(16.dp)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = anchor, verticalAlignment = Alignment.CenterVertically) {
            Text(text = selectedValue ?: "Select", style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text = item) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
