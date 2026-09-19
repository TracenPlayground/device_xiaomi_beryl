/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.thenIf
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsOpacity.alphaForEnabled
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.isSpaExpressiveEnabled
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SettingsTitle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val showValueIndicator = isPressed || isDragged

    val alphaModifier = Modifier.alphaForEnabled(enabled)
    val surfaceBright = MaterialTheme.colorScheme.surfaceBright

    val displayedSummary = if (!enabled && !disabledSummary.isNullOrEmpty()) {
        disabledSummary
    } else {
        summary
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = MaterialTheme.colorScheme.onPrimary,
        inactiveTickColor = MaterialTheme.colorScheme.outline
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(isSpaExpressiveEnabled) {
                Modifier.heightIn(min = SettingsDimension.preferenceMinHeight)
            }
            .thenIf(isSpaExpressiveEnabled) {
                Modifier.background(
                    color = surfaceBright,
                    shape = SettingsShape.CornerExtraSmall2,
                )
            }
            .padding(end = SettingsDimension.itemPaddingEnd)
            .padding(vertical = SettingsDimension.itemPaddingVertical)
            .semantics(mergeDescendants = true) {
                contentDescription = title
                stateDescription = if (enabled) currentStatusText else (disabledSummary ?: "")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = alphaModifier.size(SettingsDimension.itemIconContainerSize),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        } else {
            Spacer(modifier = Modifier.width(SettingsDimension.itemPaddingStart))
        }

        Column(
            modifier = alphaModifier
                .weight(1f)
                .padding(end = SettingsSpace.small1)
        ) {
            SettingsTitle(
                title = title,
                useMediumWeight = true
            )
            if (!displayedSummary.isNullOrEmpty()) {
                SettingsBody(body = displayedSummary)
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
                    interactionSource = interactionSource,
                    colors = sliderColors,
                    thumb = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(width = 4.dp, height = 44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 44.dp)
                                    .background(
                                        color = if (enabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                            if (showValueIndicator && enabled) {
                                Box(
                                    modifier = Modifier.layout { measurable, _ ->
                                        val placeable = measurable.measure(Constraints())
                                        layout(0, 0) {
                                            val x = (4.dp.roundToPx() - placeable.width) / 2
                                            val y = -placeable.height - 10.dp.roundToPx()
                                            placeable.placeRelative(x, y)
                                        }
                                    }
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        shadowElevation = 2.dp
                                    ) {
                                        Text(
                                            text = currentStatusText,
                                            color = MaterialTheme.colorScheme.inverseOnSurface,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
