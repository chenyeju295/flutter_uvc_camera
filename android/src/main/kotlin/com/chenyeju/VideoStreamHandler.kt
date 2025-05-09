package com.chenyeju

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
    private var lastFrameTime = 0L
    
    override fun onListen(arguments: Any?, events: EventSink?) {
        eventSink = events
        frameCounter = 0
        lastFpsUpdateTime = System.currentTimeMillis()
    }
    
    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
    
    /**
     * 处理视频帧数据
     */
    fun onVideoFrame(type: String, buffer: ByteBuffer, offset: Int, size: Int, timestamp: Long) {
        val sink = eventSink ?: return
        
        // 帧率控制
        val currentTime = System.currentTimeMillis()
        if (frameRateLimit > 0) {
            val minInterval = 1000 / frameRateLimit
            if (currentTime - lastFrameTime < minInterval) {
                return  // 跳过此帧
            }
            lastFrameTime = currentTime
        }
        
        // 大小控制
        if (frameSizeLimit > 0 && size > frameSizeLimit) {
            return  // 跳过大帧
        }
        
        // 计算FPS
        frameCounter++
        if (currentTime - lastFpsUpdateTime >= 1000) {
            currentFps = frameCounter
            frameCounter = 0
            lastFpsUpdateTime = currentTime
        }
        
        try {
            // 创建数据副本以避免并发问题
            val data = ByteArray(size)
            buffer.get(data, offset, size)
            
            val event = HashMap<String, Any>()
            event["type"] = type
            event["data"] = data
            event["timestamp"] = timestamp
            event["size"] = size
            event["fps"] = currentFps
            
            sink.success(event)
        } catch (e: Exception) {
            sink.error("VIDEO_STREAM_ERROR", "Error processing video frame: ${e.message}", null)
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
        
        sink.success(event)
    }
} 