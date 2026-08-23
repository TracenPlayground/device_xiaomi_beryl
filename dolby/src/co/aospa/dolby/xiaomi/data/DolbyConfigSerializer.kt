/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.data

import android.content.Context
import android.net.Uri
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import co.aospa.dolby.xiaomi.geq.data.Preset
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object DolbyConfigSerializer {

    private const val TAG = "DolbyConfigSerializer"
    private const val KEY_VERSION = "version"
    private const val CURRENT_VERSION = 1

    private const val KEY_DOLBY_ENABLED = "dolby_enabled"
    private const val KEY_CURRENT_PROFILE = "current_profile"
    private const val KEY_PROFILES = "profiles"

    private const val KEY_PROFILE_ID = "profile_id"
    private const val KEY_IEQ_PRESET = "ieq_preset"
    private const val KEY_DIALOGUE_AMOUNT = "dialogue_amount"
    private const val KEY_BASS_ENABLED = "bass_enabled"
    private const val KEY_STEREO_AMOUNT = "stereo_amount"
    private const val KEY_VOLUME_ENABLED = "volume_enabled"
    private const val KEY_HP_VIRT_ENABLED = "hp_virt_enabled"
    private const val KEY_SPK_VIRT_ENABLED = "spk_virt_enabled"

    private const val KEY_PRESETS = "presets"
    private const val KEY_PRESET_ID = "preset_id"
    private const val KEY_PRESET_NAME = "preset_name"
    private const val KEY_PRESET_GAINS = "band_gains"

    fun exportToJson(context: Context): String {
        val root = JSONObject()
        root.put(KEY_VERSION, CURRENT_VERSION)

        val dolbyController = DolbyController.getInstance(context)
        val eqRepo = EqualizerRepository.getInstance(context)

        root.put(KEY_DOLBY_ENABLED, dolbyController.dsOn)
        root.put(KEY_CURRENT_PROFILE, dolbyController.profile)

        val profilesArray = JSONArray()
        val originalProfile = dolbyController.profile
        val profileValues = context.resources.getStringArray(R.array.dolby_profile_values)

        for (profileValStr in profileValues) {
            val profileId = profileValStr.toInt()
            dolbyController.profile = profileId

            val profileObj = JSONObject().apply {
                put(KEY_PROFILE_ID, profileId)
                put(KEY_IEQ_PRESET, dolbyController.ieqPreset)
                put(KEY_DIALOGUE_AMOUNT, dolbyController.dialogueEnhancerAmount)
                put(KEY_BASS_ENABLED, dolbyController.bassEnhancerEnabled)
                put(KEY_STEREO_AMOUNT, dolbyController.stereoWideningAmount)
                put(KEY_VOLUME_ENABLED, dolbyController.volumeLevelerEnabled)
                put(KEY_HP_VIRT_ENABLED, dolbyController.headphoneVirtEnabled)
                put(KEY_SPK_VIRT_ENABLED, dolbyController.speakerVirtEnabled)
            }
            profilesArray.put(profileObj)
        }
        dolbyController.profile = originalProfile
        root.put(KEY_PROFILES, profilesArray)

        val presetsArray = JSONArray()
        val presets = eqRepo.getAllPresets()
        for (preset in presets) {
            val presetObj = JSONObject().apply {
                put(KEY_PRESET_ID, preset.id)
                put(KEY_PRESET_NAME, preset.name)
                val gainsArray = JSONArray()
                preset.bandGains.forEach { gainsArray.put(it.gain) }
                put(KEY_PRESET_GAINS, gainsArray)
            }
            presetsArray.put(presetObj)
        }
        root.put(KEY_PRESETS, presetsArray)

        return root.toString(2)
    }

    fun importFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val dolbyController = DolbyController.getInstance(context)
            val eqRepo = EqualizerRepository.getInstance(context)

            if (root.has(KEY_PROFILES)) {
                val profilesArray = root.getJSONArray(KEY_PROFILES)
                for (i in 0 until profilesArray.length()) {
                    val profileObj = profilesArray.getJSONObject(i)
                    val profileId = profileObj.getInt(KEY_PROFILE_ID)
                    dolbyController.profile = profileId

                    if (profileObj.has(KEY_IEQ_PRESET)) {
                        dolbyController.ieqPreset = profileObj.getInt(KEY_IEQ_PRESET)
                    }
                    if (profileObj.has(KEY_DIALOGUE_AMOUNT)) {
                        dolbyController.dialogueEnhancerAmount = profileObj.getInt(KEY_DIALOGUE_AMOUNT)
                    }
                    if (profileObj.has(KEY_BASS_ENABLED)) {
                        dolbyController.bassEnhancerEnabled = profileObj.getBoolean(KEY_BASS_ENABLED)
                    }
                    if (profileObj.has(KEY_STEREO_AMOUNT)) {
                        dolbyController.stereoWideningAmount = profileObj.getInt(KEY_STEREO_AMOUNT)
                    }
                    if (profileObj.has(KEY_VOLUME_ENABLED)) {
                        dolbyController.volumeLevelerEnabled = profileObj.getBoolean(KEY_VOLUME_ENABLED)
                    }
                    if (profileObj.has(KEY_HP_VIRT_ENABLED)) {
                        dolbyController.headphoneVirtEnabled = profileObj.getBoolean(KEY_HP_VIRT_ENABLED)
                    }
                    if (profileObj.has(KEY_SPK_VIRT_ENABLED)) {
                        dolbyController.speakerVirtEnabled = profileObj.getBoolean(KEY_SPK_VIRT_ENABLED)
                    }
                }
            }

            if (root.has(KEY_CURRENT_PROFILE)) {
                dolbyController.profile = root.getInt(KEY_CURRENT_PROFILE)
            }
            if (root.has(KEY_DOLBY_ENABLED)) {
                dolbyController.dsOn = root.getBoolean(KEY_DOLBY_ENABLED)
            }

            if (root.has(KEY_PRESETS)) {
                val presetsArray = root.getJSONArray(KEY_PRESETS)
                for (i in 0 until presetsArray.length()) {
                    val presetObj = presetsArray.getJSONObject(i)
                    val presetId = presetObj.getInt(KEY_PRESET_ID)
                    val presetName = presetObj.getString(KEY_PRESET_NAME)
                    val gainsArray = presetObj.getJSONArray(KEY_PRESET_GAINS)
                    val gains = IntArray(gainsArray.length()) { gainsArray.getInt(it) }

                    if (eqRepo.getPreset(presetId) != null) {
                        eqRepo.updatePresetGains(presetId, gains)
                        eqRepo.renamePreset(presetId, presetName)
                    } else {
                        val newPreset = eqRepo.addPreset(presetName)
                        eqRepo.updatePresetGains(newPreset.id, gains)
                    }
                }
            }

            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import settings: ${e.message}")
            false
        }
    }

    fun exportToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = exportToJson(context)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to export settings to URI: ${e.message}")
            false
        }
    }

    fun importFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return false
            importFromJson(context, json)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import settings from URI: ${e.message}")
            false
        }
    }
}
