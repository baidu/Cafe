LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := android-web-driver

LOCAL_SRC_FILES := $(call all-subdir-java-files,../testrunner/src/)
LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := webapplib

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := webapplib:third/guava-10.0.1.jar

include $(BUILD_MULTI_PREBUILT) 
