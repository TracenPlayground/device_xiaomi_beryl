/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.R

class EqualizerRepository private constructor(private val context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val presetNames = context.resources.getStringArray(R.array.dolby_preset_entries)
    private val defaultPresets = listOf(
        Preset(0, presetNames.getOrElse(0) { "Flat (off)" }, (0..9).map { BandGain(it, 0) }, isReadOnly = true),
        Preset(1, presetNames.getOrElse(1) { "Rock" }, listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 2), BandGain(3, 0), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(2, presetNames.getOrElse(2) { "Jazz" }, listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 1), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(3, presetNames.getOrElse(3) { "Pop" }, listOf(BandGain(0, -1), BandGain(1, 1), BandGain(2, 3), BandGain(3, 4), BandGain(4, 3), BandGain(5, 0), BandGain(6, -1), BandGain(7, -1), BandGain(8, -1), BandGain(9, -1)), isReadOnly = true),
        Preset(4, presetNames.getOrElse(4) { "Classical" }, listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 2), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 2), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true),
        Preset(5, presetNames.getOrElse(5) { "Hip Hop" }, listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 0), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(6, presetNames.getOrElse(6) { "Blues" }, listOf(BandGain(0, 2), BandGain(1, 1), BandGain(2, 2), BandGain(3, -1), BandGain(4, 0), BandGain(5, 1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(7, presetNames.getOrElse(7) { "Electronic" }, listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 0), BandGain(3, 0), BandGain(4, -1), BandGain(5, 2), BandGain(6, 1), BandGain(7, 1), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true),
        Preset(8, presetNames.getOrElse(8) { "Country" }, listOf(BandGain(0, 2), BandGain(1, 1), BandGain(2, 0), BandGain(3, 2), BandGain(4, 2), BandGain(5, 1), BandGain(6, 0), BandGain(7, 2), BandGain(8, 3), BandGain(9, 3)), isReadOnly = true),
        Preset(9, presetNames.getOrElse(9) { "Dance" }, listOf(BandGain(0, 3), BandGain(1, 5), BandGain(2, 2), BandGain(3, 0), BandGain(4, 1), BandGain(5, 2), BandGain(6, 3), BandGain(7, 2), BandGain(8, 1), BandGain(9, 0)), isReadOnly = true),
        Preset(10, presetNames.getOrElse(10) { "Metal" }, listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 1), BandGain(3, 0), BandGain(4, -1), BandGain(5, -1), BandGain(6, 1), BandGain(7, 2), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true)
    )

    private fun getCustomPresetIds(): List<Int> {
        val storedIds = sharedPreferences.getString(KEY_CUSTOM_PRESET_IDS, null)
        if (storedIds != null) {
            if (storedIds.isEmpty()) return emptyList()
            return storedIds.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        val legacyCount = sharedPreferences.getInt(KEY_CUSTOM_PRESET_COUNT, 0)
        if (legacyCount > 0) {
            val migratedIds = mutableListOf<Int>()
            val editor = sharedPreferences.edit()
            for (i in 0 until legacyCount) {
                val id = CUSTOM_PRESET_BASE_ID + i
                if (sharedPreferences.contains("${KEY_CUSTOM_PRESET_NAME}_$id")) {
                    migratedIds.add(id)
                } else {
                    for (band in 0..9) {
                        editor.remove("${KEY_PRESET_GAIN}_${id}_$band")
                    }
                }
            }
            editor.putString(KEY_CUSTOM_PRESET_IDS, migratedIds.joinToString(","))
            editor.remove(KEY_CUSTOM_PRESET_COUNT)
            editor.apply()
            return migratedIds
        }

        return emptyList()
    }

    private fun saveCustomPresetIds(ids: List<Int>, editor: SharedPreferences.Editor? = null) {
        val targetEditor = editor ?: sharedPreferences.edit()
        targetEditor.putString(KEY_CUSTOM_PRESET_IDS, ids.joinToString(","))
        if (editor == null) {
            targetEditor.apply()
        }
    }

    fun getAllPresets(): List<Preset> {
        val presets = mutableListOf<Preset>()
        presets.addAll(defaultPresets)

        val customIds = getCustomPresetIds()
        for (id in customIds) {
            val name = sharedPreferences.getString("${KEY_CUSTOM_PRESET_NAME}_$id", null) ?: continue
            val gains = getPresetGains(id)
            presets.add(Preset(id, name, gains, isReadOnly = false))
        }
        return presets
    }

    fun getPreset(presetId: Int): Preset? {
        if (presetId < CUSTOM_PRESET_BASE_ID) {
            return defaultPresets.firstOrNull { it.id == presetId }
        }
        val name = sharedPreferences.getString("${KEY_CUSTOM_PRESET_NAME}_$presetId", null) ?: return null
        val gains = getPresetGains(presetId)
        return Preset(presetId, name, gains, isReadOnly = false)
    }

    fun getPresetGains(presetId: Int): List<BandGain> {
        val defaultPreset = defaultPresets.firstOrNull { it.id == presetId }
        val gains = mutableListOf<BandGain>()

        for (band in 0..9) {
            val key = "${KEY_PRESET_GAIN}_${presetId}_$band"
            val defaultGain = defaultPreset?.bandGains?.getOrNull(band)?.gain ?: 0
            val gain = sharedPreferences.getInt(key, defaultGain)
            gains.add(BandGain(band, gain))
        }
        return gains
    }

    fun setBandGain(presetId: Int, band: Int, gain: Int) {
        sharedPreferences.edit().putInt("${KEY_PRESET_GAIN}_${presetId}_$band", gain).apply()
    }

    fun updatePresetGains(presetId: Int, gains: IntArray) {
        val editor = sharedPreferences.edit()
        gains.forEachIndexed { band, gain ->
            editor.putInt("${KEY_PRESET_GAIN}_${presetId}_$band", gain)
        }
        editor.apply()
    }

    fun resetPresetGains(presetId: Int) {
        val editor = sharedPreferences.edit()
        for (band in 0..9) {
            editor.remove("${KEY_PRESET_GAIN}_${presetId}_$band")
        }
        editor.apply()
    }

    fun addPreset(name: String): Preset {
        val customIds = getCustomPresetIds().toMutableList()
        val nextId = (customIds.maxOrNull() ?: (CUSTOM_PRESET_BASE_ID - 1)) + 1
        customIds.add(nextId)

        val editor = sharedPreferences.edit()
        saveCustomPresetIds(customIds, editor)
        editor.putString("${KEY_CUSTOM_PRESET_NAME}_$nextId", name)
        for (band in 0..9) {
            editor.putInt("${KEY_PRESET_GAIN}_${nextId}_$band", 0)
        }
        editor.apply()

        val gains = (0..9).map { BandGain(it, 0) }
        return Preset(nextId, name, gains, isReadOnly = false)
    }

    fun renamePreset(presetId: Int, newName: String) {
        if (presetId >= CUSTOM_PRESET_BASE_ID) {
            sharedPreferences.edit().putString("${KEY_CUSTOM_PRESET_NAME}_$presetId", newName).apply()
        }
    }

    fun deletePreset(presetId: Int) {
        if (presetId >= CUSTOM_PRESET_BASE_ID) {
            val customIds = getCustomPresetIds().toMutableList()
            customIds.remove(presetId)

            val editor = sharedPreferences.edit()
            saveCustomPresetIds(customIds, editor)
            editor.remove("${KEY_CUSTOM_PRESET_NAME}_$presetId")
            for (band in 0..9) {
                editor.remove("${KEY_PRESET_GAIN}_${presetId}_$band")
            }

            val profileValues = context.resources.getStringArray(R.array.dolby_profile_values)
            for (profileVal in profileValues) {
                val profileKey = "${DolbyConstants.PREF_PRESET}_$profileVal"
                if (sharedPreferences.getInt(profileKey, 0) == presetId) {
                    editor.putInt(profileKey, 0)
                }
            }

            editor.apply()
        }
    }

    companion object {
        private const val KEY_CUSTOM_PRESET_IDS = "custom_preset_ids"
        private const val KEY_CUSTOM_PRESET_COUNT = "custom_preset_count"
        private const val KEY_CUSTOM_PRESET_NAME = "custom_preset_name"
        private const val KEY_PRESET_GAIN = "preset_gain"
        const val CUSTOM_PRESET_BASE_ID = 100

        @Volatile
        private var instance: EqualizerRepository? = null

        fun getInstance(context: Context): EqualizerRepository {
            return instance ?: synchronized(this) {
                instance ?: EqualizerRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
