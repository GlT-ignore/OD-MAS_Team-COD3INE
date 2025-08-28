package com.example.odmas.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogFileLogger {
    @Volatile private var initialized = false
    private lateinit var appContext: Context
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private const val LOG_FILE_NAME = "odmas.log"
    private const val LOG_FILE_ROTATE = "odmas.log.1"
    private const val MAX_BYTES: Long = 512 * 1024

    fun init(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    appContext = context.applicationContext
                    initialized = true
                    log("LogFileLogger", "Initialized file logger")
                }
            }
        }
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, message, throwable) else Log.d(tag, message)
        if (!initialized) return
        try {
            val dir: File = appContext.filesDir
            if (!dir.exists()) dir.mkdirs()
            val logFile = File(dir, LOG_FILE_NAME)
            if (logFile.exists() && logFile.length() >= MAX_BYTES) {
                val rotated = File(dir, LOG_FILE_ROTATE)
                runCatching {
                    if (rotated.exists()) rotated.delete()
                    logFile.copyTo(rotated, overwrite = true)
                    logFile.writeText("")
                }
            }
            val ts = dateFormat.format(Date())
            val sb = StringBuilder()
                .append(ts).append(" ").append(tag).append(": ").append(message).append('\n')
            if (throwable != null) sb.append(throwable.stackTraceToString()).append('\n')
            logFile.appendText(sb.toString())
        } catch (_: Throwable) { }
    }
}


