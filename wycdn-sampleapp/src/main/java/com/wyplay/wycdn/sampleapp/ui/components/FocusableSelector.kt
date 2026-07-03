/*
 * Copyright (C) 2026 Wyplay, All Rights Reserved.
 * This source code and any compilation or derivative thereof is the proprietary
 * information of Wyplay and is confidential in nature.
 * Under no circumstances is this software to be exposed to or placed
 * under an Open Source License of any type without the expressed written
 * permission of Wyplay.
 */

package com.wyplay.wycdn.sampleapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color.Companion.White
import com.wyplay.wycdn.sampleapp.ui.theme.ControlFocused
import com.wyplay.wycdn.sampleapp.ui.theme.ControlUnfocused
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A single-choice selector operable entirely with a D-pad. The header row toggles an
 * inline list of focusable option rows; the focused row is highlighted, and CENTER/ENTER
 * (handled by [Modifier.clickable]) selects it. Touch clicks work the same way.
 */
@Composable
fun FocusableSelector(
    label: String,
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // Focus anchor for the collapsed header. When the option list collapses (by
    // selection or BACK), the focused row is removed from composition, so focus must
    // be moved back here or the D-pad is left with nothing focused.
    val headerFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (expanded &&
                    keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.key == Key.Back || keyEvent.key == Key.DirectionLeft)
                ) {
                    expanded = false
                    headerFocusRequester.requestFocus()
                    true
                } else {
                    false
                }
            }
    ) {
        Text(text = label, color = White)

        var headerFocused by remember { mutableStateOf(false) }
        Text(
            text = selected,
            color = White,
            style = TextStyle(fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(headerFocusRequester)
                .onFocusChanged { headerFocused = it.isFocused }
                .clickable { expanded = !expanded }
                .background(
                    color = if (headerFocused) ControlFocused else ControlUnfocused,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        )

        if (expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEach { item ->
                    key(item) {
                        var rowFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { rowFocused = it.isFocused }
                                .clickable {
                                    onSelect(item)
                                    expanded = false
                                    headerFocusRequester.requestFocus()
                                }
                                .background(
                                    color = if (rowFocused) ControlFocused else ControlUnfocused
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item, color = White)
                            if (item == selected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
