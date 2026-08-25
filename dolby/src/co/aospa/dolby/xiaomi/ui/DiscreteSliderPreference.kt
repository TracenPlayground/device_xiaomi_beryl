/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsOpacity.alphaForEnabled
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.isSpaExpressiveEnabled
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SettingsTitle
import kotlin.math.roundToInt

@Composable
fun DiscreteSliderPreference(
    title: String,
    icon: @Composable (() -> Unit)? = null,
    summary: String? = null,
    values: List<Int>,
    entries: List<String>,
    currentValue: Int,
    enabled: Boolean = true,
    disabledSummary: String? = null,
    onValueChangeFinished: (Int) -> Unit
) {
    val currentIndex = remember(currentValue, values) {
        val idx = values.indexOf(currentValue)
        if (idx >= 0) idx else 0
    }

    var sliderIndex by remember(currentIndex) {
        mutableFloatStateOf(currentIndex.toFloat())
    }

    val activeDisplayIndex = sliderIndex.roundToInt().coerceIn(0, (values.size - 1).coerceAtLeast(0))
    val currentStatusText = entries.getOrElse(activeDisplayIndex) { "" }

    val alphaModifier = Modifier.alphaForEnabled(enabled)
    val surfaceBright = MaterialTheme.colorScheme.surfaceBright

    val displayedSummary = if (!enabled && !disabledSummary.isNullOrEmpty()) {
        disabledSummary
    } else {
        summary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = surfaceBright,
                shape = if (isSpaExpressiveEnabled) SettingsShape.CornerExtraSmall2 else RectangleShape
            )
            .padding(
                start = SettingsDimension.itemPaddingStart,
                end = SettingsDimension.itemPaddingEnd,
                top = SettingsSpace.small2,
                bottom = SettingsSpace.small2
            )
            .semantics(mergeDescendants = true) {
                contentDescription = title
                stateDescription = if (enabled) currentStatusText else (disabledSummary ?: "")
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = alphaModifier.size(SettingsDimension.itemIconContainerSize),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(modifier = Modifier.width(SettingsSpace.extraSmall6))
            }

            Column(
                modifier = alphaModifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsTitle(
                            title = title,
                            useMediumWeight = true
                        )
                    }
                    if (enabled && currentStatusText.isNotEmpty()) {
                        Text(
                            text = currentStatusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                if (!displayedSummary.isNullOrEmpty()) {
                    SettingsBody(body = displayedSummary)
                }
            }
        }

        if (values.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = sliderIndex,
                onValueChange = {
                    if (enabled) {
                        sliderIndex = it
                    }
                },
                onValueChangeFinished = {
                    val targetIndex = sliderIndex.roundToInt().coerceIn(0, values.size - 1)
                    val targetValue = values[targetIndex]
                    onValueChangeFinished(targetValue)
                },
                valueRange = 0f..(values.size - 1).toFloat(),
                steps = (values.size - 2).coerceAtLeast(0),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveTickColor = MaterialTheme.colorScheme.outline
                ),
                modifier = alphaModifier.fillMaxWidth()
            )
        }
    }
}
