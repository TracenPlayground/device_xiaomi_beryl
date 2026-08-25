/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.ui

import android.app.Application
import android.app.StatusBarManager
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import co.aospa.dolby.xiaomi.preference.DolbyPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DolbyViewModel(application: Application) : AndroidViewModel(application) {

    private val dolbyController = DolbyController.getInstance(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val preferenceStore = DolbyPreferenceStore(application)

    private val _dsOn = MutableStateFlow(dolbyController.dsOn)
    val dsOn: StateFlow<Boolean> = _dsOn.asStateFlow()

    private val _profile = MutableStateFlow(dolbyController.profile)
    val profile: StateFlow<Int> = _profile.asStateFlow()

    private val _presetName = MutableStateFlow(getPresetDisplayName())
    val presetName: StateFlow<String> = _presetName.asStateFlow()

    private val _ieqPreset = MutableStateFlow(dolbyController.ieqPreset)
    val ieqPreset: StateFlow<Int> = _ieqPreset.asStateFlow()

    private val _speakerVirtEnabled = MutableStateFlow(dolbyController.speakerVirtEnabled)
    val speakerVirtEnabled: StateFlow<Boolean> = _speakerVirtEnabled.asStateFlow()

    private val _headphoneVirtEnabled = MutableStateFlow(dolbyController.headphoneVirtEnabled)
    val headphoneVirtEnabled: StateFlow<Boolean> = _headphoneVirtEnabled.asStateFlow()

    private val _stereoWideningAmount = MutableStateFlow(dolbyController.stereoWideningAmount)
    val stereoWideningAmount: StateFlow<Int> = _stereoWideningAmount.asStateFlow()

    private val _dialogueEnhancerAmount = MutableStateFlow(dolbyController.dialogueEnhancerAmount)
    val dialogueEnhancerAmount: StateFlow<Int> = _dialogueEnhancerAmount.asStateFlow()

    private val _bassEnhancerEnabled = MutableStateFlow(dolbyController.bassEnhancerEnabled)
    val bassEnhancerEnabled: StateFlow<Boolean> = _bassEnhancerEnabled.asStateFlow()

    private val _volumeLevelerEnabled = MutableStateFlow(dolbyController.volumeLevelerEnabled)
    val volumeLevelerEnabled: StateFlow<Boolean> = _volumeLevelerEnabled.asStateFlow()

    private val _isOnSpeaker = MutableStateFlow(dolbyController.isOnSpeaker())
    val isOnSpeaker: StateFlow<Boolean> = _isOnSpeaker.asStateFlow()

    private val _audioOutputDeviceTitle = MutableStateFlow(getConnectedAudioDeviceTitle())
    val audioOutputDeviceTitle: StateFlow<String> = _audioOutputDeviceTitle.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioState()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    private fun updateAudioState() {
        viewModelScope.launch {
            _isOnSpeaker.value = dolbyController.isOnSpeaker()
            _audioOutputDeviceTitle.value = getConnectedAudioDeviceTitle()
        }
    }

    fun setDsOn(enabled: Boolean) {
        dolbyController.dsOn = enabled
        _dsOn.value = enabled
    }

    fun setProfile(profileId: Int) {
        dolbyController.profile = profileId
        _profile.value = profileId
        refreshAllSettings()
    }

    fun setIeqPreset(preset: Int) {
        dolbyController.ieqPreset = preset
        _ieqPreset.value = preset
    }

    fun setSpeakerVirtEnabled(enabled: Boolean) {
        dolbyController.speakerVirtEnabled = enabled
        _speakerVirtEnabled.value = enabled
    }

    fun setHeadphoneVirtEnabled(enabled: Boolean) {
        dolbyController.headphoneVirtEnabled = enabled
        _headphoneVirtEnabled.value = enabled
    }

    fun setStereoWideningAmount(amount: Int) {
        dolbyController.stereoWideningAmount = amount
        _stereoWideningAmount.value = amount
    }

    fun setDialogueEnhancerAmount(amount: Int) {
        dolbyController.dialogueEnhancerAmount = amount
        _dialogueEnhancerAmount.value = amount
    }

    fun setBassEnhancerEnabled(enabled: Boolean) {
        dolbyController.bassEnhancerEnabled = enabled
        _bassEnhancerEnabled.value = enabled
    }

    fun setVolumeLevelerEnabled(enabled: Boolean) {
        dolbyController.volumeLevelerEnabled = enabled
        _volumeLevelerEnabled.value = enabled
    }

    fun resetCurrentProfile() {
        dolbyController.resetCurrentProfile()
        refreshAllSettings()
    }

    private fun refreshAllSettings() {
        _dsOn.value = dolbyController.dsOn
        _profile.value = dolbyController.profile
        _ieqPreset.value = dolbyController.ieqPreset
        _dialogueEnhancerAmount.value = dolbyController.dialogueEnhancerAmount
        _bassEnhancerEnabled.value = dolbyController.bassEnhancerEnabled
        _stereoWideningAmount.value = dolbyController.stereoWideningAmount
        _volumeLevelerEnabled.value = dolbyController.volumeLevelerEnabled
        _headphoneVirtEnabled.value = dolbyController.headphoneVirtEnabled
        _speakerVirtEnabled.value = dolbyController.speakerVirtEnabled
        _presetName.value = getPresetDisplayName()
        _isOnSpeaker.value = dolbyController.isOnSpeaker()
        _audioOutputDeviceTitle.value = getConnectedAudioDeviceTitle()
    }

    private fun getPresetDisplayName(): String {
        val app = getApplication<Application>()
        val presetId = sharedPreferences.getInt(
            "${DolbyConstants.PREF_PRESET}_${dolbyController.profile}",
            0
        )
        val preset = EqualizerRepository.getInstance(app).getPreset(presetId)
        return preset?.name ?: app.getString(co.aospa.dolby.xiaomi.R.string.dolby_preset_default)
    }

    fun exportSettings(uri: Uri): Boolean {
        val app = getApplication<Application>()
        return co.aospa.dolby.xiaomi.data.DolbyConfigSerializer.exportToUri(app, uri)
    }

    fun importSettings(uri: Uri): Boolean {
        val app = getApplication<Application>()
        val success = co.aospa.dolby.xiaomi.data.DolbyConfigSerializer.importFromUri(app, uri)
        if (success) {
            refreshAllSettings()
        }
        return success
    }

    fun requestAddQsTile(context: Context) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return
        val componentName = android.content.ComponentName(context, co.aospa.dolby.xiaomi.DolbyTileService::class.java)
        val icon = android.graphics.drawable.Icon.createWithResource(context, co.aospa.dolby.xiaomi.R.drawable.ic_dolby_qs)
        statusBarManager.requestAddTileService(
            componentName,
            context.getString(co.aospa.dolby.xiaomi.R.string.dolby_title),
            icon,
            { it.run() },
            { resultCode ->
                co.aospa.dolby.xiaomi.DolbyConstants.dlog(TAG, "requestAddTileService result: $resultCode")
            }
        )
    }

    fun checkAndPromptAddQsTile(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val hasPrompted = prefs.getBoolean(PREF_KEY_QS_PROMPTED, false)
        if (!hasPrompted) {
            prefs.edit().putBoolean(PREF_KEY_QS_PROMPTED, true).apply()
            requestAddQsTile(context)
        }
    }

    private fun getConnectedAudioDeviceTitle(): String {
        val app = getApplication<Application>()
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val activeDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
            it.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
            it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } ?: return app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_speaker)

        val name = activeDevice.productName?.toString()?.trim() ?: ""

        return when (activeDevice.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_speaker)
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_headphones)
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                if (name.isNotEmpty()) {
                    app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_bluetooth, name)
                } else {
                    app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_bluetooth_generic)
                }
            }
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> {
                if (name.isNotEmpty()) {
                    app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_usb, name)
                } else {
                    app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_usb_generic)
                }
            }
            AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL ->
                app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_line)
            else -> if (name.isNotEmpty()) name else app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_speaker)
        }
    }

    companion object {
        private const val TAG = "DolbyViewModel"
        private const val PREF_KEY_QS_PROMPTED = "dolby_qs_tile_prompted"
    }
}
