/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.media.audiofx.AudioEffect
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.elog
import co.aospa.dolby.xiaomi.DolbyConstants.DsParam
import java.util.UUID

class DolbyAudioEffect(priority: Int, audioSession: Int) :
    AudioEffect(EFFECT_TYPE_NULL, findDapEffectUuid(), priority, audioSession) {

    var dsOn: Boolean
        get() = try {
            getIntParam(EFFECT_PARAM_ENABLE) == 1
        } catch (e: Exception) {
            dlog(TAG, "Error getting dsOn: ${e.message}")
            false
        }
        set(value) {
            try {
                setIntParam(EFFECT_PARAM_ENABLE, if (value) 1 else 0)
                checkStatus(setEnabled(value))
            } catch (e: Exception) {
                elog(TAG, "Error setting dsOn: ${e.message}", e)
            }
        }

    var profile: Int
        get() = try {
            getIntParam(EFFECT_PARAM_PROFILE)
        } catch (e: Exception) {
            dlog(TAG, "Error getting profile: ${e.message}")
            0
        }
        set(value) {
            try {
                setIntParam(EFFECT_PARAM_PROFILE, value)
            } catch (e: Exception) {
                elog(TAG, "Error setting profile: ${e.message}", e)
            }
        }

    private fun setIntParam(param: Int, value: Int) {
        dlog(TAG, "setIntParam($param, $value)")
        val buf = ByteArray(12)
        int32ToByteArray(param, buf, 0)
        int32ToByteArray(1, buf, 4)
        int32ToByteArray(value, buf, 8)
        checkStatus(setParameter(EFFECT_PARAM_CPDP_VALUES, buf))
    }

    private fun getIntParam(param: Int): Int {
        val buf = ByteArray(12)
        int32ToByteArray(param, buf, 0)
        checkStatus(getParameter(EFFECT_PARAM_CPDP_VALUES + param, buf))
        return byteArrayToInt32(buf).also { dlog(TAG, "getIntParam($param): $it") }
    }

    fun resetProfileSpecificSettings(profile: Int = this.profile) {
        dlog(TAG, "resetProfileSpecificSettings: profile=$profile")
        try {
            setIntParam(EFFECT_PARAM_RESET_PROFILE_SETTINGS, profile)
        } catch (e: Exception) {
            elog(TAG, "Error resetting profile settings: ${e.message}", e)
        }
    }

    fun setDapParameter(param: DsParam, values: IntArray, profile: Int = this.profile) {
        dlog(TAG, "setDapParameter: profile=$profile param=$param")
        val length = values.size
        val buf = ByteArray((length + 4) * 4)
        int32ToByteArray(EFFECT_PARAM_SET_PROFILE_PARAMETER, buf, 0)
        int32ToByteArray(length + 1, buf, 4)
        int32ToByteArray(profile, buf, 8)
        int32ToByteArray(param.id, buf, 12)
        int32ArrayToByteArray(values, buf, 16)
        checkStatus(setParameter(EFFECT_PARAM_CPDP_VALUES, buf))
    }

    fun setDapParameter(param: DsParam, enable: Boolean, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(if (enable) 1 else 0), profile)

    fun setDapParameter(param: DsParam, value: Int, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(value), profile)

    fun getDapParameter(param: DsParam, profile: Int = this.profile): IntArray {
        dlog(TAG, "getDapParameter: profile=$profile param=$param")
        val length = param.length
        val buf = ByteArray((length + 2) * 4)
        val p = (param.id shl 16) + (profile shl 8) + EFFECT_PARAM_GET_PROFILE_PARAMETER
        checkStatus(getParameter(p, buf))
        return byteArrayToInt32Array(buf, length)
    }

    fun getDapParameterBool(param: DsParam, profile: Int = this.profile): Boolean =
        try {
            getDapParameter(param, profile)[0] == 1
        } catch (e: Exception) {
            dlog(TAG, "Error getting bool param $param: ${e.message}")
            false
        }

    fun getDapParameterInt(param: DsParam, profile: Int = this.profile): Int =
        try {
            getDapParameter(param, profile)[0]
        } catch (e: Exception) {
            dlog(TAG, "Error getting int param $param: ${e.message}")
            0
        }

    companion object {
        private const val TAG = "DolbyAudioEffect"
        val EFFECT_TYPE_DAP: UUID = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa")
        val EFFECT_TYPE_DOLBY: UUID = UUID.fromString("fa81dbde-588b-11ed-9b6a-0242ac120002")

        private const val EFFECT_PARAM_ENABLE = 0
        private const val EFFECT_PARAM_CPDP_VALUES = 5
        private const val EFFECT_PARAM_PROFILE = 0xA000000
        private const val EFFECT_PARAM_SET_PROFILE_PARAMETER = 0x1000000
        private const val EFFECT_PARAM_GET_PROFILE_PARAMETER = 0x1000005
        private const val EFFECT_PARAM_RESET_PROFILE_SETTINGS = 0xC000000

        private fun findDapEffectUuid(): UUID {
            try {
                val descriptors = queryEffects() ?: emptyArray()
                val known = descriptors.firstOrNull { it.uuid == EFFECT_TYPE_DAP }
                if (known != null) return known.uuid

                val byType = descriptors.firstOrNull {
                    it.type == EFFECT_TYPE_DOLBY && it.name.contains("dap", ignoreCase = true)
                }
                if (byType != null) return byType.uuid

                val matched = descriptors.firstOrNull {
                    it.name.contains("dap", ignoreCase = true) ||
                        it.implementor.contains("Dolby", ignoreCase = true)
                }
                if (matched != null) return matched.uuid
            } catch (e: Exception) {
                elog(TAG, "Error querying audio effects: ${e.message}", e)
            }
            return EFFECT_TYPE_DAP
        }

        private fun int32ToByteArray(value: Int, dst: ByteArray, index: Int) {
            var idx = index
            dst[idx++] = (value and 0xff).toByte()
            dst[idx++] = ((value ushr 8) and 0xff).toByte()
            dst[idx++] = ((value ushr 16) and 0xff).toByte()
            dst[idx] = ((value ushr 24) and 0xff).toByte()
        }

        private fun byteArrayToInt32(ba: ByteArray): Int {
            return ((ba[3].toInt() and 0xff) shl 24) or
                ((ba[2].toInt() and 0xff) shl 16) or
                ((ba[1].toInt() and 0xff) shl 8) or
                (ba[0].toInt() and 0xff)
        }

        private fun int32ArrayToByteArray(src: IntArray, dst: ByteArray, index: Int) {
            var idx = index
            for (x in src) {
                dst[idx++] = (x and 0xff).toByte()
                dst[idx++] = ((x ushr 8) and 0xff).toByte()
                dst[idx++] = ((x ushr 16) and 0xff).toByte()
                dst[idx++] = ((x ushr 24) and 0xff).toByte()
            }
        }

        private fun byteArrayToInt32Array(ba: ByteArray, dstLength: Int): IntArray {
            val srcLength = ba.size shr 2
            val dst = IntArray(dstLength.coerceAtMost(srcLength))
            for (i in dst.indices) {
                dst[i] =
                    ((ba[i * 4 + 3].toInt() and 0xff) shl 24) or
                        ((ba[i * 4 + 2].toInt() and 0xff) shl 16) or
                        ((ba[i * 4 + 1].toInt() and 0xff) shl 8) or
                        (ba[i * 4].toInt() and 0xff)
            }
            return dst
        }
    }
}
