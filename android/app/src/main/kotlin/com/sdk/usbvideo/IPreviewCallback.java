package com.sdk.usbvideo;

import java.nio.ByteBuffer;

public interface IPreviewCallback {
    void onPreviewFrame(final ByteBuffer buf);
}