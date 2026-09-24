package svenmeier.coxswain

import androidx.preference.PreferenceManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioPreferenceDefaultsTest {
    @Test
    fun soundsAndSpokenCuesDefaultToOffWithoutOverwritingAnExplicitChoice() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val previousValues = preferences.all.toMap()
        val keys = listOf(
            R.string.preference_audio_ringtones,
            R.string.preference_audio_speak_segment,
            R.string.preference_audio_speak_limit
        ).map(context::getString)

        try {
            preferences.edit().clear().commit()
            PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
            keys.forEach { key -> assertFalse("Expected $key to default off", preferences.getBoolean(key, true)) }

            preferences.edit().putBoolean(keys.first(), true).commit()
            PreferenceManager.setDefaultValues(context, R.xml.preferences, true)
            assertTrue("An explicit user choice should be preserved", preferences.getBoolean(keys.first(), false))
        } finally {
            preferences.edit().clear().apply()
            val editor = preferences.edit()
            previousValues.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.apply()
        }
    }
}
