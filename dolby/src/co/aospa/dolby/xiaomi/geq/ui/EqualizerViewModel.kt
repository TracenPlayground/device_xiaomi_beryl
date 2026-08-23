/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.geq.data.BandGain
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import co.aospa.dolby.xiaomi.geq.data.Preset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val dolbyController = DolbyController.getInstance(application)
    private val repository = EqualizerRepository.getInstance(application)
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

    private val _presets = MutableStateFlow<List<Preset>>(emptyList())
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow<Preset?>(null)
    val currentPreset: StateFlow<Preset?> = _currentPreset.asStateFlow()

    private val _bandGains = MutableStateFlow<List<BandGain>>(emptyList())
    val bandGains: StateFlow<List<BandGain>> = _bandGains.asStateFlow()

    init {
        loadPresets()
    }

    fun loadPresets() {
        val allPresets = repository.getAllPresets()
        _presets.value = allPresets

        val currentPresetId = sharedPreferences.getInt(
            "${DolbyConstants.PREF_PRESET}_${dolbyController.profile}",
            0
        )
        val selectedPreset = allPresets.firstOrNull { it.id == currentPresetId } ?: allPresets.firstOrNull()
        _currentPreset.value = selectedPreset

        if (selectedPreset != null) {
            val gains = repository.getPresetGains(selectedPreset.id)
            _bandGains.value = gains
            applyGainsToDolby(gains)
        }
    }

    fun selectPreset(preset: Preset) {
        sharedPreferences.edit().putInt(
            "${DolbyConstants.PREF_PRESET}_${dolbyController.profile}",
            preset.id
        ).apply()

        _currentPreset.value = preset
        val gains = repository.getPresetGains(preset.id)
        _bandGains.value = gains
        applyGainsToDolby(gains)
    }

    fun setBandGain(band: Int, gain: Int) {
        val preset = _currentPreset.value ?: return
        repository.setBandGain(preset.id, band, gain)

        val updatedGains = _bandGains.value.map {
            if (it.band == band) it.copy(gain = gain) else it
        }
        _bandGains.value = updatedGains
        dolbyController.setGeqBandGain(band, gain)
    }

    fun resetGains() {
        val preset = _currentPreset.value ?: return
        repository.resetPresetGains(preset.id)
        val gains = repository.getPresetGains(preset.id)
        _bandGains.value = gains
        applyGainsToDolby(gains)
    }

    fun addPreset(name: String) {
        val newPreset = repository.addPreset(name)
        loadPresets()
        selectPreset(newPreset)
    }

    fun renamePreset(presetId: Int, newName: String) {
        repository.renamePreset(presetId, newName)
        loadPresets()
    }

    fun deletePreset(presetId: Int) {
        val isCurrentlySelected = _currentPreset.value?.id == presetId
        repository.deletePreset(presetId)
        loadPresets()
        if (isCurrentlySelected) {
            _presets.value.firstOrNull()?.let { selectPreset(it) }
        }
    }

    private fun applyGainsToDolby(gains: List<BandGain>) {
        gains.forEach {
            dolbyController.setGeqBandGain(it.band, it.gain)
        }
    }
}
