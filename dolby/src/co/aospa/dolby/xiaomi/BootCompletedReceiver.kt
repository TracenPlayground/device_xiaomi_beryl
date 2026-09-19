/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        dlog(TAG, "Received " + intent.action)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DolbyController.getInstance(context).onBootCompleted()
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
