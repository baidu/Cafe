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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.R.integer;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

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
    public void onCreate() {
        super.onCreate();

        // close for android.provider.Settings$SettingNotFoundException: adb_enabled
        //        keepAdb();
    }

    /* 
     * adb shell am startservice -a com.baidu.cafe.remote.action.name.COMMAND -e cmd "i did it"
     */
    @Override
    public void onStart(Intent intent, int startId) {
        invokeArmsBinder(intent.getStringExtra("function"), intent.getStringExtra("parameter"));
    }

    class Parameter {
        public Class  type;
        public Object value;
    }

    private void invokeArmsBinder(String function, String parameter) {
        Log.print(function + "(" + parameter + ")");

        // get parameter
        String[] parameters = parameter.split(",");
        Class[] types = new Class[parameters.length];
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = getParameter(parameters[i]);
            types[i] = p.type;
            values[i] = p.value;
        }

        try {
            Method method = ArmsBinder.class.getDeclaredMethod(function, types);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            Object result = method.invoke(new ArmsBinder(this), values);
            Log.print(result.toString());
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Parameter getParameter(String parameterString) {
        Parameter p = new Parameter();
        String type = parameterString.substring(0, parameterString.indexOf(":"));
        String value = parameterString.substring(parameterString.indexOf(":") + 1, parameterString.length());

        if ("String".equalsIgnoreCase(type)) {
            p.type = String.class;
            p.value = String.valueOf(value);
        } else if ("int".equalsIgnoreCase(type)) {
            p.type = int.class;
            p.value = Integer.valueOf(value).intValue();
        } else if ("boolean".equalsIgnoreCase(type)) {
            p.type = boolean.class;
            p.value = Boolean.valueOf(value).booleanValue();
        } else if ("float".equalsIgnoreCase(type)) {
            p.type = float.class;
            p.value = Float.valueOf(value).floatValue();
        } else if ("double".equalsIgnoreCase(type)) {
            p.type = double.class;
            p.value = Double.valueOf(value).doubleValue();
        }
        return p;
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
