/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.data.DolbyProfile
import co.aospa.dolby.xiaomi.geq.EqualizerActivity
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.MainSwitchPreference
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.SettingsIcon

@Composable
fun DolbyScreen(
    viewModel: DolbyViewModel,
    onNavigateToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dsOn by viewModel.dsOn.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val presetName by viewModel.presetName.collectAsStateWithLifecycle()
    val ieqPreset by viewModel.ieqPreset.collectAsStateWithLifecycle()
    val speakerVirtEnabled by viewModel.speakerVirtEnabled.collectAsStateWithLifecycle()
    val headphoneVirtEnabled by viewModel.headphoneVirtEnabled.collectAsStateWithLifecycle()
    val stereoWideningAmount by viewModel.stereoWideningAmount.collectAsStateWithLifecycle()
    val dialogueEnhancerAmount by viewModel.dialogueEnhancerAmount.collectAsStateWithLifecycle()
    val bassEnhancerEnabled by viewModel.bassEnhancerEnabled.collectAsStateWithLifecycle()
    val volumeLevelerEnabled by viewModel.volumeLevelerEnabled.collectAsStateWithLifecycle()
    val isOnSpeaker by viewModel.isOnSpeaker.collectAsStateWithLifecycle()
    val connectedAudioDevice by viewModel.connectedAudioDevice.collectAsStateWithLifecycle()
    val isQsTileAdded by viewModel.isQsTileAdded.collectAsStateWithLifecycle()

    val ieqEntries = stringArrayResource(R.array.dolby_ieq_entries)
    val ieqValues = stringArrayResource(R.array.dolby_ieq_values)
    val stereoEntries = stringArrayResource(R.array.dolby_stereo_entries)
    val stereoValues = stringArrayResource(R.array.dolby_stereo_values)
    val dialogueEntries = stringArrayResource(R.array.dolby_dialogue_entries)
    val dialogueValues = stringArrayResource(R.array.dolby_dialogue_values)

    val connectHeadphonesText = stringResource(R.string.dolby_connect_headphones)

    val currentProfileObj = remember(profiles, profile) {
        profiles.firstOrNull { it.id == profile }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // SpaLib standard MainSwitchPreference
        MainSwitchPreference(
            model = remember(dsOn) {
                object : SwitchPreferenceModel {
                    override val title = context.getString(R.string.dolby_enable)
                    override val checked = { dsOn }
                    override val onCheckedChange = { checked: Boolean ->
                        viewModel.setDsOn(checked)
                    }
                }
            }
        )

        // Profiles Category
        Category(title = stringResource(R.string.dolby_category_profiles)) {
            Preference(
                model = remember(connectedAudioDevice, dsOn) {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_output_play_on)
                        override val summary = {
                            if (dsOn) connectedAudioDevice.title else context.getString(R.string.dolby_off)
                        }
                        override val icon = @Composable {
                            Icon(
                                painter = painterResource(id = connectedAudioDevice.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsDimension.itemIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        override val enabled = { true }
                    }
                }
            )

            Preference(
                model = remember(profile, currentProfileObj, dsOn) {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_profile_title)
                        override val summary = {
                            currentProfileObj?.name ?: context.getString(R.string.dolby_unknown)
                        }
                        override val icon = @Composable {
                            val iconRes = currentProfileObj?.iconRes ?: R.drawable.ic_dolby_dynamic
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsDimension.itemIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        override val enabled = { dsOn }
                        override val onClick = onNavigateToProfiles
                    }
                }
            )
        }

        // Equalizer Category
        Category(title = stringResource(R.string.dolby_preset)) {
            Preference(
                model = remember(presetName, dsOn) {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_preset)
                        override val summary = { presetName }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.GraphicEq)
                        }
                        override val enabled = { dsOn }
                        override val onClick = {
                            context.startActivity(Intent(context, EqualizerActivity::class.java))
                        }
                    }
                }
            )

            IconListPreference(
                model = remember(ieqPreset, dsOn) {
                    object : IconListPreferenceModel {
                        override val title = context.getString(R.string.dolby_ieq)
                        override val icon = @Composable {
                            val ieqIconRes = when (ieqPreset) {
                                1 -> R.drawable.ic_ieq_balanced
                                2 -> R.drawable.ic_ieq_warm
                                3 -> R.drawable.ic_ieq_detailed
                                else -> R.drawable.ic_ieq_off
                            }
                            Icon(
                                painter = painterResource(id = ieqIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsDimension.itemIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        override val enabled = { dsOn }
                        override val options = ieqEntries.mapIndexed { index, name ->
                            val value = ieqValues.getOrNull(index)?.toIntOrNull() ?: index
                            val itemIconRes = when (value) {
                                1 -> R.drawable.ic_ieq_balanced
                                2 -> R.drawable.ic_ieq_warm
                                3 -> R.drawable.ic_ieq_detailed
                                else -> R.drawable.ic_ieq_off
                            }
                            IconListPreferenceOption(
                                id = value,
                                text = name,
                                iconRes = itemIconRes
                            )
                        }
                        override val selectedId = androidx.compose.runtime.mutableIntStateOf(ieqPreset)
                        override val onIdSelected: (Int) -> Unit = { selected ->
                            viewModel.setIeqPreset(selected)
                        }
                    }
                }
            )
        }

        // Sound Effects Category
        Category(title = stringResource(R.string.dolby_category_settings)) {
            // Speaker Virtualization
            SwitchPreference(
                model = remember(speakerVirtEnabled, dsOn) {
                    object : SwitchPreferenceModel {
                        override val title = context.getString(R.string.dolby_spk_virtualizer)
                        override val summary = { context.getString(R.string.dolby_spk_virtualizer_summary) }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.Speaker)
                        }
                        override val checked = { speakerVirtEnabled }
                        override val changeable = { dsOn }
                        override val onCheckedChange = { checked: Boolean ->
                            viewModel.setSpeakerVirtEnabled(checked)
                        }
                    }
                }
            )

            // Headphone Virtualization
            SwitchPreference(
                model = remember(headphoneVirtEnabled, dsOn, isOnSpeaker) {
                    object : SwitchPreferenceModel {
                        override val title = context.getString(R.string.dolby_hp_virtualizer)
                        override val summary = {
                            if (isOnSpeaker) connectHeadphonesText
                            else context.getString(R.string.dolby_hp_virtualizer_summary)
                        }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.Headphones)
                        }
                        override val checked = { headphoneVirtEnabled }
                        override val changeable = { dsOn && !isOnSpeaker }
                        override val onCheckedChange = { checked: Boolean ->
                            viewModel.setHeadphoneVirtEnabled(checked)
                        }
                    }
                }
            )

            // Stereo Widening
            DiscreteSliderPreference(
                title = stringResource(R.string.dolby_stereo_widening),
                icon = {
                    SettingsIcon(imageVector = Icons.Outlined.SurroundSound)
                },
                summary = stringResource(R.string.dolby_stereo_widening_summary),
                values = remember(stereoValues) { stereoValues.map { it.toInt() } },
                entries = remember(stereoEntries) { stereoEntries.toList() },
                currentValue = stereoWideningAmount,
                enabled = dsOn && !isOnSpeaker,
                disabledSummary = if (isOnSpeaker) connectHeadphonesText else null,
                onValueChangeFinished = { viewModel.setStereoWideningAmount(it) }
            )

            // Dialogue Enhancer
            DiscreteSliderPreference(
                title = stringResource(R.string.dolby_dialogue_enhancer),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dialogue_enhancer),
                        contentDescription = null,
                        modifier = Modifier.size(SettingsDimension.itemIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                summary = stringResource(R.string.dolby_dialogue_enhancer_summary),
                values = remember(dialogueValues) { dialogueValues.map { it.toInt() } },
                entries = remember(dialogueEntries) { dialogueEntries.toList() },
                currentValue = dialogueEnhancerAmount,
                enabled = dsOn,
                onValueChangeFinished = { viewModel.setDialogueEnhancerAmount(it) }
            )

            // Bass Enhancer
            SwitchPreference(
                model = remember(bassEnhancerEnabled, dsOn, isOnSpeaker) {
                    object : SwitchPreferenceModel {
                        override val title = context.getString(R.string.dolby_bass_enhancer)
                        override val summary = {
                            if (isOnSpeaker) connectHeadphonesText
                            else context.getString(R.string.dolby_bass_enhancer_summary)
                        }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.AutoMirrored.Outlined.VolumeUp)
                        }
                        override val checked = { bassEnhancerEnabled }
                        override val changeable = { dsOn && !isOnSpeaker }
                        override val onCheckedChange = { checked: Boolean ->
                            viewModel.setBassEnhancerEnabled(checked)
                        }
                    }
                }
            )

            // Volume Leveler
            SwitchPreference(
                model = remember(volumeLevelerEnabled, dsOn) {
                    object : SwitchPreferenceModel {
                        override val title = context.getString(R.string.dolby_volume_leveler)
                        override val summary = { context.getString(R.string.dolby_volume_leveler_summary) }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.AutoMirrored.Outlined.VolumeUp)
                        }
                        override val checked = { volumeLevelerEnabled }
                        override val changeable = { dsOn }
                        override val onCheckedChange = { checked: Boolean ->
                            viewModel.setVolumeLevelerEnabled(checked)
                        }
                    }
                }
            )

            // Reset Profile Settings
            Preference(
                model = remember(dsOn, profile) {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_reset_profile)
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.RestartAlt)
                        }
                        override val enabled = { dsOn }
                        override val onClick = {
                            viewModel.resetCurrentProfile()
                            val name = currentProfileObj?.name ?: ""
                            Toast.makeText(
                                context,
                                context.getString(R.string.dolby_reset_profile_toast, name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

            // Add Quick Settings Tile
            Preference(
                model = remember(isQsTileAdded) {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_qs_add_tile)
                        override val summary = {
                            if (isQsTileAdded) {
                                context.getString(R.string.dolby_qs_tile_already_added)
                            } else {
                                context.getString(R.string.dolby_qs_add_tile_summary)
                            }
                        }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.Tune)
                        }
                        override val enabled = { !isQsTileAdded }
                        override val onClick = {
                            if (!isQsTileAdded) {
                                viewModel.requestAddQsTile(context)
                            }
                        }
                    }
                }
            )
        }
    }
}
