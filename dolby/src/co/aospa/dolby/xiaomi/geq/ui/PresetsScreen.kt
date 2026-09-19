/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.ui

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.data.Preset

@Composable
fun PresetsScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit,
    showAddDialog: Boolean = false,
    onDismissAddDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val orderedPresets = remember { mutableStateListOf<Preset>() }
    LaunchedEffect(presets) {
        if (draggingIndex == null) {
            orderedPresets.clear()
            orderedPresets.addAll(presets)
        }
    }

    var presetToRename by remember { mutableStateOf<Preset?>(null) }
    var presetToDelete by remember { mutableStateOf<Preset?>(null) }

    val itemHeightPx = 72f * context.resources.displayMetrics.density

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(orderedPresets, key = { _, item -> item.id }) { index, preset ->
            val isSelected = preset.id == currentPreset?.id
            val isDragging = draggingIndex == index

            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.03f else 1.0f,
                label = "cardScale"
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
                            viewModel.selectPreset(preset)
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
                                            .coerceIn(0, orderedPresets.lastIndex)

                                        if (targetIndex != currentIndex) {
                                            val item = orderedPresets.removeAt(currentIndex)
                                            orderedPresets.add(targetIndex, item)
                                            dragOffsetY -= (targetIndex - currentIndex) * itemHeightPx
                                            draggingIndex = targetIndex
                                        }
                                    },
                                    onDragEnd = {
                                        val finalOrder = orderedPresets.map { it.id }
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        viewModel.reorderPresets(finalOrder)
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        orderedPresets.clear()
                                        orderedPresets.addAll(presets)
                                    }
                                )
                            }
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
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

                    if (!preset.isReadOnly) {
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
                                    text = { Text(stringResource(R.string.dolby_geq_rename_preset)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        presetToRename = preset
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            stringResource(R.string.dolby_geq_delete_preset),
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
                                        presetToDelete = preset
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
        PresetNameDialog(
            title = stringResource(R.string.dolby_geq_new_preset),
            initialName = "",
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            },
            existingNames = presets.map { it.name },
            onConfirm = { name ->
                viewModel.addPreset(name)
                onDismissAddDialog()
            },
            onDismiss = onDismissAddDialog
        )
    }

    presetToRename?.let { preset ->
        PresetNameDialog(
            title = stringResource(R.string.dolby_geq_rename_preset),
            initialName = preset.name,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null
                )
            },
            existingNames = presets.filter { it.id != preset.id }.map { it.name },
            onConfirm = { newName ->
                viewModel.renamePreset(preset.id, newName)
                presetToRename = null
            },
            onDismiss = { presetToRename = null }
        )
    }

    presetToDelete?.let { preset ->
        ConfirmationDialog(
            title = stringResource(R.string.dolby_geq_delete_preset),
            message = stringResource(R.string.dolby_geq_delete_preset_prompt),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onConfirm = {
                viewModel.deletePreset(preset.id)
                presetToDelete = null
            },
            onDismiss = { presetToDelete = null }
        )
    }
}
