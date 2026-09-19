/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.data

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object DolbyConfigSerializer {

    private const val TAG = "DolbyConfigSerializer"
    private const val CURRENT_VERSION = 1

    private const val KEY_VERSION = "version"
    private const val KEY_TYPE = "type"
    private const val TYPE_DOLBY_PROFILE = "dolby_profile"

    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_BASE_PROFILE_ID = "base_profile_id"
    private const val KEY_SETTINGS = "settings"

    private const val KEY_IEQ_PRESET = "ieq_preset"
    private const val KEY_DIALOGUE_AMOUNT = "dialogue_amount"
    private const val KEY_BASS_ENABLED = "bass_enabled"
    private const val KEY_STEREO_AMOUNT = "stereo_amount"
    private const val KEY_VOLUME_ENABLED = "volume_enabled"
    private const val KEY_HP_VIRT_ENABLED = "hp_virt_enabled"
    private const val KEY_SPK_VIRT_ENABLED = "spk_virt_enabled"

    private const val KEY_EQUALIZER = "equalizer"
    private const val KEY_PRESET_NAME = "preset_name"
    private const val KEY_PRESET_GAINS = "band_gains"

    private const val TYPE_DOLBY_EQ_PRESET = "dolby_eq_preset"

    fun exportProfileToJson(context: Context, profileId: Int): String {
        val dolbyController = DolbyController.getInstance(context)
        val profileRepo = ProfileRepository.getInstance(context)
        val eqRepo = EqualizerRepository.getInstance(context)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val profile = profileRepo.getProfile(profileId)
        val profileName = profile?.name ?: context.getString(R.string.dolby_profile_dynamic)

        val isCurrent = (profileId == dolbyController.profile)

        val ieqPreset = if (isCurrent) dolbyController.ieqPreset
                        else sharedPreferences.getInt("${DolbyConstants.PREF_IEQ}_$profileId", 0)
        val dialogueAmount = if (isCurrent) dolbyController.dialogueEnhancerAmount
                             else sharedPreferences.getInt("${DolbyConstants.PREF_DIALOGUE}_$profileId", 0)
        val bassEnabled = if (isCurrent) dolbyController.bassEnhancerEnabled
                          else sharedPreferences.getBoolean("${DolbyConstants.PREF_BASS}_$profileId", false)
        val stereoAmount = if (isCurrent) dolbyController.stereoWideningAmount
                           else sharedPreferences.getInt("${DolbyConstants.PREF_STEREO}_$profileId", 0)
        val volumeEnabled = if (isCurrent) dolbyController.volumeLevelerEnabled
                            else sharedPreferences.getBoolean("${DolbyConstants.PREF_VOLUME}_$profileId", false)
        val hpVirtEnabled = if (isCurrent) dolbyController.headphoneVirtEnabled
                            else sharedPreferences.getBoolean("${DolbyConstants.PREF_HP_VIRTUALIZER}_$profileId", false)
        val spkVirtEnabled = if (isCurrent) dolbyController.speakerVirtEnabled
                             else sharedPreferences.getBoolean("${DolbyConstants.PREF_SPK_VIRTUALIZER}_$profileId", false)

        val root = JSONObject().apply {
            put(KEY_VERSION, CURRENT_VERSION)
            put(KEY_TYPE, TYPE_DOLBY_PROFILE)
            put(KEY_PROFILE_NAME, profileName)
            put(KEY_BASE_PROFILE_ID, profile?.baseProfileId ?: 0)

            val settingsObj = JSONObject().apply {
                put(KEY_IEQ_PRESET, ieqPreset)
                put(KEY_DIALOGUE_AMOUNT, dialogueAmount)
                put(KEY_BASS_ENABLED, bassEnabled)
                put(KEY_STEREO_AMOUNT, stereoAmount)
                put(KEY_VOLUME_ENABLED, volumeEnabled)
                put(KEY_HP_VIRT_ENABLED, hpVirtEnabled)
                put(KEY_SPK_VIRT_ENABLED, spkVirtEnabled)
            }
            put(KEY_SETTINGS, settingsObj)

            val presetId = sharedPreferences.getInt("${DolbyConstants.PREF_PRESET}_$profileId", 0)
            val preset = eqRepo.getPreset(presetId)
            val presetName = preset?.name ?: context.getString(R.string.dolby_preset_default)
            val gains = eqRepo.getPresetGains(presetId)

            val eqObj = JSONObject().apply {
                put(KEY_PRESET_NAME, presetName)
                val gainsArray = JSONArray()
                gains.forEach { gainsArray.put(it.gain) }
                put(KEY_PRESET_GAINS, gainsArray)
            }
            put(KEY_EQUALIZER, eqObj)
        }

        return root.toString(2)
    }

    fun exportCurrentProfileToJson(context: Context): String {
        val dolbyController = DolbyController.getInstance(context)
        return exportProfileToJson(context, dolbyController.profile)
    }

    fun importProfileFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val dolbyController = DolbyController.getInstance(context)
            val profileRepo = ProfileRepository.getInstance(context)
            val eqRepo = EqualizerRepository.getInstance(context)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            val profileName = root.optString(KEY_PROFILE_NAME, "Imported Profile")
            val baseProfileId = root.optInt(KEY_BASE_PROFILE_ID, 0)

            // Create a custom user profile
            val newProfile = profileRepo.createCustomProfile(profileName, baseProfileId)
            val newProfileId = newProfile.id

            val editor = sharedPreferences.edit()

            // Restore settings
            if (root.has(KEY_SETTINGS)) {
                val settingsObj = root.getJSONObject(KEY_SETTINGS)
                if (settingsObj.has(KEY_IEQ_PRESET)) {
                    editor.putInt("${DolbyConstants.PREF_IEQ}_$newProfileId", settingsObj.getInt(KEY_IEQ_PRESET))
                }
                if (settingsObj.has(KEY_DIALOGUE_AMOUNT)) {
                    editor.putInt("${DolbyConstants.PREF_DIALOGUE}_$newProfileId", settingsObj.getInt(KEY_DIALOGUE_AMOUNT))
                }
                if (settingsObj.has(KEY_BASS_ENABLED)) {
                    editor.putBoolean("${DolbyConstants.PREF_BASS}_$newProfileId", settingsObj.getBoolean(KEY_BASS_ENABLED))
                }
                if (settingsObj.has(KEY_STEREO_AMOUNT)) {
                    editor.putInt("${DolbyConstants.PREF_STEREO}_$newProfileId", settingsObj.getInt(KEY_STEREO_AMOUNT))
                }
                if (settingsObj.has(KEY_VOLUME_ENABLED)) {
                    editor.putBoolean("${DolbyConstants.PREF_VOLUME}_$newProfileId", settingsObj.getBoolean(KEY_VOLUME_ENABLED))
                }
                if (settingsObj.has(KEY_HP_VIRT_ENABLED)) {
                    editor.putBoolean("${DolbyConstants.PREF_HP_VIRTUALIZER}_$newProfileId", settingsObj.getBoolean(KEY_HP_VIRT_ENABLED))
                }
                if (settingsObj.has(KEY_SPK_VIRT_ENABLED)) {
                    editor.putBoolean("${DolbyConstants.PREF_SPK_VIRTUALIZER}_$newProfileId", settingsObj.getBoolean(KEY_SPK_VIRT_ENABLED))
                }
            }

            // Restore Equalizer preset
            if (root.has(KEY_EQUALIZER)) {
                val eqObj = root.getJSONObject(KEY_EQUALIZER)
                val presetName = eqObj.optString(KEY_PRESET_NAME, "Custom")
                val gainsArray = eqObj.optJSONArray(KEY_PRESET_GAINS)

                val newPreset = eqRepo.addPreset(presetName)
                if (gainsArray != null) {
                    for (band in 0 until minOf(10, gainsArray.length())) {
                        val gain = gainsArray.getInt(band)
                        eqRepo.setBandGain(newPreset.id, band, gain)
                    }
                }
                editor.putInt("${DolbyConstants.PREF_PRESET}_$newProfileId", newPreset.id)
            }

            editor.apply()

            // Activate the newly imported profile
            dolbyController.profile = newProfileId

            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import profile: ${e.message}")
            false
        }
    }

    fun exportProfileToUri(context: Context, uri: Uri): Boolean {
        val dolbyController = DolbyController.getInstance(context)
        return exportProfileToUri(context, dolbyController.profile, uri)
    }

    fun exportProfileToUri(context: Context, profileId: Int, uri: Uri): Boolean {
        return try {
            val json = exportProfileToJson(context, profileId)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to export profile to URI: ${e.message}")
            false
        }
    }

    fun importProfileFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return false
            importProfileFromJson(context, json)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import profile from URI: ${e.message}")
            false
        }
    }

    fun exportPresetToJson(context: Context, presetId: Int): String {
        val eqRepo = EqualizerRepository.getInstance(context)
        val preset = eqRepo.getPreset(presetId)
        val presetName = preset?.name ?: context.getString(R.string.dolby_preset_default)
        val gains = eqRepo.getPresetGains(presetId)

        val root = JSONObject().apply {
            put(KEY_VERSION, CURRENT_VERSION)
            put(KEY_TYPE, TYPE_DOLBY_EQ_PRESET)
            put(KEY_PRESET_NAME, presetName)
            val gainsArray = JSONArray()
            gains.forEach { gainsArray.put(it.gain) }
            put(KEY_PRESET_GAINS, gainsArray)
        }
        return root.toString(2)
    }

    fun exportPresetToUri(context: Context, presetId: Int, uri: Uri): Boolean {
        return try {
            val json = exportPresetToJson(context, presetId)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to export preset to URI: ${e.message}")
            false
        }
    }

    fun importPresetFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val eqRepo = EqualizerRepository.getInstance(context)
            val presetName = root.optString(KEY_PRESET_NAME, "Imported Preset")
            val gainsArray = root.optJSONArray(KEY_PRESET_GAINS) ?: return false

            val newPreset = eqRepo.addPreset(presetName)
            for (band in 0 until minOf(10, gainsArray.length())) {
                val gain = gainsArray.getInt(band)
                eqRepo.setBandGain(newPreset.id, band, gain)
            }
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import preset from JSON: ${e.message}")
            false
        }
    }

    fun importPresetFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return false
            importPresetFromJson(context, json)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to import preset from URI: ${e.message}")
            false
        }
    }
}
