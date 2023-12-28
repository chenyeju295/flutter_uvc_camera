package com.sdk.uvc_camera;

public interface PictureSaverCallback {
    // 回调接口
    public static final int MESSAGE_STOREIMAGE_ERR = 1;
    public static final int MESSAGE_STOREIMAGE_SUCCESS = 2;

    public void onMessage(int msg, PictureSaverInfo info);
}
