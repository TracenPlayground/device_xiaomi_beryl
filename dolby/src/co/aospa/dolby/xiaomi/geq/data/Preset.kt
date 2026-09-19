/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.data

data class Preset(
    val id: Int,
    val name: String,
    val bandGains: List<BandGain>,
    val isReadOnly: Boolean = false
)
