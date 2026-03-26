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
    private var videoSampleEveryN = 1
    private var videoSampleCounter = 0
    @Volatile var videoKeyframesOnly: Boolean = false

    /**
     * Whether to push raw frame bytes to Dart.
     *
     * Default is true for backward compatibility, but for most apps it is strongly recommended
     * to disable these and consume frames on the native side (decode/render/stream) instead.
     */
    @Volatile var enableVideoFrames: Boolean = true
    @Volatile var enableAudioFrames: Boolean = true
    
    // 更准确的FPS计算
    private val fpsCalculationWindow = 1000L // 1秒钟窗口
    private val frameTimes = ArrayDeque<Long>()

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

        // If raw bytes delivery is disabled, drop early (stats still update).
        if (isVideoFrame) {
            if (!enableVideoFrames) {
                droppedVideoFrames++
                return
            }
        } else {
            if (!enableAudioFrames) {
                droppedAudioFrames++
                return
            }
        }

        // Sampling/keyframe filters (video only).
        if (isVideoFrame) {
            // sample every N frames (N<=1 means no sampling)
            val n = videoSampleEveryN.coerceAtLeast(1)
            videoSampleCounter = (videoSampleCounter + 1) % n
            if (n > 1 && videoSampleCounter != 0) {
                droppedVideoFrames++
                return
            }
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
            frameTimes.addLast(currentTime)

            // 移除窗口外的时间戳
            while (frameTimes.isNotEmpty() && frameTimes.first() < currentTime - fpsCalculationWindow) {
                frameTimes.removeFirst()
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

        // Only scan for keyframes when the caller actually needs the information.
        val needKeyFrameInfo = isVideoFrame && videoKeyframesOnly
        val isKeyFrame = if (needKeyFrameInfo) isH264KeyFrame(dataCopy) else false

        if (needKeyFrameInfo && !isKeyFrame) {
            droppedVideoFrames++
            pendingVideoSend.set(false)
            return
        }
        
        // Build event map on the calling thread (cheap), post once to main thread.
        val event = HashMap<String, Any>(7)
        event["type"] = type
        event["data"] = dataCopy
        event["timestamp"] = timestamp
        event["size"] = size
        event["fps"] = fpsForEvent
        event["isKeyFrame"] = isKeyFrame
        mainHandler.post {
            try {
                val currentSink = eventSink
                if (currentSink != null) {
                    currentSink.success(event)
                }
            } catch (e: Exception) {
                // Sink may have been cancelled between post and execution; safe to ignore.
            } finally {
                if (isVideoFrame) {
                    pendingVideoSend.set(false)
                } else {
                    pendingAudioSend.set(false)
                }
            }
        }
    }

    fun startStatsTicker() {
        mainHandler.removeCallbacks(statsTicker)
        totalVideoFrames = 0L
        totalAudioFrames = 0L
        droppedVideoFrames = 0L
        droppedAudioFrames = 0L
        mainHandler.postDelayed(statsTicker, 1000L)
    }

    fun stopStatsTicker() {
        mainHandler.removeCallbacks(statsTicker)
    }

    fun setVideoSampleEveryN(n: Int) {
        videoSampleEveryN = n.coerceAtLeast(1)
        videoSampleCounter = 0
    }

    fun getVideoSampleEveryN(): Int = videoSampleEveryN

    private fun isH264KeyFrame(data: ByteArray): Boolean {
        // Best-effort scan: return true if any NAL unit type == 5 (IDR).
        // Supports both Annex-B (0x000001/0x00000001) and length-prefixed (AVCC) in a simple way.
        return containsH264NalType(data, 5)
    }

    private fun containsH264NalType(data: ByteArray, targetType: Int): Boolean {
        if (data.isEmpty()) return false

        // Annex-B start code scan
        var i = 0
        var foundStartCode = false
        while (i + 3 < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            val b2 = data[i + 2].toInt() and 0xFF
            if (b0 == 0 && b1 == 0 && b2 == 1) {
                foundStartCode = true
                val nalHeaderIndex = i + 3
                if (nalHeaderIndex < data.size) {
                    val nalType = (data[nalHeaderIndex].toInt() and 0x1F)
                    if (nalType == targetType) return true
                }
                i = nalHeaderIndex
                continue
            }
            if (i + 4 < data.size && b0 == 0 && b1 == 0 && b2 == 0 && (data[i + 3].toInt() and 0xFF) == 1) {
                foundStartCode = true
                val nalHeaderIndex = i + 4
                if (nalHeaderIndex < data.size) {
                    val nalType = (data[nalHeaderIndex].toInt() and 0x1F)
                    if (nalType == targetType) return true
                }
                i = nalHeaderIndex
                continue
            }
            i++
        }

        if (foundStartCode) return false

        // Try AVCC length-prefixed (4-byte length) scan for a few NALs
        var pos = 0
        var scanned = 0
        while (pos + 4 < data.size && scanned < 8) {
            val len =
                ((data[pos].toInt() and 0xFF) shl 24) or
                ((data[pos + 1].toInt() and 0xFF) shl 16) or
                ((data[pos + 2].toInt() and 0xFF) shl 8) or
                (data[pos + 3].toInt() and 0xFF)
            pos += 4
            if (len <= 0 || pos + len > data.size) break
            val nalType = (data[pos].toInt() and 0x1F)
            if (nalType == targetType) return true
            pos += len
            scanned++
        }
        return false
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