/*
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel

data class IconListPreferenceOption(
    val id: Int,
    val text: String,
    val iconRes: Int? = null,
    val summary: String = ""
)

interface IconListPreferenceModel {
    val title: String
    val icon: (@Composable () -> Unit)?
        get() = null
    val enabled: () -> Boolean
        get() = { true }
    val options: List<IconListPreferenceOption>
    val selectedId: IntState
    val onIdSelected: (id: Int) -> Unit
}

@Composable
fun IconListPreference(model: IconListPreferenceModel) {
    var dialogOpened by rememberSaveable { mutableStateOf(false) }

    if (dialogOpened) {
        AlertDialog(
            onDismissRequest = { dialogOpened = false },
            title = {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .verticalScroll(rememberScrollState())
                ) {
                    for (option in model.options) {
                        val isSelected = option.id == model.selectedId.intValue
                        val isEnabled = model.enabled()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .selectable(
                                    selected = isSelected,
                                    enabled = isEnabled,
                                    onClick = {
                                        dialogOpened = false
                                        model.onIdSelected(option.id)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                enabled = isEnabled
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            if (option.iconRes != null) {
                                Icon(
                                    painter = painterResource(id = option.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (option.summary.isNotEmpty()) {
                                    Text(
                                        text = option.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { dialogOpened = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            shape = AlertDialogDefaults.shape,
            containerColor = AlertDialogDefaults.containerColor
        )
    }

    Preference(
        model = remember(model) {
            object : PreferenceModel {
                override val title = model.title
                override val summary = {
                    model.options.find { it.id == model.selectedId.intValue }?.text ?: ""
                }
                override val icon = model.icon
                override val enabled = model.enabled
                override val onClick = { dialogOpened = true }.takeIf { model.options.isNotEmpty() }
            }
        }
    )
}
