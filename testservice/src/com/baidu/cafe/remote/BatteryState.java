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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/**
 * @author chengzhenyu@baidu.com
 * @date 2011-06-20
 */
class BatteryState {
    private String  mStatus;
    private String  mHealth;
    private boolean mPresent;
    private int     mLevel;
    private int     mScale;
    private int     mIcon_small;
    private String  mPlugged;
    private int     mVoltage;
    private int     mTemperature;
    private String  mTechnology;

    private Context mContext;

    public BatteryState(Context context) {
        mContext = context;
    }

    public void init() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void deinit() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    public String getStatus() {
        return mStatus;
    }

    public String getHealth() {
        return mHealth;
    }

    public boolean getPresent() {
        return mPresent;
    }

    public int getLevel() {
        return mLevel;
    }

    public int getScale() {
        return mScale;
    }

    public int getIcon_small() {
        return mIcon_small;
    }

    public String getPlugged() {
        return mPlugged;
    }

    public int getVoltage() {
        return mVoltage;
    }

    public int getTemperature() {
        return mTemperature;
    }

    public String getTechnology() {
        return mTechnology;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
                                                     @Override
                                                     public void onReceive(Context context,
                                                             Intent intent) {
                                                         String action = intent.getAction();
                                                         if (action
                                                                 .equals(Intent.ACTION_BATTERY_CHANGED)) {
                                                             int status = intent.getIntExtra(
                                                                     "status", 0);
                                                             int health = intent.getIntExtra(
                                                                     "health", 0);
                                                             mPresent = intent.getBooleanExtra(
                                                                     "present", false);
                                                             mLevel = intent
                                                                     .getIntExtra("level", 0);
                                                             mScale = intent
                                                                     .getIntExtra("scale", 0);
                                                             mIcon_small = intent.getIntExtra(
                                                                     "icon-small", 0);
                                                             int plugged = intent.getIntExtra(
                                                                     "plugged", 0);
                                                             mVoltage = intent.getIntExtra(
                                                                     "voltage", 0);
                                                             mTemperature = intent.getIntExtra(
                                                                     "temperature", 0);
                                                             mTechnology = intent
                                                                     .getStringExtra("technology");

                                                             switch (status) {
                                                             case BatteryManager.BATTERY_STATUS_UNKNOWN:
                                                                 mStatus = "unknown";
                                                                 break;
                                                             case BatteryManager.BATTERY_STATUS_CHARGING:
                                                                 mStatus = "charging";
                                                                 break;
                                                             case BatteryManager.BATTERY_STATUS_DISCHARGING:
                                                                 mStatus = "discharging";
                                                                 break;
                                                             case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                                                                 mStatus = "not charging";
                                                                 break;
                                                             case BatteryManager.BATTERY_STATUS_FULL:
                                                                 mStatus = "full";
                                                                 break;
                                                             }

                                                             switch (health) {
                                                             case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                                                                 mHealth = "unknown";
                                                                 break;
                                                             case BatteryManager.BATTERY_HEALTH_GOOD:
                                                                 mHealth = "good";
                                                                 break;
                                                             case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                                                                 mHealth = "overheat";
                                                                 break;
                                                             case BatteryManager.BATTERY_HEALTH_DEAD:
                                                                 mHealth = "dead";
                                                                 break;
                                                             case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                                                                 mHealth = "voltage";
                                                                 break;
                                                             case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                                                                 mHealth = "unspecified failure";
                                                                 break;
                                                             }

                                                             switch (plugged) {
                                                             case BatteryManager.BATTERY_PLUGGED_AC:
                                                                 mPlugged = "plugged ac";
                                                                 break;
                                                             case BatteryManager.BATTERY_PLUGGED_USB:
                                                                 mPlugged = "plugged usb";
                                                                 break;
                                                             }
                                                         }
                                                     }
                                                 };
}
