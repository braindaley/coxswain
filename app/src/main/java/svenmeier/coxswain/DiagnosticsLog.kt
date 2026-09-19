package svenmeier.coxswain

import android.content.Context
import androidx.core.content.edit
import java.text.DateFormat
import java.util.Date

object DiagnosticsLog {
    private const val FILE = "diagnostics"
    private const val MESSAGE = "last_message"
    private const val TIME = "last_time"

    @JvmStatic fun record(context: Context, message: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(MESSAGE, message)
            putLong(TIME, System.currentTimeMillis())
        }
    }

    @JvmStatic fun latest(context: Context): String {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val message = preferences.getString(MESSAGE, null) ?: return "No recent issues recorded"
        val time = preferences.getLong(TIME, 0L)
        return "$message · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))}"
    }
}
