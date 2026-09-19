/*
 * Copyright (C) 2023-2024 Paranoid Android
 * Copyright (C) 2024-2026 Halcyon Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.geq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import co.aospa.dolby.xiaomi.R
import co.aospa.dolby.xiaomi.geq.data.BandGain

@Composable
fun EqualizerBands(
    bandGains: List<BandGain>,
    onBandGainChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bandFreqs = stringArrayResource(R.array.dolby_geq_band_freqs)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dolby_preset),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.dolby_geq_slider_label_gain),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            (0..9).forEach { band ->
                val freqLabel = bandFreqs.getOrElse(band) { "${band + 1}" }
                val currentGain = bandGains.firstOrNull { it.band == band }?.gain ?: 0

                BandGainSlider(
                    band = band,
                    freqLabel = freqLabel,
                    gain = currentGain,
                    onGainChange = { newGain ->
                        onBandGainChange(band, newGain)
                    }
                )

                if (band < 9) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
