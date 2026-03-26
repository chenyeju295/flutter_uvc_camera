package com.chenyeju

import android.os.Handler
import android.os.Looper

/**
 * 录制计时器管理类
 *
 * Uses Handler.postDelayed at 1-second intervals (formatted time has second-level precision).
 */
class RecordingTimerManager(private val videoStreamHandler: VideoStreamHandler) {
    private var recordingStartTime: Long = 0
    private var running = false
    private val handler = Handler(Looper.getMainLooper())

    private val tick: Runnable = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = System.currentTimeMillis() - recordingStartTime
            sendRecordingUpdate(elapsed)
            handler.postDelayed(this, 1000L)
        }
    }

    fun startRecording() {
        stopRecording()
        recordingStartTime = System.currentTimeMillis()
        running = true
        sendRecordingUpdate(0)
        handler.postDelayed(tick, 1000L)
    }

    fun stopRecording() {
        if (!running) return
        running = false
        handler.removeCallbacks(tick)
        val finalTime = System.currentTimeMillis() - recordingStartTime
        sendRecordingUpdate(finalTime, isFinal = true)
    }

    private fun sendRecordingUpdate(elapsedMillis: Long, isFinal: Boolean = false) {
        val seconds = elapsedMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val formattedTime = String.format(
            "%02d:%02d:%02d",
            hours % 24,
            minutes % 60,
            seconds % 60
        )

        val data = mapOf(
            "elapsedMillis" to elapsedMillis,
            "formattedTime" to formattedTime,
            "isFinal" to isFinal
        )

        videoStreamHandler.sendState("RECORDING_TIME", data)
    }

    fun release() {
        running = false
        handler.removeCallbacks(tick)
    }
}
