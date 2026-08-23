/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.data

import android.content.Context
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.R

class EqualizerRepository private constructor(private val context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val presetNames = context.resources.getStringArray(R.array.dolby_preset_entries)
    private val defaultPresets = listOf(
        Preset(0, presetNames[0], (0..9).map { BandGain(it, 0) }, isReadOnly = true),
        Preset(1, presetNames[1], listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 2), BandGain(3, 0), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(2, presetNames[2], listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 1), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(3, presetNames[3], listOf(BandGain(0, -1), BandGain(1, 1), BandGain(2, 3), BandGain(3, 4), BandGain(4, 3), BandGain(5, 0), BandGain(6, -1), BandGain(7, -1), BandGain(8, -1), BandGain(9, -1)), isReadOnly = true),
        Preset(4, presetNames[4], listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 2), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 2), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true),
        Preset(5, presetNames[5], listOf(BandGain(0, 4), BandGain(1, 3), BandGain(2, 0), BandGain(3, 2), BandGain(4, -1), BandGain(5, -1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(6, presetNames[6], listOf(BandGain(0, 2), BandGain(1, 1), BandGain(2, 2), BandGain(3, -1), BandGain(4, 0), BandGain(5, 1), BandGain(6, 0), BandGain(7, 1), BandGain(8, 2), BandGain(9, 3)), isReadOnly = true),
        Preset(7, presetNames[7], listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 0), BandGain(3, 0), BandGain(4, -1), BandGain(5, 2), BandGain(6, 1), BandGain(7, 1), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true),
        Preset(8, presetNames[8], listOf(BandGain(0, 2), BandGain(1, 1), BandGain(2, 0), BandGain(3, 2), BandGain(4, 2), BandGain(5, 1), BandGain(6, 0), BandGain(7, 2), BandGain(8, 3), BandGain(9, 3)), isReadOnly = true),
        Preset(9, presetNames[9], listOf(BandGain(0, 3), BandGain(1, 5), BandGain(2, 2), BandGain(3, 0), BandGain(4, 1), BandGain(5, 2), BandGain(6, 3), BandGain(7, 2), BandGain(8, 1), BandGain(9, 0)), isReadOnly = true),
        Preset(10, presetNames[10], listOf(BandGain(0, 3), BandGain(1, 2), BandGain(2, 1), BandGain(3, 0), BandGain(4, -1), BandGain(5, -1), BandGain(6, 1), BandGain(7, 2), BandGain(8, 3), BandGain(9, 4)), isReadOnly = true)
    )

    fun getAllPresets(): List<Preset> {
        val presets = mutableListOf<Preset>()
        presets.addAll(defaultPresets)

        val customCount = sharedPreferences.getInt(KEY_CUSTOM_PRESET_COUNT, 0)
        for (i in 0 until customCount) {
            val id = CUSTOM_PRESET_BASE_ID + i
            val name = sharedPreferences.getString("${KEY_CUSTOM_PRESET_NAME}_$id", "Preset ${i + 1}") ?: "Preset ${i + 1}"
            val gains = getPresetGains(id)
            presets.add(Preset(id, name, gains, isReadOnly = false))
        }
        return presets
    }

    fun getPreset(presetId: Int): Preset? {
        return getAllPresets().firstOrNull { it.id == presetId }
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
        val customCount = sharedPreferences.getInt(KEY_CUSTOM_PRESET_COUNT, 0)
        val newId = CUSTOM_PRESET_BASE_ID + customCount

        sharedPreferences.edit()
            .putInt(KEY_CUSTOM_PRESET_COUNT, customCount + 1)
            .putString("${KEY_CUSTOM_PRESET_NAME}_$newId", name)
            .apply()

        val gains = (0..9).map { BandGain(it, 0) }
        return Preset(newId, name, gains, isReadOnly = false)
    }

    fun renamePreset(presetId: Int, newName: String) {
        if (presetId >= CUSTOM_PRESET_BASE_ID) {
            sharedPreferences.edit().putString("${KEY_CUSTOM_PRESET_NAME}_$presetId", newName).apply()
        }
    }

    fun deletePreset(presetId: Int) {
        if (presetId >= CUSTOM_PRESET_BASE_ID) {
            val editor = sharedPreferences.edit()
            editor.remove("${KEY_CUSTOM_PRESET_NAME}_$presetId")
            for (band in 0..9) {
                editor.remove("${KEY_PRESET_GAIN}_${presetId}_$band")
            }
            editor.apply()
        }
    }

    companion object {
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
