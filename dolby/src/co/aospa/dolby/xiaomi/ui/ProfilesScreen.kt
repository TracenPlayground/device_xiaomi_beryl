/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.data.DolbyProfile
import co.aospa.dolby.xiaomi.geq.ui.ConfirmationDialog
import co.aospa.dolby.xiaomi.geq.ui.PresetNameDialog

@Composable
fun ProfilesScreen(
    viewModel: DolbyViewModel,
    onNavigateBack: () -> Unit,
    showAddDialog: Boolean = false,
    onDismissAddDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val currentProfile by viewModel.profile.collectAsStateWithLifecycle()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val orderedProfiles = remember { mutableStateListOf<DolbyProfile>() }
    LaunchedEffect(profiles) {
        if (draggingIndex == null) {
            orderedProfiles.clear()
            orderedProfiles.addAll(profiles)
        }
    }

    var profileToRename by remember { mutableStateOf<DolbyProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<DolbyProfile?>(null) }
    var profileToExport by remember { mutableStateOf<DolbyProfile?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && profileToExport != null) {
            val success = viewModel.exportProfile(profileToExport!!.id, uri)
            Toast.makeText(
                context,
                if (success) R.string.dolby_export_success else R.string.dolby_export_failed,
                Toast.LENGTH_SHORT
            ).show()
        }
        profileToExport = null
    }

    val itemHeightPx = 72f * context.resources.displayMetrics.density

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        itemsIndexed(orderedProfiles, key = { _, item -> item.id }) { index, profileItem ->
            val isSelected = profileItem.id == currentProfile
            val isDragging = draggingIndex == index

            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.03f else 1.0f,
                label = "profileCardScale"
            )

            var menuExpanded by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .zIndex(if (isDragging) 2f else 0f)
                    .graphicsLayer {
                        if (isDragging) {
                            translationY = dragOffsetY
                        }
                    }
                    .scale(scale)
                    .then(if (isDragging) Modifier else Modifier.animateItem())
                    .clickable {
                        if (draggingIndex == null) {
                            viewModel.setProfile(profileItem.id)
                            onNavigateBack()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                        val targetIndex = (currentIndex + (dragOffsetY / itemHeightPx).toInt())
                                            .coerceIn(0, orderedProfiles.lastIndex)

                                        if (targetIndex != currentIndex) {
                                            val item = orderedProfiles.removeAt(currentIndex)
                                            orderedProfiles.add(targetIndex, item)
                                            dragOffsetY -= (targetIndex - currentIndex) * itemHeightPx
                                            draggingIndex = targetIndex
                                        }
                                    },
                                    onDragEnd = {
                                        val finalOrder = orderedProfiles.map { it.id }
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        viewModel.reorderProfiles(finalOrder)
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        orderedProfiles.clear()
                                        orderedProfiles.addAll(profiles)
                                    }
                                )
                            }
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        painter = painterResource(id = profileItem.iconRes),
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profileItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 4.dp)
                        )
                    }

                    if (profileItem.isCustom) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.dolby_export_profile)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        profileToExport = profileItem
                                        val fileName = "dolby_profile_${profileItem.name.lowercase().replace(" ", "_")}.json"
                                        exportLauncher.launch(fileName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.dolby_profile_rename)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        profileToRename = profileItem
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.dolby_profile_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        profileToDelete = profileItem
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showAddDialog) {
        CreateProfileDialog(
            existingNames = profiles.map { it.name },
            onConfirm = { name, baseProfileId ->
                viewModel.createProfile(name, baseProfileId)
                onDismissAddDialog()
            },
            onDismiss = onDismissAddDialog
        )
    }

    profileToRename?.let { profileItem ->
        PresetNameDialog(
            title = stringResource(R.string.dolby_profile_rename),
            initialName = profileItem.name,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null
                )
            },
            existingNames = profiles.filter { it.id != profileItem.id }.map { it.name },
            onConfirm = { newName ->
                viewModel.renameProfile(profileItem.id, newName)
                profileToRename = null
            },
            onDismiss = { profileToRename = null }
        )
    }

    profileToDelete?.let { profileItem ->
        ConfirmationDialog(
            title = stringResource(R.string.dolby_profile_delete),
            message = stringResource(R.string.dolby_profile_delete_prompt),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onConfirm = {
                viewModel.deleteProfile(profileItem.id)
                profileToDelete = null
            },
            onDismiss = { profileToDelete = null }
        )
    }
}

@Composable
private fun CreateProfileDialog(
    existingNames: List<String>,
    onConfirm: (name: String, baseProfileId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedBaseId by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val baseProfiles = listOf(
        0 to stringResource(R.string.dolby_profile_dynamic),
        1 to stringResource(R.string.dolby_profile_video),
        2 to stringResource(R.string.dolby_profile_music),
        8 to stringResource(R.string.dolby_profile_voice)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
        title = { Text(stringResource(R.string.dolby_profile_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.dolby_profile_name_label)) },
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.dolby_base_profile),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                baseProfiles.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBaseId = id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedBaseId == id),
                            onClick = { selectedBaseId = id }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isEmpty() -> return@TextButton
                        trimmed.length > 30 -> errorText = "Name too long"
                        existingNames.any { it.equals(trimmed, ignoreCase = true) } -> errorText = "Name already exists"
                        else -> onConfirm(trimmed, selectedBaseId)
                    }
                }
            ) {
                Text(stringResource(R.string.dolby_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
