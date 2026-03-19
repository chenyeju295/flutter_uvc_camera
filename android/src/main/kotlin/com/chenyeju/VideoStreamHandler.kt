package com.chenyeju

import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import java.nio.ByteBuffer

/**
 * 处理视频流数据的EventChannel处理类
 */
class VideoStreamHandler : EventChannel.StreamHandler {
    private var eventSink: EventSink? = null
    
    // 视频帧计数器，用于帧率控制
    private var frameCounter = 0
    private var lastFpsUpdateTime = 0L
    private var currentFps = 0
    
    // 控制参数
    var frameRateLimit = 30
    var frameSizeLimit = 0
    var audioFrameSizeLimit = 0
    private var lastFrameTime = 0L
    
    // 更准确的FPS计算
    private val fpsCalculationWindow = 1000L // 1秒钟窗口
    private val frameTimes = mutableListOf<Long>()

    private var totalVideoFrames = 0L
    private var totalAudioFrames = 0L
    private var droppedVideoFrames = 0L
    private var droppedAudioFrames = 0L

    private val statsTicker = object : Runnable {
        override fun run() {
            sendState(
                "STREAM_STATS",
                mapOf(
                    "totalVideoFrames" to totalVideoFrames,
                    "totalAudioFrames" to totalAudioFrames,
                    "droppedVideoFrames" to droppedVideoFrames,
                    "droppedAudioFrames" to droppedAudioFrames,
                    "videoFps" to currentFps
                )
            )
            mainHandler.postDelayed(this, 1000L)
        }
    }
    
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
        frameCounter = 0
        lastFpsUpdateTime = System.currentTimeMillis()
        frameTimes.clear()
        totalVideoFrames = 0L
        totalAudioFrames = 0L
        droppedVideoFrames = 0L
        droppedAudioFrames = 0L
        mainHandler.removeCallbacks(statsTicker)
        mainHandler.postDelayed(statsTicker, 1000L)
    }
    
    override fun onCancel(arguments: Any?) {
        mainHandler.removeCallbacks(statsTicker)
        eventSink = null
    }
    
    /**
     * 处理视频帧数据
     */
    fun onVideoFrame(type: String, buffer: ByteBuffer, offset: Int, size: Int, timestamp: Long) {
        val sink = eventSink ?: return
        val isVideoFrame = type.equals("H264", ignoreCase = true)
        if (isVideoFrame) {
            totalVideoFrames++
        } else {
            totalAudioFrames++
        }
        
        // 帧率控制
        val currentTime = System.currentTimeMillis()
        if (isVideoFrame && frameRateLimit > 0) {
            val minInterval = 1000 / frameRateLimit
            if (currentTime - lastFrameTime < minInterval) {
                droppedVideoFrames++
                return  // 跳过此帧
            }
            lastFrameTime = currentTime
        }
        
        // 大小控制
        if (isVideoFrame && frameSizeLimit > 0 && size > frameSizeLimit) {
            droppedVideoFrames++
            return  // 跳过大帧
        } else if (!isVideoFrame && audioFrameSizeLimit > 0 && size > audioFrameSizeLimit) {
            droppedAudioFrames++
            return
        }
        
        // 更精确的FPS计算仅用于视频帧，音频帧不参与
        val fpsForEvent = if (isVideoFrame) {
            frameTimes.add(currentTime)

            // 移除窗口外的时间戳
            while (frameTimes.isNotEmpty() && frameTimes.first() < currentTime - fpsCalculationWindow) {
                frameTimes.removeAt(0)
            }

            val calculatedFps = if (frameTimes.size > 1) {
                // 窗口内帧数 / 窗口时长 (秒)
                val framesInWindow = frameTimes.size
                val windowDuration = (currentTime - frameTimes.first()) / 1000.0
                (framesInWindow / windowDuration).toInt()
            } else {
                0
            }

            if (calculatedFps > 0) {
                currentFps = calculatedFps
            }
            currentFps
        } else {
            0
        }
        
        // 创建完整缓冲区副本，避免并发访问问题
        val dataCopy = try {
            // 保存当前位置
            val originalPosition = buffer.position()
            val originalLimit = buffer.limit()
            
            // 创建新缓冲区并复制数据
            val copy = ByteArray(size)
            buffer.position(offset)
            buffer.get(copy, 0, size)
            
            // 恢复缓冲区状态
            buffer.position(originalPosition)
            buffer.limit(originalLimit)
            
            copy
        } catch (e: Exception) {
            sink.error("VIDEO_STREAM_ERROR", "Error copying video buffer: ${e.message}", null)
            return
        }
        
        mainHandler.post {
            try {
                val event = HashMap<String, Any>()
                event["type"] = type
                event["data"] = dataCopy
                event["timestamp"] = timestamp
                event["size"] = size
                event["fps"] = fpsForEvent
                
                sink.success(event)
            } catch (e: Exception) {
                sink.error("VIDEO_STREAM_ERROR", "Error processing video frame: ${e.message}", null)
            }
        }
    }
    
    /**
     * 发送状态更新
     */
    fun sendState(state: String, data: Map<String, Any>? = null) {
        val sink = eventSink ?: return
        
        val event = HashMap<String, Any>()
        event["type"] = "STATE"
        event["state"] = state
        if (data != null) {
            event.putAll(data)
        }
        
        mainHandler.post {
            sink.success(event)
        }
    }
} 