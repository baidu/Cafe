LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := testcafe

LOCAL_SRC_FILES := $(call all-subdir-java-files)
LOCAL_SRC_FILES += \
        src/com/baidu/cafe/remote/IRemoteArms.aidl

LOCAL_STATIC_JAVA_LIBRARIES := lib3party

LOCAL_JAVA_LIBRARIES := android.test.runner

include $(BUILD_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := lib3party:third/robotium-solo-3.1.jar

include $(BUILD_MULTI_PREBUILT) 
