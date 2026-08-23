/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.media.audiofx.AudioEffect
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyConstants.DsParam
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class DolbyAudioEffect(priority: Int, audioSession: Int) :
    AudioEffect(EFFECT_TYPE_NULL, EFFECT_TYPE_DAP, priority, audioSession) {

    fun setDsOn(on: Boolean) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            .putInt(if (on) 1 else 0).array()
        setParameter(DsParam.DAP_PARAM_GLOBAL_ENABLE.id, bytes)
    }

    fun getDsOn(): Boolean {
        val bytes = IntArray(1)
        getParameter(DsParam.DAP_PARAM_GLOBAL_ENABLE.id, bytes)
        return bytes[0] == 1
    }

    fun setProfile(profile: Int) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            .putInt(profile).array()
        setParameter(DsParam.DAP_GEN_PROFILE.id, bytes)
    }

    fun getProfile(): Int {
        val bytes = IntArray(1)
        getParameter(DsParam.DAP_GEN_PROFILE.id, bytes)
        return bytes[0]
    }

    fun setGeqBandGain(band: Int, gain: Int) {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.nativeOrder())
            .putInt(band).putInt(gain).array()
        setParameter(DsParam.DAP_GEQ_BAND_GAINS.id, bytes)
    }

    fun getGeqBandGain(band: Int): Int {
        val bytes = IntArray(1)
        getParameter(DsParam.DAP_GEQ_BAND_GAINS.id, bytes)
        return bytes[0]
    }

    fun setDapParameter(param: DsParam, value: Int) {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            .putInt(value).array()
        setParameter(param.id, bytes)
    }

    fun setDapParameterBool(param: DsParam, value: Boolean) {
        setDapParameter(param, if (value) 1 else 0)
    }

    fun getDapParameter(param: DsParam): Int {
        val bytes = IntArray(1)
        getParameter(param.id, bytes)
        return bytes[0]
    }

    fun getDapParameterBool(param: DsParam): Boolean {
        return getDapParameter(param) == 1
    }

    fun setDapParameterIntArray(param: DsParam, values: IntArray) {
        val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.nativeOrder())
        values.forEach { buffer.putInt(it) }
        setParameter(param.id, buffer.array())
    }

    fun getDapParameterIntArray(param: DsParam, length: Int): IntArray {
        val values = IntArray(length)
        getParameter(param.id, values)
        return values
    }

    companion object {
        private val EFFECT_TYPE_DAP = UUID.fromString("46d279d9-9be7-453d-9d7c-ef937f675587")
        private const val TAG = "DolbyAudioEffect"
    }
}
