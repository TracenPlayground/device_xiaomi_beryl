/*
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.R

data class DolbyProfile(
    val id: Int,
    val name: String,
    val iconRes: Int,
    val isCustom: Boolean = false,
    val baseProfileId: Int = 0
)

class ProfileRepository private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getBuiltInProfiles(): List<DolbyProfile> {
        val entries = context.resources.getStringArray(R.array.dolby_profile_entries)
        val values = context.resources.getStringArray(R.array.dolby_profile_values)
        return values.mapIndexed { index, valueStr ->
            val id = valueStr.toInt()
            val name = entries.getOrElse(index) { valueStr }
            val iconRes = when (id) {
                1 -> R.drawable.ic_dolby_movie
                2 -> R.drawable.ic_dolby_music
                8 -> R.drawable.ic_dolby_voice
                else -> R.drawable.ic_dolby_dynamic
            }
            DolbyProfile(
                id = id,
                name = name,
                iconRes = iconRes,
                isCustom = false,
                baseProfileId = id
            )
        }
    }

    private fun getCustomProfileIds(): List<Int> {
        val stored = sharedPreferences.getString(KEY_CUSTOM_PROFILE_IDS, null) ?: return emptyList()
        if (stored.isEmpty()) return emptyList()
        return stored.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    private fun saveCustomProfileIds(ids: List<Int>, editor: SharedPreferences.Editor? = null) {
        val targetEditor = editor ?: sharedPreferences.edit()
        targetEditor.putString(KEY_CUSTOM_PROFILE_IDS, ids.joinToString(","))
        if (editor == null) {
            targetEditor.apply()
        }
    }

    fun getAllProfiles(): List<DolbyProfile> {
        val profiles = mutableListOf<DolbyProfile>()
        profiles.addAll(getBuiltInProfiles())

        val customIds = getCustomProfileIds()
        for (id in customIds) {
            val name = sharedPreferences.getString("${KEY_CUSTOM_PROFILE_NAME}_$id", null) ?: continue
            val baseId = sharedPreferences.getInt("${KEY_CUSTOM_PROFILE_BASE_ID}_$id", 0)
            profiles.add(
                DolbyProfile(
                    id = id,
                    name = name,
                    iconRes = R.drawable.ic_dolby_user,
                    isCustom = true,
                    baseProfileId = baseId
                )
            )
        }
        return profiles
    }

    fun getProfile(id: Int): DolbyProfile? {
        val all = getAllProfiles()
        return all.firstOrNull { it.id == id }
    }

    fun isCustomProfile(id: Int): Boolean = id >= CUSTOM_PROFILE_BASE_ID

    fun getProfileName(id: Int): String {
        return getProfile(id)?.name ?: context.getString(R.string.dolby_unknown)
    }

    fun getProfileIcon(id: Int): Int {
        return getProfile(id)?.iconRes ?: R.drawable.ic_dolby_dynamic
    }

    fun createCustomProfile(name: String, baseProfileId: Int = 0): DolbyProfile {
        val customIds = getCustomProfileIds().toMutableList()
        val nextId = (customIds.maxOrNull() ?: (CUSTOM_PROFILE_BASE_ID - 1)) + 1
        customIds.add(nextId)

        val editor = sharedPreferences.edit()
        saveCustomProfileIds(customIds, editor)
        editor.putString("${KEY_CUSTOM_PROFILE_NAME}_$nextId", name)
        editor.putInt("${KEY_CUSTOM_PROFILE_BASE_ID}_$nextId", baseProfileId)
        editor.apply()

        return DolbyProfile(
            id = nextId,
            name = name,
            iconRes = R.drawable.ic_dolby_user,
            isCustom = true,
            baseProfileId = baseProfileId
        )
    }

    fun renameCustomProfile(id: Int, newName: String) {
        if (isCustomProfile(id)) {
            sharedPreferences.edit().putString("${KEY_CUSTOM_PROFILE_NAME}_$id", newName).apply()
        }
    }

    fun deleteCustomProfile(id: Int) {
        if (isCustomProfile(id)) {
            val customIds = getCustomProfileIds().toMutableList()
            customIds.remove(id)

            val editor = sharedPreferences.edit()
            saveCustomProfileIds(customIds, editor)
            editor.remove("${KEY_CUSTOM_PROFILE_NAME}_$id")
            editor.remove("${KEY_CUSTOM_PROFILE_BASE_ID}_$id")

            // Clean up profile preference keys
            val keysToRemove = listOf(
                "${DolbyConstants.PREF_PRESET}_$id",
                "${DolbyConstants.PREF_IEQ}_$id",
                "${DolbyConstants.PREF_DIALOGUE}_$id",
                "${DolbyConstants.PREF_BASS}_$id",
                "${DolbyConstants.PREF_STEREO}_$id",
                "${DolbyConstants.PREF_VOLUME}_$id",
                "${DolbyConstants.PREF_HP_VIRTUALIZER}_$id",
                "${DolbyConstants.PREF_SPK_VIRTUALIZER}_$id"
            )
            keysToRemove.forEach { editor.remove(it) }

            // If current profile was deleted, fall back to default profile 0
            if (sharedPreferences.getInt(DolbyConstants.PREF_PROFILE, 0) == id) {
                editor.putInt(DolbyConstants.PREF_PROFILE, 0)
            }

            editor.apply()
        }
    }

    companion object {
        private const val KEY_CUSTOM_PROFILE_IDS = "custom_profile_ids"
        private const val KEY_CUSTOM_PROFILE_NAME = "custom_profile_name"
        private const val KEY_CUSTOM_PROFILE_BASE_ID = "custom_profile_base_id"
        const val CUSTOM_PROFILE_BASE_ID = 100

        @Volatile
        private var instance: ProfileRepository? = null

        fun getInstance(context: Context): ProfileRepository {
            return instance ?: synchronized(this) {
                instance ?: ProfileRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
