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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
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

    val profileEntries = stringArrayResource(R.array.dolby_profile_entries)
    val profileValues = stringArrayResource(R.array.dolby_profile_values)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
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

            Surface(
                color = if (isDsOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val newState = !isDsOn
                        isDsOn = newState
                        onDsOnChanged(newState)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dolby_enable),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDsOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isDsOn,
                        onCheckedChange = { checked ->
                            isDsOn = checked
                            onDsOnChanged(checked)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in profileValues.indices) {
                    val profileId = profileValues[i].toInt()
                    val profileName = profileEntries.getOrElse(i) { profileValues[i] }
                    val isSelected = (profileId == currentProfile) && isDsOn

                    val tileColor: Color by animateColorAsState(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isDsOn -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                    val contentColor: Color by animateColorAsState(
                        when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isDsOn -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )

                    Surface(
                        color = tileColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = isDsOn) {
                                currentProfile = profileId
                                onProfileSelected(profileId)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dolby),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = contentColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = profileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = contentColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = contentColor
                                )
                            }
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
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dolby_category_settings),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dolby_done),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
