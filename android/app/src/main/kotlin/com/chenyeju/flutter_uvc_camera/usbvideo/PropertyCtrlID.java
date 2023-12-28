package com.chenyeju.flutter_uvc_camera.usbvideo;

public class PropertyCtrlID {
    public static final long PU_BRIGHTNESS = 0x000001;	    // D0: Brightness
    public static final long PU_CONTRAST = 0x000002;	    // D1: Contrast
    public static final long PU_HUE = 0x000004;	            // D2: Hue
    public static final long PU_SATURATION = 0x000008;	    // D3: Saturation
    public static final long PU_SHARPNESS = 0x000010;	    // D4: Sharpness
    public static final long PU_GAMMA = 0x000020;	        // D5: Gamma
    public static final long PU_WB_TEMP = 0x000040;	        // D6: White Balance Temperature
    public static final long PU_BACKLIGHT = 0x000100;	    // D8: Backlight Compensation
    public static final long PU_GAIN = 0x000200;	        // D9: Gain
    public static final long PU_POWER_LF = 0x000400;	    // D10: Power Line Frequency
    public static final long PU_WB_TEMP_AUTO = 0x001000;	// D12: White Balance Temperature, Auto


    public static final long IT_AE = 0x000002;	            // D1:  Auto-Exposure Mode
    public static final long IT_AE_PRIORITY = 0x000004;	    // D2:  Auto-Exposure Priority
    public static final long IT_AE_ABS = 0x000008;	        // D3:  Exposure Time (Absolute)
    public static final long IT_FOCUS_ABS = 0x000020;	    // D5:  Focus (Absolute)
    public static final long IT_FOCUS_AUTO = 0x020000;	    // D17: Focus, Auto

}
