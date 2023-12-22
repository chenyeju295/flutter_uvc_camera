package com.jiangdg.usbvideo;

import java.nio.ByteBuffer;

public interface IPreviewCallback {
    void onPreviewFrame(final ByteBuffer buf);
}