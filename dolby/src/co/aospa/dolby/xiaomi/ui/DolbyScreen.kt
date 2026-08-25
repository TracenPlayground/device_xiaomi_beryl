/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.aospa.dolby.xiaomi.R
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dsOn by viewModel.dsOn.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val presetName by viewModel.presetName.collectAsState()
    val ieqPreset by viewModel.ieqPreset.collectAsState()
    val speakerVirtEnabled by viewModel.speakerVirtEnabled.collectAsState()
    val headphoneVirtEnabled by viewModel.headphoneVirtEnabled.collectAsState()
    val stereoWideningAmount by viewModel.stereoWideningAmount.collectAsState()
    val dialogueEnhancerAmount by viewModel.dialogueEnhancerAmount.collectAsState()
    val bassEnhancerEnabled by viewModel.bassEnhancerEnabled.collectAsState()
    val volumeLevelerEnabled by viewModel.volumeLevelerEnabled.collectAsState()
    val isOnSpeaker by viewModel.isOnSpeaker.collectAsState()
    val outputDeviceTitle by viewModel.audioOutputDeviceTitle.collectAsState()

    val profileEntries = stringArrayResource(R.array.dolby_profile_entries)
    val profileValues = stringArrayResource(R.array.dolby_profile_values)
    val ieqEntries = stringArrayResource(R.array.dolby_ieq_entries)
    val ieqValues = stringArrayResource(R.array.dolby_ieq_values)
    val stereoEntries = stringArrayResource(R.array.dolby_stereo_entries)
    val stereoValues = stringArrayResource(R.array.dolby_stereo_values)
    val dialogueEntries = stringArrayResource(R.array.dolby_dialogue_entries)
    val dialogueValues = stringArrayResource(R.array.dolby_dialogue_values)

    val connectHeadphonesText = stringResource(R.string.dolby_connect_headphones)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Main Switch with Audio output subtitle
        MainSwitchPreference(
            model = remember(dsOn, outputDeviceTitle) {
                object : SwitchPreferenceModel {
                    override val title = context.getString(R.string.dolby_enable)
                    override val summary = {
                        if (dsOn && outputDeviceTitle.isNotEmpty()) {
                            context.getString(R.string.dolby_output_format, outputDeviceTitle)
                        } else {
                            ""
                        }
                    }
                    override val checked = { dsOn }
                    override val changeable = { true }
                    override val onCheckedChange = { checked: Boolean ->
                        viewModel.setDsOn(checked)
                    }
                }
            }
        )

        // Profile Selector Category
        Category {
            IconListPreference(
                model = remember(profile, dsOn) {
                    object : IconListPreferenceModel {
                        override val title = context.getString(R.string.dolby_profile_title)
                        override val icon = @Composable {
                            val profileIconRes = when (profile) {
                                1 -> R.drawable.ic_dolby_movie
                                2 -> R.drawable.ic_dolby_music
                                8 -> R.drawable.ic_dolby_voice
                                else -> R.drawable.ic_dolby_dynamic
                            }
                            Icon(
                                painter = painterResource(id = profileIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsDimension.itemIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        override val enabled = { dsOn }
                        override val options = profileValues.mapIndexed { index, valueStr ->
                            val id = valueStr.toInt()
                            val itemIconRes = when (id) {
                                1 -> R.drawable.ic_dolby_movie
                                2 -> R.drawable.ic_dolby_music
                                8 -> R.drawable.ic_dolby_voice
                                else -> R.drawable.ic_dolby_dynamic
                            }
                            IconListPreferenceOption(
                                id = id,
                                text = profileEntries.getOrElse(index) { valueStr },
                                iconRes = itemIconRes
                            )
                        }
                        override val selectedId = androidx.compose.runtime.mutableIntStateOf(profile)
                        override val onIdSelected: (Int) -> Unit = { selected ->
                            viewModel.setProfile(selected)
                        }
                    }
                }
            )
        }

        // Settings Category (Segmented list items)
        Category(title = stringResource(R.string.dolby_category_settings)) {
            // Graphic Equalizer Navigation
            Preference(
                model = remember(dsOn, presetName) {
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

            // Intelligent Equalizer
            IconListPreference(
                model = remember(ieqPreset, dsOn) {
                    object : IconListPreferenceModel {
                        override val title = context.getString(R.string.dolby_ieq)
                        override val icon = @Composable {
                            val iconRes = when (ieqPreset) {
                                1 -> R.drawable.ic_ieq_balanced
                                2 -> R.drawable.ic_ieq_warm
                                3 -> R.drawable.ic_ieq_detailed
                                else -> R.drawable.ic_ieq_off
                            }
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsDimension.itemIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        override val enabled = { dsOn }
                        override val options = ieqValues.mapIndexed { index, valueStr ->
                            val id = valueStr.toInt()
                            val itemIconRes = when (id) {
                                1 -> R.drawable.ic_ieq_balanced
                                2 -> R.drawable.ic_ieq_warm
                                3 -> R.drawable.ic_ieq_detailed
                                else -> R.drawable.ic_ieq_off
                            }
                            IconListPreferenceOption(
                                id = id,
                                text = ieqEntries.getOrElse(index) { valueStr },
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

            // Stereo Widening (Discrete slider inside container list with left icon)
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

            // Dialogue Enhancer (Discrete slider with person speaking icon on left)
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
                            SettingsIcon(imageVector = Icons.Outlined.VolumeUp)
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
                            SettingsIcon(imageVector = Icons.Outlined.VolumeUp)
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
                            val profileName = profileEntries.getOrElse(profile) { "" }
                            Toast.makeText(
                                context,
                                context.getString(R.string.dolby_reset_profile_toast, profileName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        // Backup & Restore Category
        Category(title = stringResource(R.string.dolby_category_backup)) {
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    val success = viewModel.exportSettings(uri)
                    Toast.makeText(
                        context,
                        if (success) R.string.dolby_export_success else R.string.dolby_export_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    val success = viewModel.importSettings(uri)
                    Toast.makeText(
                        context,
                        if (success) R.string.dolby_import_success else R.string.dolby_import_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Export Settings
            Preference(
                model = remember {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_export_settings)
                        override val summary = { context.getString(R.string.dolby_export_settings_summary) }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.FileUpload)
                        }
                        override val onClick = {
                            exportLauncher.launch("dolby_settings.json")
                        }
                    }
                }
            )

            // Import Settings
            Preference(
                model = remember {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_import_settings)
                        override val summary = { context.getString(R.string.dolby_import_settings_summary) }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.FileDownload)
                        }
                        override val onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    }
                }
            )

            // Add Quick Settings Tile
            Preference(
                model = remember {
                    object : PreferenceModel {
                        override val title = context.getString(R.string.dolby_qs_add_tile)
                        override val summary = { context.getString(R.string.dolby_qs_add_tile_summary) }
                        override val icon = @Composable {
                            SettingsIcon(imageVector = Icons.Outlined.Tune)
                        }
                        override val onClick = {
                            viewModel.requestAddQsTile(context)
                        }
                    }
                }
            )
        }
    }
}
