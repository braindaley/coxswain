package svenmeier.coxswain

import android.content.Context
import androidx.preference.Preference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import svenmeier.coxswain.view.SettingsFragment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsFunctionalityTest {
    @Test fun resetClearsCurrentLayoutAndStorageSwitchIsDisabled() {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        try {
            val activity = controller.get()
            activity.supportFragmentManager.executePendingTransactions()
            val fragment = activity.supportFragmentManager.findFragmentByTag("settings") as SettingsFragment
            val display = activity.getSharedPreferences("live_row_display", Context.MODE_PRIVATE)
            display.edit().putString("metrics", "custom").commit()
            val reset = fragment.findPreference<Preference>(activity.getString(R.string.preference_workout_bindings_reset))!!
            assertTrue(reset.onPreferenceClickListener!!.onPreferenceClick(reset))
            assertTrue(display.all.isEmpty())
            assertFalse(fragment.findPreference<Preference>(activity.getString(R.string.preference_data_external))!!.isEnabled)
            val retention = fragment.findPreference<Preference>(activity.getString(R.string.preference_compact))!!
            assertFalse(retention.callChangeListener("-1"))
            assertFalse(retention.callChangeListener("0"))
            assertTrue(retention.callChangeListener("180"))
            val weight = fragment.findPreference<Preference>(activity.getString(R.string.preference_weight))!!
            assertFalse(weight.callChangeListener("39"))
            assertTrue(weight.callChangeListener("68"))
        } finally { controller.pause().stop().destroy() }
    }
}
