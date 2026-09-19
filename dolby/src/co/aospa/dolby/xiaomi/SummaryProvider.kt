/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import co.aospa.dolby.xiaomi.R

class SummaryProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val bundle = Bundle()
        val context = context ?: return bundle
        val dolbyController = DolbyController.getInstance(context)

        val summary = if (dolbyController.dsOn) {
            val profileName = dolbyController.getProfileName() ?: context.getString(R.string.dolby_unknown)
            context.getString(R.string.dolby_on_with_profile, profileName)
        } else {
            context.getString(R.string.dolby_off)
        }

        bundle.putString(KEY_SUMMARY, summary)
        return bundle
    }

    companion object {
        private const val KEY_SUMMARY = "com.android.settings.summary"
    }
}
