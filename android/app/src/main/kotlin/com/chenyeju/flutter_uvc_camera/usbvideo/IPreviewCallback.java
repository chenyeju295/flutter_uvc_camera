package com.chenyeju.flutter_uvc_camera.usbvideo;

import java.nio.ByteBuffer;

public interface IPreviewCallback {
    void onPreviewFrame(final ByteBuffer buf);
}