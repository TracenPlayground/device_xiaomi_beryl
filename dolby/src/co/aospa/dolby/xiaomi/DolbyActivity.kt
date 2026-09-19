/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.dolby.xiaomi.ui.DolbyScreen
import co.aospa.dolby.xiaomi.ui.DolbyViewModel
import co.aospa.dolby.xiaomi.ui.ProfilesScreen
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold

class DolbyActivity : ComponentActivity() {

    private val viewModel: DolbyViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.checkAndPromptAddQsTile(this)

        setContent {
            SettingsTheme {
                val context = LocalContext.current
                var showingProfilesScreen by remember { mutableStateOf(false) }
                var showAddProfileDialog by remember { mutableStateOf(false) }
                var predictiveBackProgress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                var isPredictiveBackActive by remember { mutableStateOf(false) }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        val success = viewModel.importProfile(uri)
                        Toast.makeText(
                            context,
                            if (success) R.string.dolby_import_success else R.string.dolby_import_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                PredictiveBackHandler(enabled = showingProfilesScreen) { progress ->
                    try {
                        isPredictiveBackActive = true
                        progress.collect { backEvent ->
                            predictiveBackProgress = backEvent.progress
                        }
                        isPredictiveBackActive = false
                        predictiveBackProgress = 0f
                        showingProfilesScreen = false
                    } catch (e: CancellationException) {
                        isPredictiveBackActive = false
                        predictiveBackProgress = 0f
                    }
                }

                val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
                val navControllerWrapper = remember(backDispatcherOwner, showingProfilesScreen) {
                    object : NavControllerWrapper {
                        override fun navigate(route: String, popUpCurrent: Boolean) {}
                        override fun navigateBack() {
                            if (showingProfilesScreen) {
                                showingProfilesScreen = false
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
                            if (showingProfilesScreen) {
                                FloatingActionButton(
                                    onClick = { showAddProfileDialog = true },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = stringResource(R.string.dolby_profile_new)
                                    )
                                }
                            }
                        }
                    ) { _ ->
                        SettingsScaffold(
                            title = if (showingProfilesScreen) {
                                stringResource(R.string.dolby_profile_title)
                            } else {
                                stringResource(R.string.dolby_title)
                            },
                            actions = {
                                if (showingProfilesScreen) {
                                    IconButton(onClick = {
                                        importLauncher.launch(arrayOf("application/json"))
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.FileDownload,
                                            contentDescription = stringResource(R.string.dolby_import_profile)
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            AnimatedContent(
                                targetState = showingProfilesScreen,
                                transitionSpec = {
                                    if (targetState) {
                                        slideInHorizontally { width -> width } togetherWith
                                                slideOutHorizontally { width -> -width }
                                    } else {
                                        slideInHorizontally { width -> -width } togetherWith
                                                slideOutHorizontally { width -> width }
                                    }
                                },
                                label = "DolbyScreenTransition"
                            ) { isProfiles ->
                                if (isProfiles) {
                                    val backScale = if (isPredictiveBackActive) (1f - predictiveBackProgress * 0.08f) else 1f
                                    val backCorner = if (isPredictiveBackActive) (24.dp * predictiveBackProgress) else 0.dp
                                    ProfilesScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { showingProfilesScreen = false },
                                        showAddDialog = showAddProfileDialog,
                                        onDismissAddDialog = { showAddProfileDialog = false },
                                        modifier = Modifier
                                            .padding(paddingValues)
                                            .graphicsLayer {
                                                scaleX = backScale
                                                scaleY = backScale
                                                clip = isPredictiveBackActive
                                                shape = RoundedCornerShape(backCorner)
                                            }
                                    )
                                } else {
                                    DolbyScreen(
                                        viewModel = viewModel,
                                        onNavigateToProfiles = { showingProfilesScreen = true },
                                        modifier = Modifier.padding(paddingValues)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
