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
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.data.DolbyConfigSerializer
import co.aospa.dolby.xiaomi.data.DolbyProfile
import co.aospa.dolby.xiaomi.data.ProfileRepository
import co.aospa.dolby.xiaomi.geq.data.EqualizerRepository
import co.aospa.dolby.xiaomi.preference.DolbyPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
data class ConnectedAudioDevice(
    val title: String,
    val iconRes: Int
)

class DolbyViewModel(application: Application) : AndroidViewModel(application) {

    private val dolbyController = DolbyController.getInstance(application)
    private val profileRepository = ProfileRepository.getInstance(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val preferenceStore = DolbyPreferenceStore(application)

    private val _dsOn = MutableStateFlow(dolbyController.dsOn)
    val dsOn: StateFlow<Boolean> = _dsOn.asStateFlow()

    private val _profile = MutableStateFlow(dolbyController.profile)
    val profile: StateFlow<Int> = _profile.asStateFlow()

    private val _profiles = MutableStateFlow(profileRepository.getAllProfiles())
    val profiles: StateFlow<List<DolbyProfile>> = _profiles.asStateFlow()

    private val _isCurrentProfileCustom = MutableStateFlow(profileRepository.isCustomProfile(dolbyController.profile))
    val isCurrentProfileCustom: StateFlow<Boolean> = _isCurrentProfileCustom.asStateFlow()

    private val _presetName = MutableStateFlow(getPresetDisplayName())
    val presetName: StateFlow<String> = _presetName.asStateFlow()

    private val _ieqPreset = MutableStateFlow(dolbyController.ieqPreset)
    val ieqPreset: StateFlow<Int> = _ieqPreset.asStateFlow()

    private val _dialogueEnhancerAmount = MutableStateFlow(dolbyController.dialogueEnhancerAmount)
    val dialogueEnhancerAmount: StateFlow<Int> = _dialogueEnhancerAmount.asStateFlow()

    private val _speakerVirtEnabled = MutableStateFlow(dolbyController.speakerVirtEnabled)
    val speakerVirtEnabled: StateFlow<Boolean> = _speakerVirtEnabled.asStateFlow()

    private val _headphoneVirtEnabled = MutableStateFlow(dolbyController.headphoneVirtEnabled)
    val headphoneVirtEnabled: StateFlow<Boolean> = _headphoneVirtEnabled.asStateFlow()

    private val _stereoWideningAmount = MutableStateFlow(dolbyController.stereoWideningAmount)
    val stereoWideningAmount: StateFlow<Int> = _stereoWideningAmount.asStateFlow()

    private val _bassEnhancerEnabled = MutableStateFlow(dolbyController.bassEnhancerEnabled)
    val bassEnhancerEnabled: StateFlow<Boolean> = _bassEnhancerEnabled.asStateFlow()

    private val _volumeLevelerEnabled = MutableStateFlow(dolbyController.volumeLevelerEnabled)
    val volumeLevelerEnabled: StateFlow<Boolean> = _volumeLevelerEnabled.asStateFlow()

    private val _isOnSpeaker = MutableStateFlow(dolbyController.isOnSpeaker())
    val isOnSpeaker: StateFlow<Boolean> = _isOnSpeaker.asStateFlow()

    private val _connectedAudioDevice = MutableStateFlow(getConnectedAudioDevice())
    val connectedAudioDevice: StateFlow<ConnectedAudioDevice> = _connectedAudioDevice.asStateFlow()

    private val _audioOutputDeviceTitle = MutableStateFlow(_connectedAudioDevice.value.title)
    val audioOutputDeviceTitle: StateFlow<String> = _audioOutputDeviceTitle.asStateFlow()

    private val _audioOutputSubtitle = MutableStateFlow(getAudioOutputSubtitle(_connectedAudioDevice.value))
    val audioOutputSubtitle: StateFlow<String> = _audioOutputSubtitle.asStateFlow()

    private val _isQsTileAdded = MutableStateFlow(checkIsQsTileAdded())
    val isQsTileAdded: StateFlow<Boolean> = _isQsTileAdded.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateAudioState()
        }
    }

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        refreshAllSettings()
    }

    init {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        sharedPreferences.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    fun onResume() {
        refreshAllSettings()
        updateQsTileStatus()
    }

    private fun updateAudioState() {
        viewModelScope.launch {
            _isOnSpeaker.value = dolbyController.isOnSpeaker()
            val device = getConnectedAudioDevice()
            _connectedAudioDevice.value = device
            _audioOutputDeviceTitle.value = device.title
            _audioOutputSubtitle.value = getAudioOutputSubtitle(device)
        }
    }

    fun setDsOn(enabled: Boolean) {
        dolbyController.dsOn = enabled
        _dsOn.value = enabled
        _audioOutputSubtitle.value = getAudioOutputSubtitle(_connectedAudioDevice.value)
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

    fun saveCurrentProfileAsCustom(name: String): Boolean {
        val baseId = profileRepository.getProfile(dolbyController.profile)?.baseProfileId ?: 0
        val newProfile = profileRepository.createCustomProfile(name, baseId)
        val newId = newProfile.id

        val editor = sharedPreferences.edit()
        editor.putInt("${DolbyConstants.PREF_IEQ}_$newId", dolbyController.ieqPreset)
        editor.putInt("${DolbyConstants.PREF_DIALOGUE}_$newId", dolbyController.dialogueEnhancerAmount)
        editor.putBoolean("${DolbyConstants.PREF_BASS}_$newId", dolbyController.bassEnhancerEnabled)
        editor.putInt("${DolbyConstants.PREF_STEREO}_$newId", dolbyController.stereoWideningAmount)
        editor.putBoolean("${DolbyConstants.PREF_VOLUME}_$newId", dolbyController.volumeLevelerEnabled)
        editor.putBoolean("${DolbyConstants.PREF_HP_VIRTUALIZER}_$newId", dolbyController.headphoneVirtEnabled)
        editor.putBoolean("${DolbyConstants.PREF_SPK_VIRTUALIZER}_$newId", dolbyController.speakerVirtEnabled)

        val activePresetId = sharedPreferences.getInt("${DolbyConstants.PREF_PRESET}_${dolbyController.profile}", 0)
        editor.putInt("${DolbyConstants.PREF_PRESET}_$newId", activePresetId)
        editor.apply()

        setProfile(newId)
        return true
    }

    fun renameCurrentProfile(newName: String) {
        profileRepository.renameCustomProfile(dolbyController.profile, newName)
        refreshAllSettings()
    }

    fun deleteCurrentProfile() {
        val currentId = dolbyController.profile
        if (profileRepository.isCustomProfile(currentId)) {
            profileRepository.deleteCustomProfile(currentId)
            setProfile(0)
        }
    }

    private fun refreshAllSettings() {
        _dsOn.value = dolbyController.dsOn
        _profile.value = dolbyController.profile
        _profiles.value = profileRepository.getAllProfiles()
        _isCurrentProfileCustom.value = profileRepository.isCustomProfile(dolbyController.profile)
        _ieqPreset.value = dolbyController.ieqPreset
        _dialogueEnhancerAmount.value = dolbyController.dialogueEnhancerAmount
        _bassEnhancerEnabled.value = dolbyController.bassEnhancerEnabled
        _stereoWideningAmount.value = dolbyController.stereoWideningAmount
        _volumeLevelerEnabled.value = dolbyController.volumeLevelerEnabled
        _headphoneVirtEnabled.value = dolbyController.headphoneVirtEnabled
        _speakerVirtEnabled.value = dolbyController.speakerVirtEnabled
        _presetName.value = getPresetDisplayName()
        _isOnSpeaker.value = dolbyController.isOnSpeaker()
        val device = getConnectedAudioDevice()
        _connectedAudioDevice.value = device
        _audioOutputDeviceTitle.value = device.title
        _audioOutputSubtitle.value = getAudioOutputSubtitle(device)
    }

    private fun getAudioOutputSubtitle(device: ConnectedAudioDevice = _connectedAudioDevice.value): String {
        val app = getApplication<Application>()
        return if (dolbyController.dsOn) {
            device.title
        } else {
            app.getString(co.aospa.dolby.xiaomi.R.string.dolby_off)
        }
    }

    private fun getPresetDisplayName(): String {
        val app = getApplication<Application>()
        val presetId = dolbyController.preset
        val preset = EqualizerRepository.getInstance(app).getPreset(presetId)
        return preset?.name ?: app.getString(co.aospa.dolby.xiaomi.R.string.dolby_preset_default)
    }

    fun createProfile(name: String, baseProfileId: Int) {
        val newProfile = profileRepository.createCustomProfile(name, baseProfileId)
        _profiles.value = profileRepository.getAllProfiles()
        setProfile(newProfile.id)
    }

    fun reorderProfiles(newOrder: List<Int>) {
        profileRepository.saveProfileOrder(newOrder)
        _profiles.value = profileRepository.getAllProfiles()
    }

    fun renameProfile(profileId: Int, newName: String) {
        profileRepository.renameCustomProfile(profileId, newName)
        _profiles.value = profileRepository.getAllProfiles()
    }

    fun deleteProfile(profileId: Int) {
        val isCurrentlySelected = dolbyController.profile == profileId
        profileRepository.deleteCustomProfile(profileId)
        _profiles.value = profileRepository.getAllProfiles()
        if (isCurrentlySelected) {
            setProfile(0)
        }
    }

    fun exportProfile(uri: Uri): Boolean {
        val app = getApplication<Application>()
        return DolbyConfigSerializer.exportProfileToUri(app, uri)
    }

    fun exportProfile(profileId: Int, uri: Uri): Boolean {
        val app = getApplication<Application>()
        return DolbyConfigSerializer.exportProfileToUri(app, profileId, uri)
    }

    fun importProfile(uri: Uri): Boolean {
        val app = getApplication<Application>()
        val success = DolbyConfigSerializer.importProfileFromUri(app, uri)
        if (success) {
            refreshAllSettings()
        }
        return success
    }

    private fun checkIsQsTileAdded(): Boolean {
        return try {
            val tiles = Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                "sysui_qs_tiles"
            )
            tiles?.contains("DolbyTileService") == true
        } catch (e: Exception) {
            false
        }
    }

    fun updateQsTileStatus() {
        _isQsTileAdded.value = checkIsQsTileAdded()
    }

    fun requestAddQsTile(context: Context) {
        if (_isQsTileAdded.value) return
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
                if (resultCode == 1 || resultCode == 2) {
                    _isQsTileAdded.value = true
                }
            }
        )
    }

    fun checkAndPromptAddQsTile(context: Context) {
        updateQsTileStatus()
        if (_isQsTileAdded.value) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val hasPrompted = prefs.getBoolean(PREF_KEY_QS_PROMPTED, false)
        if (!hasPrompted) {
            prefs.edit().putBoolean(PREF_KEY_QS_PROMPTED, true).apply()
            requestAddQsTile(context)
        }
    }

    private fun getConnectedAudioDevice(): ConnectedAudioDevice {
        val app = getApplication<Application>()

        // 1. Query audio policy for active media playback device
        try {
            val mediaAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            val mediaDevices = audioManager.getAudioDevicesForAttributes(mediaAttrs)
            val activeDevice = mediaDevices.firstOrNull {
                it.isSink && it.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO && it.type != AudioDeviceInfo.TYPE_TELEPHONY
            }
            if (activeDevice != null) {
                when (activeDevice.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_BLE_BROADCAST,
                    AudioDeviceInfo.TYPE_HEARING_AID -> {
                        val name = activeDevice.productName?.toString()?.trim() ?: ""
                        val title = if (name.isNotEmpty()) name else app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_bluetooth_generic)
                        return ConnectedAudioDevice(title, co.aospa.dolby.xiaomi.R.drawable.ic_output_bluetooth)
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        return ConnectedAudioDevice(app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_headphones), co.aospa.dolby.xiaomi.R.drawable.ic_output_headphones)
                    }
                    AudioDeviceInfo.TYPE_LINE_ANALOG,
                    AudioDeviceInfo.TYPE_LINE_DIGITAL -> {
                        return ConnectedAudioDevice(app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_line), co.aospa.dolby.xiaomi.R.drawable.ic_output_headphones)
                    }
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                        val name = activeDevice.productName?.toString()?.trim() ?: ""
                        val title = if (name.isNotEmpty()) name else app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_usb_generic)
                        return ConnectedAudioDevice(title, co.aospa.dolby.xiaomi.R.drawable.ic_output_usb)
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> {
                        return ConnectedAudioDevice(app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_speaker), co.aospa.dolby.xiaomi.R.drawable.ic_output_speaker)
                    }
                }
            }
        } catch (e: Exception) {
            dlog(TAG, "getAudioDevicesForAttributes failed: ${e.message}")
        }

        // 2. Fallback scan of connected output devices
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val bluetoothDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
            it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST ||
            it.type == AudioDeviceInfo.TYPE_HEARING_AID
        }
        if (bluetoothDevice != null) {
            val name = bluetoothDevice.productName?.toString()?.trim() ?: ""
            val title = if (name.isNotEmpty()) name else app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_bluetooth_generic)
            return ConnectedAudioDevice(title, co.aospa.dolby.xiaomi.R.drawable.ic_output_bluetooth)
        }

        val wiredDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
            it.type == AudioDeviceInfo.TYPE_LINE_DIGITAL
        }
        if (wiredDevice != null) {
            val title = if (wiredDevice.type == AudioDeviceInfo.TYPE_LINE_ANALOG || wiredDevice.type == AudioDeviceInfo.TYPE_LINE_DIGITAL) {
                app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_line)
            } else {
                app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_headphones)
            }
            return ConnectedAudioDevice(title, co.aospa.dolby.xiaomi.R.drawable.ic_output_headphones)
        }

        val usbDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
        if (usbDevice != null) {
            val name = usbDevice.productName?.toString()?.trim() ?: ""
            val title = if (name.isNotEmpty()) name else app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_usb_generic)
            return ConnectedAudioDevice(title, co.aospa.dolby.xiaomi.R.drawable.ic_output_usb)
        }

        return ConnectedAudioDevice(app.getString(co.aospa.dolby.xiaomi.R.string.dolby_output_speaker), co.aospa.dolby.xiaomi.R.drawable.ic_output_speaker)
    }

    companion object {
        private const val TAG = "DolbyViewModel"
        private const val PREF_KEY_QS_PROMPTED = "dolby_qs_tile_prompted"
    }
}
