package com.chenyeju

import android.os.Handler
import android.os.Looper
import java.util.Timer
import java.util.TimerTask

/**
 * 录制计时器管理类
 */
class RecordingTimerManager(private val videoStreamHandler: VideoStreamHandler) {
    private var timer: Timer? = null
    private var recordingStartTime: Long = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * 开始录制计时
     */
    fun startRecording() {
        stopTimer()
        
        recordingStartTime = System.currentTimeMillis()
        timer = Timer()
        
        // 发送初始状态
        sendRecordingUpdate(0)
        
        // 启动定时器，每100毫秒更新一次
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - recordingStartTime
                mainHandler.post {
                    sendRecordingUpdate(elapsedTime)
                }
            }
        }, 100, 100)
    }
    
    /**
     * 停止录制计时
     */
    fun stopRecording() {
        val finalTime = System.currentTimeMillis() - recordingStartTime
        sendRecordingUpdate(finalTime, true)
        stopTimer()
    }
    
    /**
     * 发送录制状态更新
     */
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
    
    /**
     * 停止计时器
     */
    private fun stopTimer() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopTimer()
    }
} 