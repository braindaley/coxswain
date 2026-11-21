package svenmeier.coxswain

import android.content.Context
import androidx.core.content.edit
import java.text.DateFormat
import java.util.Date

object DiagnosticsLog {
    private const val FILE = "diagnostics"
    private const val MESSAGE = "last_message"
    private const val TIME = "last_time"
    private const val HEART_MESSAGE = "heart_message"
    private const val HEART_TIME = "heart_time"

    @JvmStatic fun recordHeart(context: Context, message: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(HEART_MESSAGE, message)
            putLong(HEART_TIME, System.currentTimeMillis())
        }
    }

    @JvmStatic fun record(context: Context, message: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
            putString(MESSAGE, message)
            putLong(TIME, System.currentTimeMillis())
        }
    }

    @JvmStatic fun latest(context: Context): String {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return listOf(MESSAGE to TIME, HEART_MESSAGE to HEART_TIME).mapNotNull { (key, timeKey) ->
            preferences.getString(key, null)?.let { message ->
                val time = preferences.getLong(timeKey, 0L)
                "$message · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))}"
            }
        }.joinToString("\n").ifBlank { "No recent issues recorded" }
    }
}
