#/*
# * UVCCamera
# * library and sample to access to UVC web camera on non-rooted Android device
# * 
# * Copyright (c) 2015-2017 saki t_saki@serenegiant.com
# * 
# * File name: Android.mk
# * 
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# *  You may obtain a copy of the License at
# * 
# *     http://www.apache.org/licenses/LICENSE-2.0
# * 
# *  Unless required by applicable law or agreed to in writing, software
# *  distributed under the License is distributed on an "AS IS" BASIS,
# *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# *  See the License for the specific language governing permissions and
# *  limitations under the License.
# * 
# * All files in the folder are under this Apache License, Version 2.0.
# * Files in the jni/libjpeg-turbo, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
#*/
######################################################################
# jpeg-turbo1500.so
######################################################################
LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := jpeg-turbo1500

LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/ \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/simd \

LOCAL_EXPORT_C_INCLUDES := \
		$(LOCAL_PATH)/ \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/simd \

LOCAL_CFLAGS := $(LOCAL_C_INCLUDES:%=-I%)
LOCAL_CFLAGS += -DANDROID_NDK

LOCAL_ARM_MODE := arm

LOCAL_ASMFLAGS += -DELF -DPIC

LOCAL_SRC_FILES := \
	jcapimin.c \
	jcapistd.c \
	jccoefct.c \
	jccolor.c \
	jcdctmgr.c \
	jchuff.c \
	jcinit.c \
	jcmainct.c \
	jcmarker.c \
	jcmaster.c \
	jcomapi.c \
	jcparam.c \
	jcphuff.c \
	jcprepct.c \
	jcsample.c \
	jctrans.c \
	jdapimin.c \
	jdapistd.c \
	jdatadst.c \
	jdatasrc.c \
	jdcoefct.c \
	jdcolor.c \
	jddctmgr.c \
	jdhuff.c \
	jdinput.c \
	jdmainct.c \
	jdmarker.c \
	jdmaster.c \
	jdmerge.c \
	jdphuff.c \
	jdpostct.c \
	jdsample.c \
	jdtrans.c \
	jerror.c \
	jfdctflt.c \
	jfdctfst.c \
	jfdctint.c \
	jidctflt.c \
	jidctfst.c \
	jidctint.c \
	jidctred.c \
	jquant1.c \
	jquant2.c \
	jutils.c \
	jmemmgr.c \
	jmemnobs.c \
	jaricom.c \
	jcarith.c \
	jdarith.c \
	turbojpeg.c \
	transupp.c \
	jdatadst-tj.c \
	jdatasrc-tj.c \

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
#LOCAL_ARM_NEON := true
LOCAL_SRC_FILES += simd/jsimd_arm.c simd/jsimd_arm_neon.S
LOCAL_CFLAGS += -DSIZEOF_SIZE_T=4

else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
#LOCAL_ARM_NEON := true
LOCAL_SRC_FILES += simd/jsimd_arm64.c simd/jsimd_arm64_neon.S
LOCAL_CFLAGS += -DSIZEOF_SIZE_T=8

else ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_SRC_FILES += \
	simd/jsimd_x86_64.c \
	simd/jfdctflt-sse-64.asm \
	simd/jccolor-sse2-64.asm \
	simd/jcgray-sse2-64.asm \
	simd/jcsample-sse2-64.asm \
	simd/jdcolor-sse2-64.asm \
	simd/jdmerge-sse2-64.asm \
	simd/jdsample-sse2-64.asm \
	simd/jfdctfst-sse2-64.asm \
	simd/jfdctint-sse2-64.asm \
	simd/jidctflt-sse2-64.asm \
	simd/jidctfst-sse2-64.asm \
	simd/jidctint-sse2-64.asm \
	simd/jidctred-sse2-64.asm \
	simd/jquantf-sse2-64.asm \
	simd/jquanti-sse2-64.asm \
	simd/jchuff-sse2-64.asm \

LOCAL_CFLAGS += -DSIZEOF_SIZE_T=8
LOCAL_ASMFLAGS += -D__x86_64__

else
LOCAL_SRC_FILES += jsimd_none.c
endif

LOCAL_CPPFLAGS += -Wno-incompatible-pointer-types
LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -ldl

include $(BUILD_SHARED_LIBRARY)