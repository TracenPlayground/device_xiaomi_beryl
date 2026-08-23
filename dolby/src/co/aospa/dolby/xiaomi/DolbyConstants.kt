/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.util.Log

class DolbyConstants {

    enum class DsParam(val id: Int) {
        // ... (preserving all DS parameters)
        GEN_PARAM_INVALID(0),
        DAP_GEQ_BAND_FREQS(1),
        DAP_GEQ_BAND_GAINS(2),
        DAP_GEN_PROFILE(3),
        DAP_PARAM_DE_AMOUNT(4),
        DAP_PARAM_DE_ENABLE(5),
        DAP_PARAM_IEQ_AMOUNT(6),
        DAP_PARAM_IEQ_ENABLE(7),
        DAP_PARAM_VOL_LEVELER_ENABLE(8),
        DAP_PARAM_VOL_LEVELER_AMOUNT(9),
        DAP_PARAM_SURROUND_BOOST(10),
        DAP_PARAM_SURROUND_DECODER_ENABLE(11),
        DAP_PARAM_POSTGAIN(12),
        DAP_PARAM_PREGAIN(13),
        DAP_PARAM_SYSTEM_GAIN(14),
        DAP_PARAM_VIRTUALIZER_ENABLE(15),
        DAP_PARAM_BASS_BOOST(16),
        DAP_PARAM_BASS_ENABLE(17),
        DAP_PARAM_GEQ_ENABLE(18),
        DAP_PARAM_SPEAKER_VIRTUALIZER_ENABLE(19),
        DAP_PARAM_VIRT_SURROUND_BOOST(20),
        DAP_PARAM_IEQ_PRESET(21),
        DAP_PARAM_GLOBAL_ENABLE(22),
        DAP_PARAM_BASS_EXTRA_BOOST(23),
        DAP_PARAM_HP_VIRTUALIZER_ENABLE(24),
        DAP_PARAM_UNKNOWN(25);

        companion object {
            fun fromInt(value: Int) = entries.firstOrNull { it.id == value } ?: DAP_PARAM_UNKNOWN
        }
    }

    companion object {
        const val TAG = "XiaomiDolby"
        val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

        const val PREF_ENABLE = "dolby_enable"
        const val PREF_PROFILE = "dolby_profile"
        const val PREF_PRESET = "dolby_preset"
        const val PREF_IEQ = "dolby_ieq"
        const val PREF_DIALOGUE = "dolby_dialogue"
        const val PREF_BASS = "dolby_bass"
        const val PREF_STEREO = "dolby_stereo"
        const val PREF_VOLUME = "dolby_volume"
        const val PREF_HP_VIRTUALIZER = "dolby_hp_virtualizer"
        const val PREF_SPK_VIRTUALIZER = "dolby_spk_virtualizer"
        const val PREF_RESET = "dolby_reset"

        fun dlog(tag: String, msg: String) {
            if (DEBUG) Log.d(tag, msg)
        }
    }
}
