/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import co.aospa.dolby.xiaomi.R

@Composable
fun PresetNameDialog(
    title: String,
    initialName: String = "",
    icon: @Composable (() -> Unit)? = null,
    existingNames: List<String> = emptyList(),
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    var error by remember { mutableStateOf<PresetNameValidationError?>(null) }

    fun validate(input: String): Boolean {
        val trimmed = input.trim()
        return when {
            trimmed.isEmpty() -> {
                error = null
                false
            }
            trimmed.length > 20 -> {
                error = PresetNameValidationError.TOO_LONG
                false
            }
            existingNames.any { it.equals(trimmed, ignoreCase = true) } -> {
                error = PresetNameValidationError.ALREADY_EXISTS
                false
            }
            else -> {
                error = null
                true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        shape = AlertDialogDefaults.shape,
        containerColor = AlertDialogDefaults.containerColor,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        validate(it)
                    },
                    label = { Text(stringResource(R.string.dolby_geq_preset_name)) },
                    isError = error != null,
                    supportingText = {
                        error?.let {
                            Text(stringResource(it.errorMessageRes))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validate(text)) {
                        onConfirm(text.trim())
                    }
                },
                enabled = text.trim().isNotEmpty() && error == null
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
