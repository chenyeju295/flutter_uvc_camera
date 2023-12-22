package com.jiangdg.usbvideo;

public interface IPictureCallback {
    void onPictureFrame(byte [] pbuf, int size);
}
