package uk.co.rosshome.pumpkin

import android.app.Application

class PumpkinApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val reporter = CrashReporter(this)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            reporter.reportCrash(throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
