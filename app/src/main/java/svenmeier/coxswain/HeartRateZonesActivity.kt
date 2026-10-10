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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import svenmeier.coxswain.compose.CoxswainTheme

class HeartRateZonesActivity : ComponentActivity() {
    private var recordedRange by mutableStateOf(RecordedHeartRateRange())
    private var peakLoading by mutableStateOf(true)
    private var peakFailed by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val initial = HeartRateZones.decode(prefs.getString(HeartRateZones.KEY, null))
        lifecycleScope.launch {
            try {
                recordedRange = withContext(Dispatchers.IO) {
                    val gym = Gym.instance(this@HeartRateZonesActivity)
                    var low: Int? = null
                    var peak: Int? = null
                    for (workout in ArrayList(gym.allWorkouts.list())) {
                        if (workout.status.get() != svenmeier.coxswain.gym.WorkoutStatus.COMPLETED) continue
                        val range = recordedHeartRateRange(workout, gym.getSnapshots(workout).list())
                        range.startingLow?.let { low = minOf(low ?: it, it) }
                        range.peak?.let { peak = maxOf(peak ?: it, it) }
                    }
                    RecordedHeartRateRange(low, peak)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                peakFailed = true
            } finally { peakLoading = false }
        }
        setContent {
            CoxswainTheme {
                HeartRateZonesSettings(initial, onBack = { finish() }, onSave = { zones ->
                    prefs.edit {
                        if (zones == null) remove(HeartRateZones.KEY) else putString(HeartRateZones.KEY, zones.encode())
                    }
                    finish()
                }, recordedRange = recordedRange, peakLoading = peakLoading, peakFailed = peakFailed)
            }
        }
    }
}

@Composable
internal fun HeartRateZonesSettings(initial: HeartRateZones?, onBack: () -> Unit,
                                     onSave: (HeartRateZones?) -> Unit, recordedRange: RecordedHeartRateRange = RecordedHeartRateRange(),
                                     peakLoading: Boolean = false, peakFailed: Boolean = false) {
    var custom by rememberSaveable { mutableStateOf(initial != null && initial.resting == null) }
    var estimate by rememberSaveable { mutableStateOf(initial?.age != null) }
    var resting by rememberSaveable { mutableStateOf(initial?.resting?.toString() ?: "") }
    var maximum by rememberSaveable { mutableStateOf(initial?.maximum?.toString() ?: "") }
    var age by rememberSaveable { mutableStateOf(initial?.age?.toString() ?: "") }
    var moderate by rememberSaveable { mutableStateOf(initial?.moderate?.toString() ?: "") }
    var vigorous by rememberSaveable { mutableStateOf(initial?.vigorous?.toString() ?: "") }
    var peak by rememberSaveable { mutableStateOf(initial?.peak?.toString() ?: "") }
    var restingEdited by rememberSaveable { mutableStateOf(false) }
    var maximumEdited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(recordedRange) {
        if (initial == null) {
            if (!restingEdited && resting.isBlank()) recordedRange.startingLow?.let { resting = it.toString() }
            if (!maximumEdited && maximum.isBlank()) recordedRange.peak?.let { maximum = it.toString() }
        }
    }
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
                HeartNumber(stringResource(R.string.hr_resting), resting) { restingEdited = true; resting = it }
                Text(stringResource(when {
                    peakLoading -> R.string.hr_peak_loading
                    peakFailed -> R.string.hr_peak_failed
                    recordedRange.startingLow == null || recordedRange.peak == null -> R.string.hr_peak_unavailable
                    else -> R.string.hr_history_values
                }, recordedRange.startingLow ?: 0, recordedRange.peak ?: 0),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (recordedRange.startingLow != null && recordedRange.peak != null) {
                    Text(stringResource(R.string.hr_history_help), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        resting = recordedRange.startingLow.toString()
                        maximum = recordedRange.peak.toString()
                        estimate = false
                        restingEdited = true
                        maximumEdited = true
                    }) { Text(stringResource(R.string.hr_use_history)) }
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(checked = estimate, onCheckedChange = { estimate = it })
                    Text(stringResource(R.string.hr_estimate), Modifier.padding(start = 8.dp))
                }
                if (estimate) {
                    HeartNumber(stringResource(R.string.hr_age), age) { age = it }
                    Text(stringResource(R.string.hr_estimate_help), style = MaterialTheme.typography.bodySmall)
                } else HeartNumber(stringResource(R.string.hr_maximum), maximum) { maximumEdited = true; maximum = it }
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
