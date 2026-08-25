/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.ui.EqualizerScreen
import co.aospa.dolby.xiaomi.geq.ui.EqualizerViewModel
import co.aospa.dolby.xiaomi.geq.ui.PresetsScreen
import com.android.settingslib.spa.framework.theme.SettingsTheme

class EqualizerActivity : ComponentActivity() {

    private val viewModel: EqualizerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SettingsTheme {
                var showingPresetsScreen by remember { mutableStateOf(false) }
                var showAddPresetDialog by remember { mutableStateOf(false) }

                val topAppBarState = rememberTopAppBarState()
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    stringResource(
                                        if (showingPresetsScreen) R.string.dolby_geq_preset
                                        else R.string.dolby_preset
                                    )
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    if (showingPresetsScreen) {
                                        showingPresetsScreen = false
                                    } else {
                                        finish()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    },
                    floatingActionButton = {
                        if (showingPresetsScreen) {
                            FloatingActionButton(
                                onClick = { showAddPresetDialog = true },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.dolby_geq_new_preset)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    if (showingPresetsScreen) {
                        PresetsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showingPresetsScreen = false },
                            showAddDialog = showAddPresetDialog,
                            onDismissAddDialog = { showAddPresetDialog = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        EqualizerScreen(
                            viewModel = viewModel,
                            onNavigateToPresets = { showingPresetsScreen = true },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
