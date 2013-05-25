/*
 * Copyright (C) 2011 Baidu.com Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        arg1.printStackTrace();
        this.mycafe.callWhenExceptionHappen();
        this.morignal.uncaughtException(arg0, arg1);
    }
}
