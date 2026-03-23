package com.chenyeju

import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import java.nio.ByteBuffer
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

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

    /**
     * Backpressure: avoid posting every frame to the main thread.
     * Keep only one in-flight send slot for video/audio, drop extra frames early.
     */
    private val pendingVideoSend = AtomicBoolean(false)
    private val pendingAudioSend = AtomicBoolean(false)

    /**
     * State queue for cases where native emits events before Dart subscribes.
     * We keep it bounded to avoid memory leaks.
     */
    private val pendingLock = Any()
    private val pendingStates = ArrayDeque<Pair<String, Map<String, Any>?>>()
    private val maxPendingStates = 64

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
        pendingVideoSend.set(false)
        pendingAudioSend.set(false)
        frameCounter = 0
        lastFpsUpdateTime = System.currentTimeMillis()
        frameTimes.clear()
        totalVideoFrames = 0L
        totalAudioFrames = 0L
        droppedVideoFrames = 0L
        droppedAudioFrames = 0L
        mainHandler.removeCallbacks(statsTicker)
        if (events != null) {
            mainHandler.post {
                flushPendingStates(events)
            }
        }
        mainHandler.postDelayed(statsTicker, 1000L)
    }
    
    override fun onCancel(arguments: Any?) {
        mainHandler.removeCallbacks(statsTicker)
        eventSink = null
        pendingVideoSend.set(false)
        pendingAudioSend.set(false)
        synchronized(pendingLock) {
            pendingStates.clear()
        }
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

        // Backpressure drop: if a send is already pending on main thread, drop current frame early.
        if (isVideoFrame) {
            if (pendingVideoSend.getAndSet(true)) {
                droppedVideoFrames++
                return
            }
        } else {
            if (pendingAudioSend.getAndSet(true)) {
                droppedAudioFrames++
                return
            }
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
            // If we don't post an event, we must release the backpressure slot now.
            if (isVideoFrame) {
                pendingVideoSend.set(false)
            } else {
                pendingAudioSend.set(false)
            }
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
            } finally {
                // Release the backpressure slot after posting is processed.
                if (isVideoFrame) {
                    pendingVideoSend.set(false)
                } else {
                    pendingAudioSend.set(false)
                }
            }
        }
    }
    
    /**
     * 发送状态更新
     */
    fun sendState(state: String, data: Map<String, Any>? = null) {
        val sink = eventSink
        if (sink == null) {
            enqueuePendingState(state, data)
            return
        }
        mainHandler.post {
            deliverStateToSink(sink, state, data)
        }
    }

    private fun enqueuePendingState(state: String, data: Map<String, Any>?) {
        synchronized(pendingLock) {
            while (pendingStates.size >= maxPendingStates) {
                pendingStates.removeFirst()
            }
            pendingStates.addLast(state to data)
        }
    }

    private fun flushPendingStates(sink: EventSink) {
        val batch: List<Pair<String, Map<String, Any>?>> = synchronized(pendingLock) {
            val list = pendingStates.toList()
            pendingStates.clear()
            list
        }
        for ((state, data) in batch) {
            deliverStateToSink(sink, state, data)
        }
    }

    private fun deliverStateToSink(sink: EventSink, state: String, data: Map<String, Any>?) {
        try {
            val event = HashMap<String, Any>()
            event["type"] = "STATE"
            event["state"] = state
            if (data != null) {
                event.putAll(data)
            }
            sink.success(event)
        } catch (_: Exception) {
            // Ignore; sink can be invalid after cancellation.
        }
    }
} 