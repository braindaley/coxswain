package svenmeier.coxswain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import svenmeier.coxswain.compose.CoxswainTheme

class HeartRateZonesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val initial = HeartRateZones.decode(prefs.getString(HeartRateZones.KEY, null))
        setContent {
            CoxswainTheme {
                HeartRateZonesSettings(initial, onBack = { finish() }, onSave = { zones ->
                    prefs.edit {
                        if (zones == null) remove(HeartRateZones.KEY) else putString(HeartRateZones.KEY, zones.encode())
                    }
                    finish()
                })
            }
        }
    }
}

@Composable
internal fun HeartRateZonesSettings(initial: HeartRateZones?, onBack: () -> Unit,
                                     onSave: (HeartRateZones?) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(initial != null && initial.resting == null) }
    var estimate by rememberSaveable { mutableStateOf(initial?.age != null) }
    var resting by rememberSaveable { mutableStateOf(initial?.resting?.toString() ?: "") }
    var maximum by rememberSaveable { mutableStateOf(initial?.maximum?.toString() ?: "") }
    var age by rememberSaveable { mutableStateOf(initial?.age?.toString() ?: "") }
    var moderate by rememberSaveable { mutableStateOf(initial?.moderate?.toString() ?: "") }
    var vigorous by rememberSaveable { mutableStateOf(initial?.vigorous?.toString() ?: "") }
    var peak by rememberSaveable { mutableStateOf(initial?.peak?.toString() ?: "") }
    val profile = runCatching {
        if (custom) HeartRateZones(moderate.toInt(), vigorous.toInt(), peak.toInt())
        else HeartRateZones.reserve(resting.toInt(), if (estimate) 220 - age.toInt() else maximum.toInt(),
            if (estimate) age.toInt() else null)
    }.getOrNull()
    Surface {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.ui_back)) }
            Text(stringResource(R.string.hr_zones_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.hr_zones_explanation))
            Row {
                FilterChip(selected = !custom, onClick = { custom = false }, label = { Text(stringResource(R.string.hr_calculate)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = custom, onClick = { custom = true }, label = { Text(stringResource(R.string.hr_custom)) })
            }
            if (custom) {
                Text(stringResource(R.string.hr_custom_help))
                HeartNumber(stringResource(R.string.hr_moderate_from), moderate) { moderate = it }
                HeartNumber(stringResource(R.string.hr_vigorous_from), vigorous) { vigorous = it }
                HeartNumber(stringResource(R.string.hr_peak_from), peak) { peak = it }
            } else {
                HeartNumber(stringResource(R.string.hr_resting), resting) { resting = it }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = estimate, onCheckedChange = { estimate = it })
                    Text(stringResource(R.string.hr_estimate), Modifier.padding(start = 8.dp))
                }
                if (estimate) {
                    HeartNumber(stringResource(R.string.hr_age), age) { age = it }
                    Text(stringResource(R.string.hr_estimate_help), style = MaterialTheme.typography.bodySmall)
                } else HeartNumber(stringResource(R.string.hr_maximum), maximum) { maximum = it }
            }
            if (profile != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (estimate && !custom) Text(stringResource(R.string.hr_estimated_max, profile.maximum!!))
                        (0..3).forEach { zone ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(heartZoneName(zone)))
                                Text(profile.bounds(zone))
                            }
                        }
                    }
                }
            } else Text(stringResource(if (custom) R.string.hr_invalid_custom else R.string.hr_invalid_reserve),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.hr_frozen_help), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onSave(profile) }, enabled = profile != null, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hr_save))
            }
            if (initial != null) OutlinedButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hr_disable))
            }
        }
    }
}

@Composable
private fun HeartNumber(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { input -> if (input.length <= 3 && input.all { it.isDigit() }) onChange(input) },
        label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth())
}

internal fun heartZoneName(zone: Int): Int = when (zone) {
    0 -> R.string.ui_chart_light
    1 -> R.string.ui_chart_moderate
    2 -> R.string.ui_chart_vigorous
    else -> R.string.ui_chart_peak
}
