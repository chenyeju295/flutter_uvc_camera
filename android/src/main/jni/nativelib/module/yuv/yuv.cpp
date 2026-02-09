/**
 *  yuv handle
 *
 *  NV21：YYYYYYYY VUVU
 *  YV12：YYYYYYYY VV UU
 *  YUV420sp：YYYYYYYY UVUV
 *  YUV420p：YYYYYYYY UU VV
 *
 * @author Created by jiangdg on 2022/2/18
 */

#include "yuv.h"

void *yuv420spToNv21Internal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + 2*i+1] = srcData[yLength + 2 * i];
        destData[yLength + 2*i] = srcData[yLength + 2*i+1];
    }
    return nullptr;
}

void *nv21ToYuv420spInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + 2 * i] = srcData[yLength + 2*i+1];
        destData[yLength + 2*i+1] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420spWithMirrorInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    // Mirror Y
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            destData[j * width + (width - 1 - i)] = srcData[j * width + i];
        }
    }
    // Mirror UV and Swap VU -> UV
    int uvStart = yLength;
    for (int j = 0; j < height / 2; j++) {
        for (int i = 0; i < width; i += 2) {
            // Source V, U
            char v = srcData[uvStart + j * width + i];
            char u = srcData[uvStart + j * width + i + 1];

            // Dest index: mirrored
            // If i=0 (left), dest is width-2 (right).
            int destIdx = uvStart + j * width + (width - 2 - i);

            // Store as UV (Swap)
            destData[destIdx] = u;
            destData[destIdx + 1] = v;
        }
    }
    return nullptr;
}

void *nv21ToYuv420pInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + i] = srcData[yLength + 2*i + 1];
        destData[yLength + uLength + i] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420pWithMirrorInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    int uStart = yLength;
    int vStart = yLength + uLength;

    // Mirror Y
    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {
            destData[j * width + (width - 1 - i)] = srcData[j * width + i];
        }
    }

    // Mirror and Split UV
    for (int j = 0; j < height / 2; j++) {
        for (int i = 0; i < width; i += 2) {
            // Source V, U at src[yLength + j*width + i]
            char v = srcData[yLength + j * width + i];
            char u = srcData[yLength + j * width + i + 1];

            // Dest index in planar arrays
            // i goes 0, 2, 4... width-2.
            // planar index k goes 0, 1, 2... width/2 - 1.
            // k = i / 2.
            // Mirrored k' = (width/2 - 1 - k).
            int k = i / 2;
            int k_mirror = (width / 2) - 1 - k;

            // Dest U
            destData[uStart + j * (width / 2) + k_mirror] = u;
            // Dest V
            destData[vStart + j * (width / 2) + k_mirror] = v;
        }
    }
    return nullptr;
}
