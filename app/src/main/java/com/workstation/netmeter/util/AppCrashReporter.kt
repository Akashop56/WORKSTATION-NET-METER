package com.workstation.netmeter.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures the last failure the app ran into.
 *
 * A foreground-service start can be rejected by the platform (missing type
 * permission, background-start restriction, an unexpected notification). Those
 * failures are otherwise invisible: the process dies and the user only sees the
 * system "app keeps stopping" dialog. Recording the stack trace both in logcat
 * and in a small file means the Settings screen can show what happened, so a
 * failure can be diagnosed without a computer.
 */
object AppCrashReporter {

    private const val TAG = "NetMeter"
    private const val FILE_NAME = "last_error.txt"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var installed = false

    /**
     * Routes uncaught thread/coroutine failures through [record] before
     * letting the previous handler (and the platform) do their usual work.
     */
    @Synchronized
    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        appContext = app
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(throwable, "uncaught on ${thread.name}") }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Stores [throwable] in logcat and in [FILE_NAME]; never throws. */
    fun record(throwable: Throwable, where: String) {
        val text = format(throwable, where)
        Log.e(TAG, text)
        val app = appContext ?: return
        runCatching {
            val file = File(app.filesDir, FILE_NAME)
            file.writeText(text)
        }
    }

    /** Returns the recorded failure, or null when nothing has been recorded. */
    fun lastError(context: Context): String? = runCatching {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        if (file.exists()) file.readText().takeIf { it.isNotBlank() } else null
    }.getOrNull()

    /** Clears the recorded failure. */
    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
    }

    private fun format(throwable: Throwable, where: String): String {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date())
        val writer = StringWriter()
        writer.append("Workstation Net Meter - last error\n")
        writer.append("time: ").append(stamp).append('\n')
        writer.append("where: ").append(where).append("\n\n")
        throwable.printStackTrace(PrintWriter(writer))
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 8) {
            writer.append("\nCaused by: ").append(cause.toString()).append('\n')
            cause.printStackTrace(PrintWriter(writer))
            cause = cause.cause
            depth++
        }
        return writer.toString()
    }
}
