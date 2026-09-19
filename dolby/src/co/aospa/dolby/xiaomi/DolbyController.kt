/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.elog
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.wlog
import co.aospa.dolby.xiaomi.DolbyConstants.DsParam
import co.aospa.dolby.xiaomi.data.ProfileRepository
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import co.aospa.dolby.xiaomi.preference.DolbyPreferenceStore

class DolbyController private constructor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferenceStore = DolbyPreferenceStore(context)
    private val profileRepository by lazy { ProfileRepository.getInstance(context) }
    private var dolbyAudioEffect: DolbyAudioEffect? = null
    private val handler = Handler(Looper.getMainLooper())

    private val current10BandGains = IntArray(10)
    private val current20BandGains = IntArray(20)

    val currentBaseProfileId: Int
        get() = profileRepository.getProfile(preferenceStore.profile)?.baseProfileId ?: preferenceStore.profile

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            updateVirtualizerForRecording(configs.isNotEmpty())
        }
    }

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            val isPlaying = configs.any { it.playerState == AudioPlaybackConfiguration.PLAYER_STATE_STARTED }
            if (isPlaying && (dolbyAudioEffect == null || !dolbyAudioEffect!!.hasControl())) {
                dlog(TAG, "Playback active and effect needs control, restoring settings")
                restoreSettings()
            }
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            checkEffect()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            checkEffect()
        }
    }

    init {
        initEffect()
        audioManager.registerAudioRecordingCallback(recordingCallback, handler)
        audioManager.registerAudioPlaybackCallback(playbackCallback, handler)
        audioManager.registerAudioDeviceCallback(deviceCallback, handler)
    }

    private fun initEffect() {
        try {
            dolbyAudioEffect?.release()
        } catch (e: Exception) {
            // Ignore release error
        }
        try {
            dolbyAudioEffect = DolbyAudioEffect(EFFECT_PRIORITY, AUDIO_SESSION_GLOBAL)
            dlog(TAG, "DolbyAudioEffect initialized successfully")
        } catch (e: Exception) {
            elog(TAG, "Failed to initialize DolbyAudioEffect: ${e.message}", e)
            dolbyAudioEffect = null
        }
    }

    private fun checkEffect(): Boolean {
        if (dolbyAudioEffect == null || !dolbyAudioEffect!!.hasControl()) {
            dlog(TAG, "DolbyAudioEffect null or lost control, reinitializing...")
            initEffect()
        }
        return dolbyAudioEffect != null
    }

    private fun <T> checkEffectAndRun(block: () -> T): T? {
        if (!checkEffect()) {
            wlog(TAG, "checkEffectAndRun: DolbyAudioEffect unavailable")
            return null
        }
        return try {
            block()
        } catch (e: Exception) {
            elog(TAG, "Error executing effect operation: ${e.message}", e)
            null
        }
    }

    fun onBootCompleted() {
        dlog(TAG, "onBootCompleted")
        restoreSettings()
    }

    fun restoreSettings() {
        if (!checkEffect()) return
        val currentProfile = preferenceStore.profile
        val baseId = currentBaseProfileId
        dolbyAudioEffect?.profile = baseId
        dolbyAudioEffect?.dsOn = preferenceStore.dsOn

        val deAmount = preferenceStore.dialogueEnhancerAmount
        dolbyAudioEffect?.setDapParameter(DsParam.IEQ_PRESET, preferenceStore.ieqPreset, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, deAmount, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, deAmount > 0, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, preferenceStore.bassEnhancerEnabled, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, preferenceStore.stereoWideningAmount, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, preferenceStore.volumeLevelerEnabled, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, preferenceStore.headphoneVirtEnabled, baseId)
        dolbyAudioEffect?.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, preferenceStore.speakerVirtEnabled, baseId)

        restoreGeqGains()
    }

    private fun updateVirtualizerForRecording(isRecording: Boolean) {
        checkEffectAndRun {
            if (isRecording) {
                dolbyAudioEffect?.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, false, currentBaseProfileId)
                dolbyAudioEffect?.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, false, currentBaseProfileId)
            } else {
                dolbyAudioEffect?.setDapParameter(
                    DsParam.HEADPHONE_VIRTUALIZER,
                    preferenceStore.headphoneVirtEnabled,
                    currentBaseProfileId
                )
                dolbyAudioEffect?.setDapParameter(
                    DsParam.SPEAKER_VIRTUALIZER,
                    preferenceStore.speakerVirtEnabled,
                    currentBaseProfileId
                )
            }
        }
    }

    var dsOn: Boolean
        get() = dolbyAudioEffect?.dsOn ?: preferenceStore.dsOn
        set(value) {
            preferenceStore.dsOn = value
            checkEffectAndRun {
                dolbyAudioEffect?.dsOn = value
            }
        }

    var profile: Int
        get() = preferenceStore.profile
        set(value) {
            preferenceStore.profile = value
            val baseId = currentBaseProfileId
            checkEffectAndRun {
                dolbyAudioEffect?.profile = baseId
            }
            restoreSettings()
        }

    var preset: Int
        get() = preferenceStore.preset
        set(value) {
            preferenceStore.preset = value
            restoreGeqGains()
        }

    fun getProfileName(): String? {
        return profileRepository.getProfileName(profile)
    }

    var ieqPreset: Int
        get() = checkEffectAndRun { dolbyAudioEffect?.getDapParameterInt(DsParam.IEQ_PRESET, currentBaseProfileId) }
            ?: preferenceStore.ieqPreset
        set(value) {
            preferenceStore.ieqPreset = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.IEQ_PRESET, value, currentBaseProfileId)
            }
        }

    var dialogueEnhancerAmount: Int
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterInt(DsParam.DIALOGUE_ENHANCER_AMOUNT, currentBaseProfileId)
        } ?: preferenceStore.dialogueEnhancerAmount
        set(value) {
            preferenceStore.dialogueEnhancerAmount = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, value, currentBaseProfileId)
                dolbyAudioEffect?.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, value > 0, currentBaseProfileId)
            }
        }

    var bassEnhancerEnabled: Boolean
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterBool(DsParam.BASS_ENHANCER_ENABLE, currentBaseProfileId)
        } ?: preferenceStore.bassEnhancerEnabled
        set(value) {
            preferenceStore.bassEnhancerEnabled = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, value, currentBaseProfileId)
            }
        }

    var stereoWideningAmount: Int
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterInt(DsParam.STEREO_WIDENING_AMOUNT, currentBaseProfileId)
        } ?: preferenceStore.stereoWideningAmount
        set(value) {
            preferenceStore.stereoWideningAmount = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, value, currentBaseProfileId)
            }
        }

    var volumeLevelerEnabled: Boolean
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterBool(DsParam.VOLUME_LEVELER_ENABLE, currentBaseProfileId)
        } ?: preferenceStore.volumeLevelerEnabled
        set(value) {
            preferenceStore.volumeLevelerEnabled = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, value, currentBaseProfileId)
            }
        }

    var headphoneVirtEnabled: Boolean
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterBool(DsParam.HEADPHONE_VIRTUALIZER, currentBaseProfileId)
        } ?: preferenceStore.headphoneVirtEnabled
        set(value) {
            preferenceStore.headphoneVirtEnabled = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, value, currentBaseProfileId)
            }
        }

    var speakerVirtEnabled: Boolean
        get() = checkEffectAndRun {
            dolbyAudioEffect?.getDapParameterBool(DsParam.SPEAKER_VIRTUALIZER, currentBaseProfileId)
        } ?: preferenceStore.speakerVirtEnabled
        set(value) {
            preferenceStore.speakerVirtEnabled = value
            checkEffectAndRun {
                dolbyAudioEffect?.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, value, currentBaseProfileId)
            }
        }

    fun setGeqBandGain(band: Int, gain: Int) {
        if (band in 0..9) {
            current10BandGains[band] = gain
            syncGeqToDap()
        }
    }

    fun getGeqBandGain(band: Int): Int {
        return if (band in 0..9) current10BandGains[band] else 0
    }

    fun restoreGeqGains() {
        try {
            val equalizerRepo = EqualizerRepository.getInstance(context)
            val gains = equalizerRepo.getPresetGains(preferenceStore.preset)
            for (i in 0 until 10) {
                current10BandGains[i] = gains.getOrNull(i)?.gain ?: 0
            }
            syncGeqToDap()
        } catch (e: Exception) {
            dlog(TAG, "Error restoring GEQ gains: ${e.message}")
        }
    }

    private fun syncGeqToDap() {
        for (i in 0 until 10) {
            val scaled = current10BandGains[i] * 10
            current20BandGains[2 * i] = scaled
            current20BandGains[2 * i + 1] = scaled
        }
        checkEffectAndRun {
            dolbyAudioEffect?.setDapParameter(DsParam.GEQ_BAND_GAINS, current20BandGains, currentBaseProfileId)
        }
    }

    fun resetCurrentProfile() {
        val prof = profile
        preferenceStore.resetProfile(prof)
        checkEffectAndRun {
            dolbyAudioEffect?.resetProfileSpecificSettings(currentBaseProfileId)
        }
        restoreSettings()
    }

    fun isOnSpeaker(): Boolean {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } &&
                !devices.any {
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_HEARING_AID ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
                }
    }

    companion object {
        private const val TAG = "DolbyController"
        private const val EFFECT_PRIORITY = 100
        private const val AUDIO_SESSION_GLOBAL = 0

        @Volatile
        private var instance: DolbyController? = null

        fun getInstance(context: Context): DolbyController {
            return instance ?: synchronized(this) {
                instance ?: DolbyController(context.applicationContext).also { instance = it }
            }
        }
    }
}
