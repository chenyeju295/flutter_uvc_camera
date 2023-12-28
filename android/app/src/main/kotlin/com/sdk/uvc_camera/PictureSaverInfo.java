package com.sdk.uvc_camera;

public class PictureSaverInfo {
    public byte [] data;
    public int size;           // 数据长度
    public int index;          // 编号
    public int w;              // 宽
    public int h;              // 长
    public long dateTaken;     // 日期
    public String title;       // 文件名,不带扩展名
    public String name;        // 文件名,带扩展名
    public String pathname;    // 文件路径及文件名

    public PictureSaverInfo() {
        data = null;
        size = 0;
        w = 0;
        h = 0;
        dateTaken = 0;
        title = "";
        name = "";
        pathname = "";
    }
}
