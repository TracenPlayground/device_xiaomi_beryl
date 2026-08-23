/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.ui

import androidx.annotation.StringRes
import co.aospa.dolby.xiaomi.R

enum class PresetNameValidationError(@StringRes val errorMessageRes: Int) {
    ALREADY_EXISTS(R.string.dolby_geq_preset_name_exists),
    TOO_LONG(R.string.dolby_geq_preset_name_too_long)
}
