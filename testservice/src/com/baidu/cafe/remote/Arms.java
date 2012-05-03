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
import android.app.Instrumentation;
import android.content.Intent;
import android.os.IBinder;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

/**
 * This class provides autotest assistance by AIDL+Service, including the
 * following features: 
 * 1. file system 
 * 2. power 
 * 3. connectivity 
 * 4. telephony 
 * 5.storage 
 * 6. system 
 * 7. appbasic 
 * 8. media
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
    public void onCreate() {
        super.onCreate();

        keepAdb();
    }

    private void keepAdb() {
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        int adbEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ADB_ENABLED);
                        if (adbEnabled == 0) {
                            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ADB_ENABLED, 1);
                            Log.print("resume adb!");
                        }
                        Thread.sleep(1000);
                    } catch (SettingNotFoundException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
