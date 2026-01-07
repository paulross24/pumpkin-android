package uk.co.rosshome.pumpkin

import android.content.Context
import android.content.SharedPreferences

class CrashReportStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("crash_reports", Context.MODE_PRIVATE)

    fun save(reportJson: String) {
        prefs.edit().putString(KEY_LAST_REPORT, reportJson).apply()
    }

    fun load(): String? {
        return prefs.getString(KEY_LAST_REPORT, null)
    }

    fun clear() {
        prefs.edit().remove(KEY_LAST_REPORT).apply()
    }

    companion object {
        private const val KEY_LAST_REPORT = "last_report"
    }
}
