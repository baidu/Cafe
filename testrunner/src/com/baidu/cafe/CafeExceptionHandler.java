package com.baidu.cafe;

import android.util.Log;

public class CafeExceptionHandler implements Thread.UncaughtExceptionHandler {
    public interface ExceptionCallBack {
        public void callWhenExceptionHappen();
    }

    private final static String             TAG      = "MyAndroidRuntime";

    private ExceptionCallBack               mycafe   = null;
    private Thread.UncaughtExceptionHandler morignal = null;

    public CafeExceptionHandler(Thread.UncaughtExceptionHandler orignal, ExceptionCallBack mycafe) {
        this.morignal = orignal;
        this.mycafe = mycafe;
    }

    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        Log.i(TAG, "this is in Cafe exceptionhandler");
        this.mycafe.callWhenExceptionHappen();
        this.morignal.uncaughtException(arg0, arg1);
    }
}
