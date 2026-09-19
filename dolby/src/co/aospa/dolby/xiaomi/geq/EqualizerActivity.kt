/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.ui.EqualizerScreen
import co.aospa.dolby.xiaomi.geq.ui.EqualizerViewModel
import co.aospa.dolby.xiaomi.geq.ui.PresetsScreen
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold

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
                var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
                var isPredictiveBackActive by remember { mutableStateOf(false) }

                PredictiveBackHandler(enabled = showingPresetsScreen) { progress ->
                    try {
                        isPredictiveBackActive = true
                        progress.collect { backEvent ->
                            predictiveBackProgress = backEvent.progress
                        }
                        isPredictiveBackActive = false
                        predictiveBackProgress = 0f
                        showingPresetsScreen = false
                    } catch (e: CancellationException) {
                        isPredictiveBackActive = false
                        predictiveBackProgress = 0f
                    }
                }

                val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
                val navControllerWrapper = remember(backDispatcherOwner, showingPresetsScreen) {
                    object : NavControllerWrapper {
                        override fun navigate(route: String, popUpCurrent: Boolean) {}
                        override fun navigateBack() {
                            if (showingPresetsScreen) {
                                showingPresetsScreen = false
                            } else {
                                backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed()
                            }
                        }
                    }
                }

                CompositionLocalProvider(LocalNavController provides navControllerWrapper) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
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
                    ) { _ ->
                        SettingsScaffold(
                            title = stringResource(
                                if (showingPresetsScreen) R.string.dolby_geq_preset
                                else R.string.dolby_preset
                            )
                        ) { paddingValues ->
                            AnimatedContent(
                                targetState = showingPresetsScreen,
                                transitionSpec = {
                                    if (targetState) {
                                        (slideInHorizontally { width -> width } + fadeIn())
                                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                                    } else {
                                        (slideInHorizontally { width -> -width } + fadeIn())
                                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                                    }
                                },
                                label = "EqualizerScreenTransition",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) { isPresets ->
                                if (isPresets) {
                                    val backScale = if (isPredictiveBackActive) (1f - predictiveBackProgress * 0.08f) else 1f
                                    val backCorner = if (isPredictiveBackActive) (24.dp * predictiveBackProgress) else 0.dp
                                    PresetsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { showingPresetsScreen = false },
                                        showAddDialog = showAddPresetDialog,
                                        onDismissAddDialog = { showAddPresetDialog = false },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = backScale
                                                scaleY = backScale
                                                clip = isPredictiveBackActive
                                                shape = RoundedCornerShape(backCorner)
                                            }
                                    )
                                } else {
                                    EqualizerScreen(
                                        viewModel = viewModel,
                                        onNavigateToPresets = { showingPresetsScreen = true },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
