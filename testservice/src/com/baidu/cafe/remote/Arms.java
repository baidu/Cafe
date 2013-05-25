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

package com.baidu.cafe.remote;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.baidu.cafe.utils.ReflectHelper;

/**
 * This class provides autotest assistance by AIDL+Service, including the
 * following features: 1. file system 2. power 3. connectivity 4. telephony
 * 5.storage 6. system 7. appbasic 8. media
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-06-20
 * @version
 * @todo
 */
public class Arms extends Service {
    public Arms() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.print("service bind!");
        return new ArmsBinder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        invokeArmsBinder(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // close for android.provider.Settings$SettingNotFoundException: adb_enabled
        //        keepAdb();
    }

    /* 
     * HOW TO CALL CAFE'S API FROM COMMAND LINE
     * 
     * adb shell am startservice -a com.baidu.cafe.remote.action.name.COMMAND 
     * -e function "waitforTopActivity" -e parameter "String:com.baidu.calculator2.Calculator,long:5000" 
     * > /dev/null;adb logcat -d | grep Arms
     */
    @Override
    public void onStart(Intent intent, int startId) {
        //        invokeArmsBinder(intent);
    }

    private void invokeArmsBinder(final Intent intent) {
        if (null == intent) {
            Log.print("null == intent at invokeArmsBinder");
            return;
        }

        // It must be invoked in thread, because some function use socket.
        final ArmsBinder armsBinder = new ArmsBinder(this);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String function = intent.getStringExtra("function");
                    String parameter = intent.getStringExtra("parameter");
                    long begin = System.currentTimeMillis();
                    Object result = ReflectHelper.invoke(armsBinder, function, parameter);

                    Log.print(null == result ? "" : result.toString());
                    Log.print(String.format("invoke completed [%s] timecost [%ss]", function,
                            (System.currentTimeMillis() - begin) / 1000f));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "invokeArmsBinder").start();
    }

}
