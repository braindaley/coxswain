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
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import svenmeier.coxswain.compose.CoxswainTheme

class PerformanceZonesActivity : ComponentActivity() {
    private var suggested by mutableStateOf<PerformanceZones?>(null)
    private var loading by mutableStateOf(true)
    private var failed by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val initial = PerformanceZones.decode(prefs.getString(PerformanceZones.KEY, null))
        lifecycleScope.launch {
            try { suggested = withContext(Dispatchers.IO) { PerformanceZones.suggest(Gym.instance(this@PerformanceZonesActivity)) } }
            catch (error: Exception) { if (error is CancellationException) throw error; failed = true }
            finally { loading = false }
        }
        setContent { CoxswainTheme { PerformanceZonesSettings(initial, suggested, loading, failed, { finish() }) {
            prefs.edit { if (it == null) remove(PerformanceZones.KEY) else putString(PerformanceZones.KEY, it.encode()) }
            finish()
        } } }
    }
}

@Composable
internal fun PerformanceZonesSettings(initial: PerformanceZones?, suggestion: PerformanceZones?, loading: Boolean,
                                      failed: Boolean, onBack: () -> Unit, onSave: (PerformanceZones?) -> Unit) {
    var powerModerate by rememberSaveable { mutableStateOf(initial?.power?.moderate?.toString() ?: "") }
    var powerVigorous by rememberSaveable { mutableStateOf(initial?.power?.vigorous?.toString() ?: "") }
    var powerPeak by rememberSaveable { mutableStateOf(initial?.power?.peak?.toString() ?: "") }
    var paceModerate by rememberSaveable { mutableStateOf(initial?.pace?.moderate?.let(::formatZoneSplit) ?: "") }
    var paceVigorous by rememberSaveable { mutableStateOf(initial?.pace?.vigorous?.let(::formatZoneSplit) ?: "") }
    var pacePeak by rememberSaveable { mutableStateOf(initial?.pace?.peak?.let(::formatZoneSplit) ?: "") }
    var edited by rememberSaveable { mutableStateOf(false) }
    fun applySuggestion() {
        suggestion?.power?.let {
            powerModerate = it.moderate.toString()
            powerVigorous = it.vigorous.toString()
            powerPeak = it.peak.toString()
        }
        suggestion?.pace?.let {
            paceModerate = formatZoneSplit(it.moderate)
            paceVigorous = formatZoneSplit(it.vigorous)
            pacePeak = formatZoneSplit(it.peak)
        }
    }
    LaunchedEffect(suggestion) { if (initial == null && !edited) applySuggestion() }
    val profile = runCatching {
        val power = if (listOf(powerModerate, powerVigorous, powerPeak).all { it.isBlank() }) null
            else OutputZones(powerModerate.toInt(), powerVigorous.toInt(), powerPeak.toInt())
        val pace = if (listOf(paceModerate, paceVigorous, pacePeak).all { it.isBlank() }) null
            else OutputZones(requireNotNull(parseZoneSplit(paceModerate)), requireNotNull(parseZoneSplit(paceVigorous)), requireNotNull(parseZoneSplit(pacePeak)), true)
        PerformanceZones(power, pace)
    }.getOrNull()
    Surface {
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.ui_back)) }
            Text(stringResource(R.string.output_zones_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.output_zones_help))
            if (loading) Text(stringResource(R.string.hr_peak_loading))
            else if (failed) Text(stringResource(R.string.output_zones_failed))
            else if (suggestion == null) Text(stringResource(R.string.output_zones_unavailable))
            if (suggestion != null) TextButton(onClick = { edited = true; applySuggestion() }) { Text(stringResource(R.string.hr_use_history)) }
            Text(stringResource(R.string.ui_power), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.output_power_help), style = MaterialTheme.typography.bodySmall)
            OutputNumber(stringResource(R.string.output_power_moderate), powerModerate, false) { edited = true; powerModerate = it }
            OutputNumber(stringResource(R.string.output_power_vigorous), powerVigorous, false) { edited = true; powerVigorous = it }
            OutputNumber(stringResource(R.string.output_power_peak), powerPeak, false) { edited = true; powerPeak = it }
            Text(stringResource(R.string.ui_split_time), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.output_pace_help), style = MaterialTheme.typography.bodySmall)
            OutputNumber(stringResource(R.string.output_pace_moderate), paceModerate, true) { edited = true; paceModerate = it }
            OutputNumber(stringResource(R.string.output_pace_vigorous), paceVigorous, true) { edited = true; paceVigorous = it }
            OutputNumber(stringResource(R.string.output_pace_peak), pacePeak, true) { edited = true; pacePeak = it }
            if (profile == null) Text(stringResource(R.string.output_zones_invalid), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.hr_frozen_help), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { onSave(profile) }, enabled = profile != null, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.hr_save)) }
            if (initial != null) OutlinedButton(onClick = { onSave(null) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.hr_disable)) }
        }
    }
}

@Composable private fun OutputNumber(label: String, value: String, pace: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() || pace && c == ':' }) onChange(it) },
        label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if (pace) KeyboardType.Ascii else KeyboardType.Number))
}
