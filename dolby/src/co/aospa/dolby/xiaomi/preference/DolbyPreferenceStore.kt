/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.preference

import android.content.Context
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants

class DolbyPreferenceStore(context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var dsOn: Boolean
        get() = sharedPreferences.getBoolean(DolbyConstants.PREF_ENABLE, false)
        set(value) = sharedPreferences.edit().putBoolean(DolbyConstants.PREF_ENABLE, value).apply()

    var profile: Int
        get() = sharedPreferences.getInt(DolbyConstants.PREF_PROFILE, 0)
        set(value) = sharedPreferences.edit().putInt(DolbyConstants.PREF_PROFILE, value).apply()

    var ieqPreset: Int
        get() = getProfileInt(DolbyConstants.PREF_IEQ, 0)
        set(value) = setProfileInt(DolbyConstants.PREF_IEQ, value)

    var dialogueEnhancerAmount: Int
        get() = getProfileInt(DolbyConstants.PREF_DIALOGUE, 0)
        set(value) = setProfileInt(DolbyConstants.PREF_DIALOGUE, value)

    var bassEnhancerEnabled: Boolean
        get() = getProfileBoolean(DolbyConstants.PREF_BASS, false)
        set(value) = setProfileBoolean(DolbyConstants.PREF_BASS, value)

    var stereoWideningAmount: Int
        get() = getProfileInt(DolbyConstants.PREF_STEREO, 0)
        set(value) = setProfileInt(DolbyConstants.PREF_STEREO, value)

    var volumeLevelerEnabled: Boolean
        get() = getProfileBoolean(DolbyConstants.PREF_VOLUME, false)
        set(value) = setProfileBoolean(DolbyConstants.PREF_VOLUME, value)

    var headphoneVirtEnabled: Boolean
        get() = getProfileBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, false)
        set(value) = setProfileBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, value)

    var speakerVirtEnabled: Boolean
        get() = getProfileBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, false)
        set(value) = setProfileBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, value)

    private fun getProfileKey(key: String): String = "${key}_${profile}"

    private fun getProfileInt(key: String, defValue: Int): Int =
        sharedPreferences.getInt(getProfileKey(key), defValue)

    private fun setProfileInt(key: String, value: Int) =
        sharedPreferences.edit().putInt(getProfileKey(key), value).apply()

    private fun getProfileBoolean(key: String, defValue: Boolean): Boolean =
        sharedPreferences.getBoolean(getProfileKey(key), defValue)

    private fun setProfileBoolean(key: String, value: Boolean) =
        sharedPreferences.edit().putBoolean(getProfileKey(key), value).apply()

    fun resetProfile(profileId: Int) {
        val editor = sharedPreferences.edit()
        val keysToRemove = listOf(
            "${DolbyConstants.PREF_IEQ}_$profileId",
            "${DolbyConstants.PREF_DIALOGUE}_$profileId",
            "${DolbyConstants.PREF_BASS}_$profileId",
            "${DolbyConstants.PREF_STEREO}_$profileId",
            "${DolbyConstants.PREF_VOLUME}_$profileId",
            "${DolbyConstants.PREF_HP_VIRTUALIZER}_$profileId",
            "${DolbyConstants.PREF_SPK_VIRTUALIZER}_$profileId"
        )
        keysToRemove.forEach { editor.remove(it) }
        editor.apply()
    }
}
