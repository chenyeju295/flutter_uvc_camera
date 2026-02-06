LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := nativelib

# Lame sources
LAME_PATH := module/mp3/lame
LOCAL_SRC_FILES := \
    $(LAME_PATH)/bitstream.c \
    $(LAME_PATH)/encoder.c \
    $(LAME_PATH)/fft.c \
    $(LAME_PATH)/gain_analysis.c \
    $(LAME_PATH)/id3tag.c \
    $(LAME_PATH)/lame.c \
    $(LAME_PATH)/mpglib_interface.c \
    $(LAME_PATH)/newmdct.c \
    $(LAME_PATH)/presets.c \
    $(LAME_PATH)/psymodel.c \
    $(LAME_PATH)/quantize.c \
    $(LAME_PATH)/quantize_pvt.c \
    $(LAME_PATH)/reservoir.c \
    $(LAME_PATH)/set_get.c \
    $(LAME_PATH)/tables.c \
    $(LAME_PATH)/takehiro.c \
    $(LAME_PATH)/util.c \
    $(LAME_PATH)/vbrquantize.c \
    $(LAME_PATH)/VbrTag.c \
    $(LAME_PATH)/version.c

# Other sources
LOCAL_SRC_FILES += \
    utils/logger.cpp \
    module/yuv/yuv.cpp \
    module/mp3/mp3.cpp \
    proxy/proxy_yuv.cpp \
    proxy/proxy_mp3.cpp \
    nativelib.cpp

LOCAL_C_INCLUDES := $(LOCAL_PATH)/module/mp3/lame

LOCAL_CFLAGS += -DSTDC_HEADERS

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)