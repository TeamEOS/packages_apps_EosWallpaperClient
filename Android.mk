LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES += $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := \
    wpm_async-http \
    wpm_support-v4-21 \
    wpm_image-loader \
    wpm_image-loader-sources

LOCAL_PACKAGE_NAME := EOSWallpaperClient
include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    wpm_async-http:libs/android-async-http-1.4.2.jar \
    wpm_support-v4-21:libs/android-support-v4-21.jar \
    wpm_image-loader:libs/universal-image-loader-1.7.0.jar \
    wpm_image-loader-sources:libs/universal-image-loader-1.7.0-sources.jar
include $(BUILD_MULTI_PREBUILT)
