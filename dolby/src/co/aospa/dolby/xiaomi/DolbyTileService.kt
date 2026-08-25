/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.MainSwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import co.aospa.dolby.xiaomi.data.ProfileRepository

class DolbyTileService : TileService() {

    private val dolbyController by lazy { DolbyController.getInstance(applicationContext) }

    override fun onStartListening() {
        updateTileState()
        super.onStartListening()
    }

    override fun onClick() {
        if (isLocked) {
            unlockAndRun {
                showProfileDialog()
            }
        } else {
            showProfileDialog()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.state = if (dolbyController.dsOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (dolbyController.dsOn) {
            dolbyController.getProfileName() ?: getString(R.string.dolby_unknown)
        } else {
            getString(R.string.dolby_off)
        }
        tile.updateTile()
    }

    private fun showProfileDialog() {
        var dialog: AlertDialog? = null

        val lifecycleOwner = DialogLifecycleOwner().apply {
            onCreate()
            onResume()
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                SettingsTheme {
                    DolbyQsDialogContent(
                        initialDsOn = dolbyController.dsOn,
                        initialProfile = dolbyController.profile,
                        onDsOnChanged = { isChecked ->
                            dolbyController.dsOn = isChecked
                            updateTileState()
                        },
                        onProfileSelected = { selectedProfile ->
                            dolbyController.profile = selectedProfile
                            if (!dolbyController.dsOn) {
                                dolbyController.dsOn = true
                            }
                            updateTileState()
                        },
                        onOpenSettings = {
                            dialog?.dismiss()
                            val intent = Intent(this@DolbyTileService, DolbyActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val pendingIntent = PendingIntent.getActivity(
                                this@DolbyTileService,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            startActivityAndCollapse(pendingIntent)
                        },
                        onDismiss = {
                            dialog?.dismiss()
                        }
                    )
                }
            }
        }

        dialog = AlertDialog.Builder(this)
            .setView(composeView)
            .setOnDismissListener {
                lifecycleOwner.onDestroy()
            }
            .create()

        dialog.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        showDialog(dialog)
    }
}

private class DialogLifecycleOwner : SavedStateRegistryOwner {
    override val lifecycle = LifecycleRegistry(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val savedStateRegistryController =
        SavedStateRegistryController.create(this).apply { performAttach() }

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }
}

@Composable
private fun DolbyQsDialogContent(
    initialDsOn: Boolean,
    initialProfile: Int,
    onDsOnChanged: (Boolean) -> Unit,
    onProfileSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var isDsOn by remember { mutableStateOf(initialDsOn) }
    var currentProfile by remember { mutableIntStateOf(initialProfile) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val profiles = remember { ProfileRepository.getInstance(context).getAllProfiles() }

    Surface(
        color = AlertDialogDefaults.containerColor,
        shape = AlertDialogDefaults.shape,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.dolby_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.dolby_qs_select_profile),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val enableTitle = stringResource(R.string.dolby_enable)
            MainSwitchPreference(
                model = remember(isDsOn, enableTitle) {
                    object : SwitchPreferenceModel {
                        override val title = enableTitle
                        override val checked = { isDsOn }
                        override val changeable = { true }
                        override val onCheckedChange = { checked: Boolean ->
                            isDsOn = checked
                            onDsOnChanged(checked)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (profile in profiles) {
                    val profileId = profile.id
                    val isSelected = (profileId == currentProfile) && isDsOn
                    val isEnabled = isDsOn

                    Surface(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .selectable(
                                selected = isSelected,
                                enabled = isEnabled,
                                onClick = {
                                    currentProfile = profileId
                                    onProfileSelected(profileId)
                                },
                                role = Role.RadioButton
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isEnabled) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = profile.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else if (isEnabled) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else if (isEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                                if (isSelected) {
                                    Text(
                                        text = stringResource(R.string.dolby_on),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                enabled = isEnabled
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text(
                        text = stringResource(R.string.dolby_category_settings),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.dolby_done),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
