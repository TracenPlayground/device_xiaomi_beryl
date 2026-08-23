/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyConstants.DsParam
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.preference.DolbyPreferenceStore

class DolbyController private constructor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferenceStore = DolbyPreferenceStore(context)
    private var dolbyAudioEffect: DolbyAudioEffect? = null
    private val handler = Handler(Looper.getMainLooper())

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            updateVirtualizerForRecording(configs.isNotEmpty())
        }
    }

    init {
        initEffect()
        audioManager.registerAudioRecordingCallback(recordingCallback, handler)
    }

    private fun initEffect() {
        try {
            dolbyAudioEffect = DolbyAudioEffect(0, 0)
        } catch (e: Exception) {
            dlog(TAG, "Failed to initialize DolbyAudioEffect: ${e.message}")
        }
    }

    fun onBootCompleted() {
        dlog(TAG, "onBootCompleted")
        restoreSettings()
    }

    private fun restoreSettings() {
        dsOn = preferenceStore.dsOn
        profile = preferenceStore.profile
        ieqPreset = preferenceStore.ieqPreset
        dialogueEnhancerAmount = preferenceStore.dialogueEnhancerAmount
        bassEnhancerEnabled = preferenceStore.bassEnhancerEnabled
        stereoWideningAmount = preferenceStore.stereoWideningAmount
        volumeLevelerEnabled = preferenceStore.volumeLevelerEnabled
        headphoneVirtEnabled = preferenceStore.headphoneVirtEnabled
        speakerVirtEnabled = preferenceStore.speakerVirtEnabled
    }

    private fun updateVirtualizerForRecording(isRecording: Boolean) {
        if (isRecording) {
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_VIRTUALIZER_ENABLE, false)
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_SPEAKER_VIRTUALIZER_ENABLE, false)
        } else {
            dolbyAudioEffect?.setDapParameterBool(
                DsParam.DAP_PARAM_VIRTUALIZER_ENABLE,
                preferenceStore.headphoneVirtEnabled
            )
            dolbyAudioEffect?.setDapParameterBool(
                DsParam.DAP_PARAM_SPEAKER_VIRTUALIZER_ENABLE,
                preferenceStore.speakerVirtEnabled
            )
        }
    }

    var dsOn: Boolean
        get() = dolbyAudioEffect?.getDsOn() ?: preferenceStore.dsOn
        set(value) {
            preferenceStore.dsOn = value
            dolbyAudioEffect?.setDsOn(value)
        }

    var profile: Int
        get() = dolbyAudioEffect?.getProfile() ?: preferenceStore.profile
        set(value) {
            preferenceStore.profile = value
            dolbyAudioEffect?.setProfile(value)
        }

    fun getProfileName(): String? {
        val entries = context.resources.getStringArray(R.array.dolby_profile_entries)
        val values = context.resources.getStringArray(R.array.dolby_profile_values)
        val index = values.indexOf(profile.toString())
        return if (index >= 0) entries.getOrNull(index) else null
    }

    var ieqPreset: Int
        get() = dolbyAudioEffect?.getDapParameter(DsParam.DAP_PARAM_IEQ_PRESET) ?: preferenceStore.ieqPreset
        set(value) {
            preferenceStore.ieqPreset = value
            dolbyAudioEffect?.setDapParameter(DsParam.DAP_PARAM_IEQ_PRESET, value)
        }

    var dialogueEnhancerAmount: Int
        get() = dolbyAudioEffect?.getDapParameter(DsParam.DAP_PARAM_DE_AMOUNT) ?: preferenceStore.dialogueEnhancerAmount
        set(value) {
            preferenceStore.dialogueEnhancerAmount = value
            dolbyAudioEffect?.setDapParameter(DsParam.DAP_PARAM_DE_AMOUNT, value)
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_DE_ENABLE, value > 0)
        }

    var bassEnhancerEnabled: Boolean
        get() = dolbyAudioEffect?.getDapParameterBool(DsParam.DAP_PARAM_BASS_ENABLE) ?: preferenceStore.bassEnhancerEnabled
        set(value) {
            preferenceStore.bassEnhancerEnabled = value
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_BASS_ENABLE, value)
        }

    var stereoWideningAmount: Int
        get() = dolbyAudioEffect?.getDapParameter(DsParam.DAP_PARAM_SURROUND_BOOST) ?: preferenceStore.stereoWideningAmount
        set(value) {
            preferenceStore.stereoWideningAmount = value
            dolbyAudioEffect?.setDapParameter(DsParam.DAP_PARAM_SURROUND_BOOST, value)
        }

    var volumeLevelerEnabled: Boolean
        get() = dolbyAudioEffect?.getDapParameterBool(DsParam.DAP_PARAM_VOL_LEVELER_ENABLE) ?: preferenceStore.volumeLevelerEnabled
        set(value) {
            preferenceStore.volumeLevelerEnabled = value
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_VOL_LEVELER_ENABLE, value)
        }

    var headphoneVirtEnabled: Boolean
        get() = dolbyAudioEffect?.getDapParameterBool(DsParam.DAP_PARAM_HP_VIRTUALIZER_ENABLE) ?: preferenceStore.headphoneVirtEnabled
        set(value) {
            preferenceStore.headphoneVirtEnabled = value
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_HP_VIRTUALIZER_ENABLE, value)
        }

    var speakerVirtEnabled: Boolean
        get() = dolbyAudioEffect?.getDapParameterBool(DsParam.DAP_PARAM_SPEAKER_VIRTUALIZER_ENABLE) ?: preferenceStore.speakerVirtEnabled
        set(value) {
            preferenceStore.speakerVirtEnabled = value
            dolbyAudioEffect?.setDapParameterBool(DsParam.DAP_PARAM_SPEAKER_VIRTUALIZER_ENABLE, value)
        }

    fun setGeqBandGain(band: Int, gain: Int) {
        dolbyAudioEffect?.setGeqBandGain(band, gain)
    }

    fun getGeqBandGain(band: Int): Int {
        return dolbyAudioEffect?.getGeqBandGain(band) ?: 0
    }

    fun resetCurrentProfile() {
        preferenceStore.resetProfile(profile)
        restoreSettings()
    }

    fun isOnSpeaker(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } &&
                !devices.any {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                }
    }

    companion object {
        private const val TAG = "DolbyController"

        @Volatile
        private var instance: DolbyController? = null

        fun getInstance(context: Context): DolbyController {
            return instance ?: synchronized(this) {
                instance ?: DolbyController(context.applicationContext).also { instance = it }
            }
        }
    }
}
