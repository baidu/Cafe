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

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.baidu.cafe.local.LocalLib;

/**
 * This is a interface-class to invoke functions of
 * com.baidu.cafe.remote.ArmsBinder.
 * 
 * @author chengzhenyu@baidu.com
 * @date 2011-06-20
 */
public class Armser {

    private IRemoteArms         iArms                            = null;
    private Context             mContext                         = null;
    private MyServiceConnection serviceConnection                = null;
    private int                 mScreenHeight                    = 0;
    private int                 mScreenWidth                     = 0;

    public final static int     SCREEN_BRIGHTNESS_MODE_AUTOMATIC = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
    public final static int     SCREEN_BRIGHTNESS_MODE_MANUAL    = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    public final static int     GPS_PROVIDER                     = 0;
    public final static int     NETWORK_PROVIDER                 = 1;

    private class MyServiceConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            iArms = IRemoteArms.Stub.asInterface(service);

        }

        public void onServiceDisconnected(ComponentName name) {
            iArms = null;
        }
    }

    public Armser(Context context) {
        mContext = context;
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        mScreenHeight = display.getHeight();
        mScreenWidth = display.getWidth();
        serviceConnection = new MyServiceConnection();
    }

    public boolean bind(Context context) {
        context.bindService(new Intent(IRemoteArms.class.getName()), serviceConnection,
                Context.BIND_AUTO_CREATE);
        int count = 0;
        try {
            while (null == iArms) {
                Thread.sleep(200);
                count++;
                if (count == 20) { //timeout = 4 seconds
                    context.unbindService(serviceConnection);
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean unbind(Context context) {
        context.unbindService(serviceConnection);
        int count = 0;
        try {
            while (null != iArms) {
                Thread.sleep(200);
                count++;
                if (count == 20) {//timeout = 4 seconds
                    return false;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void print(String message) {
        Log.i("Armser", "" + message);
    }

    /**
     * get account name
     */
    public String getAccountName() {
        String str = null;
        try {
            str = iArms.getAccountName();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get account type
     */
    public String getAccountType() {
        String str = null;
        try {
            str = iArms.getAccountType();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningActivities() {
        String str = null;
        try {
            str = iArms.getAllRunningActivities();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningServices() {
        String str = null;
        try {
            str = iArms.getAllRunningServices();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOff() {
        try {
            iArms.setInputMethodShowOff();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOn() {
        try {
            iArms.setInputMethodShowOn();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if screen is locked
     * 
     * @return true if screen is locked
     */
    public boolean isScreenLocked() {
        boolean ret = true;
        try {
            ret = iArms.isScreenLocked();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setScreenLocked() {
        try {
            iArms.setScreenLocked();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setScreenUnlocked() {
        try {
            iArms.setScreenUnlocked();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * get battery status
     * 
     * @return one of the following value:<br/>
     *         1. "unknown"<br/>
     *         2. "charging"<br/>
     *         3. "discharging", <br/>
     *         4. "not charging", <br/>
     *         5. "full"
     */
    public String getBatteryStatus() {
        String str = null;
        try {
            str = iArms.getBatteryStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get battery status
     * 
     * @return one of the following value:<br/>
     *         1. "unknown"<br/>
     *         2. "good"<br/>
     *         3. "overheat", <br/>
     *         4. "dead", <br/>
     *         5. "voltage"<br/>
     *         6. "unspecified failure"<br/>
     */
    public String getBatteryHealth() {
        String str = null;
        try {
            str = iArms.getBatteryHealth();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get battery present
     * 
     * @return true or false indicating whether a battery is present
     */
    public boolean getBatteryPresent() {
        boolean ret = true;
        try {
            ret = iArms.getBatteryPresent();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * get battery level
     * 
     * @return integer containing the current battery level from 0 to battery
     *         scale which can get by getBatteryScale() function
     */
    public int getBatteryLevel() {
        int i = 0;
        try {
            i = iArms.getBatteryLevel();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get battery scale
     * 
     * @return integer containing the maximum battery level
     */
    public int getBatteryScale() {
        int i = 0;
        try {
            i = iArms.getBatteryScale();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get resource id of battery small icon
     * 
     * @return integer containing the resource ID of a small status bar icon
     *         indicating the current battery state
     */
    public int getBatteryIconsmall() {
        int i = 0;
        try {
            i = iArms.getBatteryIconsmall();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get battery plugged category
     * 
     * @return one of the following value:<br/>
     *         0. null<br/>
     *         1. "plugged ac"<br/>
     *         2. "plugged usb"<br/>
     */
    public String getBatteryPlugged() {
        String str = null;
        try {
            str = iArms.getBatteryPlugged();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get battery voltage
     * 
     * @return integer containing the current battery voltage level
     */
    public int getBatteryVoltage() {
        int i = 0;
        try {
            i = iArms.getBatteryVoltage();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get battery temperature
     * 
     * @return integer containing the current battery temperature
     */
    public int getBatteryTemperature() {
        int i = 0;
        try {
            i = iArms.getBatteryTemperature();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get battery technology
     * 
     * @return string describing the technology of the current battery
     */
    public String getBatteryTechnology() {
        String str = null;
        try {
            str = iArms.getBatteryTechnology();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get bluetooth address
     * 
     * @return 1. string of bluetooth addresss<br/>
     *         2. "device not BT capable"<br/>
     *         3. "Unavailable"<br/>
     */
    public String getBlueToothAddress() {
        String str = null;
        try {
            str = iArms.getBlueToothAddress();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get build version
     */
    public String getBuildVersion() {
        String str = null;
        try {
            str = iArms.getBuildVersion();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get baseband version
     * 
     * e.g. "32.41.00.32H_5.08.00.04"
     */
    public String getBaseBandVersion() {
        String str = null;
        try {
            str = iArms.getBuildVersion();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get device model
     * 
     * Full BMOS on Passion
     */
    public String getDeviceModel() {
        String str = null;
        try {
            str = iArms.getDeviceModel();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get build number
     * 
     * @return string <br/>
     *         e.g "full_passion-userdebug 2.3.3 GRI40 1.2.23-11395 test-keys"
     */
    public String getBuildNumber() {
        String str = null;
        try {
            str = iArms.getBuildNumber();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get kernel version
     */
    public String getKernelVersion() {
        String str = null;
        try {
            str = iArms.getKernelVersion();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void recordVideo() {
        try {
            iArms.recordVideo();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String addContact(String name, String phone) {
        try {
            return iArms.addContact(name, phone);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int deleteContact(String uriStr) {
        try {
            return iArms.deleteContact(uriStr);
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * get the current audio mode
     * 
     * @return integer<br/>
     *         0: MODE_NORMAL, means not ringing and no call established<br/>
     *         1: MODE_RINGTONE means an incoming is being signaled<br/>
     *         2: MODE_IN_CALL means a telephony call is established<br/>
     *         3: MODE_IN_COMMUNICATION meas an audio/video chat or VoIP call is
     *         established<br/>
     */
    public int getAudioMode() {
        int i = 0;
        try {
            i = iArms.getAudioMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get the audio volume
     * 
     * @param streamType
     * <br/>
     *            -----------------------------<br/>
     *            STREAM_VOICE_CALL 0<br/>
     *            STREAM_SYSTEM 1<br/>
     *            STREAM_RING 2<br/>
     *            STREAM_MUSIC 3<br/>
     *            STREAM_ALARM 4<br/>
     *            STREAM_NOTIFICATION 5<br/>
     *            STREAM_BLUETOOTH_SCO 6 (not defined in android SDK)<br/>
     *            STREAM_SYSTEM_ENFORCED 7 (not defined in android SDK)<br/>
     *            STREAM_DTMF 8<br/>
     *            STREAM_TTS 9 (not defined in android SDK)<br/>
     *            ------------------------------<br/>
     * 
     * @return the current volume index for the stream
     */
    public int getAudioVolume(int streamType) {
        int i = 0;
        try {
            i = iArms.getAudioVolume(streamType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get the current ringtone mode
     * 
     * @return integer, one of <br/>
     *         0: RINGER_MODE_SILENT <br/>
     *         1: RING_MODE_VIBRATE<br/>
     *         2: RINGER_MODE_NORMAL<br/>
     */
    public int getRingtoneMode() {
        int i = 0;
        try {
            i = iArms.getRingtoneMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * check if any music is active
     * 
     * @return true if any music tracks are active
     */
    public boolean isMusicActive() {
        boolean ret = true;
        try {
            ret = iArms.isMusicActive();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * set the audio volume down
     * 
     * @param streamType
     * <br/>
     *            -----------------------------<br/>
     *            STREAM_VOICE_CALL 0<br/>
     *            STREAM_SYSTEM 1<br/>
     *            STREAM_RING 2<br/>
     *            STREAM_MUSIC 3<br/>
     *            STREAM_ALARM 4<br/>
     *            STREAM_NOTIFICATION 5<br/>
     *            STREAM_BLUETOOTH_SCO 6 (not defined in android SDK)<br/>
     *            STREAM_SYSTEM_ENFORCED 7 (not defined in android SDK)<br/>
     *            STREAM_DTMF 8<br/>
     *            STREAM_TTS 9 (not defined in android SDK)<br/>
     *            ------------------------------<br/>
     */
    public void setAudioVolumeDown(int streamType) {
        try {
            iArms.setAudioVolumeDown(streamType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * set the audio volume up
     * 
     * @param streamType
     * <br/>
     *            -----------------------------<br/>
     *            STREAM_VOICE_CALL 0<br/>
     *            STREAM_SYSTEM 1<br/>
     *            STREAM_RING 2<br/>
     *            STREAM_MUSIC 3<br/>
     *            STREAM_ALARM 4<br/>
     *            STREAM_NOTIFICATION 5<br/>
     *            STREAM_BLUETOOTH_SCO 6 (not defined in android SDK)<br/>
     *            STREAM_SYSTEM_ENFORCED 7 (not defined in android SDK)<br/>
     *            STREAM_DTMF 8<br/>
     *            STREAM_TTS 9 (not defined in android SDK)<br/>
     *            ------------------------------<br/>
     */
    public void setAudioVolumeUp(int streamType) {
        try {
            iArms.setAudioVolumeUp(streamType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setAudioMuteOn(int streamType) {
        try {
            iArms.setAudioMuteOn(streamType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setAudioMuteOff(int streamType) {
        try {
            iArms.setAudioMuteOff(streamType);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * get available internal memory
     * 
     * @return byte value, can be formated to string by formatSize(long size)
     *         function<br/>
     *         e.g. 135553024
     */

    public long getMemoryInternalAvail() {
        long i = 0;
        try {
            i = iArms.getMemoryInternalAvail();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * force the devcie to go to sleep
     */
    public void goToSleep() {
        try {
            iArms.goToSleep();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if screen is currently on
     */
    public boolean isScreenOn() {
        boolean ret = true;
        try {
            ret = iArms.isScreenOn();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * reboot device
     */
    public void reboot() {
        try {
            iArms.reboot();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * reboot device to recovery mode
     */
    public void rebootToRecoveryMode() {
        try {
            iArms.rebootToRecoveryMode();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * reboot device to bootloader
     */
    public void rebootToBootloader() {
        try {
            iArms.rebootToBootloader();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * check if storage card is currently valid
     */
    public boolean isStorageCardValid() {
        boolean ret = true;
        try {
            ret = iArms.isStorageCardValid();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * check if storage card is currently readonly
     */
    public boolean isStorageCardReadOnly() {
        boolean ret = true;
        try {
            ret = iArms.isStorageCardReadOnly();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void writeLineToSdcard(String filename, String line) {
        return;
    }

    public long getStorageCardSize() {
        long i = 0;
        try {
            i = iArms.getStorageCardSize();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get storage card total size
     * 
     * @return byte value, can be formated to string by formatSize(long size)
     *         function
     */
    public long getStorageCardAvail() {
        long i = 0;
        try {
            i = iArms.getStorageCardAvail();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * check if any app is accessing storage card
     */
    public boolean hasAppsAccessingStorage() {
        boolean ret = true;
        try {
            ret = iArms.hasAppsAccessingStorage();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * mount storage card
     */
    public void mount() {
        try {
            iArms.mount();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * unmount storage card
     */
    public void unmount() {
        try {
            iArms.unmount();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * get width of screen
     */
    public int getDisplayX() {
        int i = 0;
        try {
            i = iArms.getDisplayX();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get height of screen
     */
    public int getDisplayY() {
        int i = 0;
        try {
            i = iArms.getDisplayY();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getScreenBrightness() {
        int i = 0;
        try {
            i = iArms.getScreenBrightness();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * get system time
     * 
     * e.g. "Nov 29, 2011 4:46:28 PM"
     */
    public String getSystemTime() {
        String str = null;
        try {
            str = iArms.getSystemTime();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * set screen brightness
     * 
     * @param brightness
     *            , 0 ~ 255
     */

    public void setScreenBrightness(int brightness) {
        try {
            iArms.setScreenBrightness(brightness);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * set system time
     * 
     * @param time
     *            , format "yyyy/MM/dd hh:mm:ss", e.g. "2011/11/25 17:30:00"
     */
    public void setSystemTime(String time) {
        try {
            iArms.setSystemTime(time);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * get my phone number
     * 
     */
    public String getMyPhoneNumber() {
        String str = null;
        try {
            str = iArms.getMyPhoneNumber();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get current network type
     * 
     * @return EDGE, UMTS, etc...
     */
    public String getNetworkType() {
        String str = null;
        try {
            str = iArms.getNetworkType();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get current data state
     * 
     * @return "unknown", "Connected", "Suspended", "Connecting" or
     *         "Disconnected"
     */
    public String getDataState() {
        String str = null;
        try {
            str = iArms.getDataState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get current sim card state
     * 
     * @return 0: SIM_STATE_UNKNOWN<br/>
     *         1: SIM_STATE_ABSENT<br/>
     *         2: SIM_STATE_PIN_REQUIRED<br/>
     *         3: SIM_STATE_PUK_REQUIRED<br/>
     *         4: SIM_STATE_NETWORK_LOCKED<br/>
     *         5: SIM_STATE_READY<br/>
     */
    public int getSimCardState() {
        int i = 0;
        try {
            i = iArms.getSimCardState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getSmsState() {
        String str = null;
        try {
            str = iArms.getSmsState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * set airplane mode
     * 
     * @param enable
     *            true means open; false means closed
     */
    public void setAirplaneMode(boolean enable) {
        try {
            iArms.setAirplaneMode(enable);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * set mobile data disabled
     */
    public void setDataConnectionDisabled() {
        try {
            iArms.setDataConnectionDisabled();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * set mobile data enalbed
     */
    public void setDataConnectionEnabled() {
        try {
            iArms.setDataConnectionEnabled();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * formats a content size to be in the form of bytes, kilobytes, megabytes,
     * etc<br/>
     * 
     * @return e.g. formatSize(123456789) = "118MB"
     */

    public String formatSize(long size) {
        String str = null;
        try {
            str = iArms.formatSize(size);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get current wifi mac address
     */
    public String getWlanMacAddress() {
        String str = null;
        try {
            str = iArms.getWlanMacAddress();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * get current wifi state
     * 
     * @return 0: WIFI_STATE_DISABLING<br/>
     *         1: WIFI_STATE_DISABLED<br/>
     *         2: WIFI_STATE_ENABLING<br/>
     *         3: WIFI_STATE_ENABLED<br/>
     *         4: WIFI_STATE_UNKNOWN<br/>
     */
    public int getWifiState() {
        int i = 0;
        try {
            i = iArms.getWifiState();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return i;
    }

    /**
     * check if wifi is currently enabled
     */
    public boolean isWifiEnabled() {
        boolean ret = true;
        try {
            ret = iArms.isWifiEnabled();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * set wifi disabled
     */
    public boolean setWifiDisabled() {
        boolean b = true;
        try {
            b = iArms.setWifiDisabled();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * set wifi enabled
     */
    public boolean setWifiEnabled() {
        boolean b = true;
        try {
            b = iArms.setWifiEnabled();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * set wifi disconnect
     */
    public boolean setWifiDisconnect() {
        boolean b = true;
        try {
            b = iArms.setWifiDisconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * set wifi reconnect
     */
    public boolean setWifiReconnect() {
        boolean b = true;
        try {
            b = iArms.setWifiReconnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * set wifi starting scan now
     */
    public boolean setWifiStartScan() {
        boolean b = true;
        try {
            b = iArms.setWifiStartScan();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * get server ip
     * 
     * @return null: if pc does not push the arms.xml<br/>
     */
    /*
     * PC command: <br/> adb root<br/> adb mkdir
     * /data/data/baidu.cafe.arms/shared_prefs/<br/> adb push arms.xml
     * /data/data/baidu.cafe.arms/shared_prefs<br/>
     * 
     * arms.xml file format<br/> <?xml version='1.0' encoding='utf-8'
     * standalone='yes' ?> <map> <string name="serverip">172.0.0.1</string>
     * </map>
     */
    public String getServerIP() {
        String str = null;
        try {
            str = iArms.getServerIP();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * run command by service side on PC
     */
    @Deprecated
    public String runCmdOnServer(String command) {
        String str = null;
        try {
            str = iArms.runCmdOnServer(command);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    private static final int TIMEOUT_DEFAULT_VALUE = 10000;

    /**
     * press home key
     */
    public void goHome() {
        try {
            iArms.pressKey(KeyEvent.KEYCODE_HOME);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * press back key
     */
    public void goBack() {
        try {
            iArms.pressKey(KeyEvent.KEYCODE_BACK);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * get top activity
     * 
     * @return the name of top activity. ex. "com.baidu.baiduclock.BaiduClock"
     */
    public String getTopActivity() {
        String str = null;
        try {
            str = iArms.getTopActivity();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * @param className
     *            className of activity
     *            e.g."com.android.mms/.ui.ConversationList"
     * @return whether the activity launched succeed
     */
    public boolean launchActivity(String className) {
        return launchActivity(className, Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * launch a activity via sent intent
     * 
     * @param className
     *            className of activity
     *            e.g."com.android.mms/.ui.ConversationList"
     * @param flags
     *            param for intent.setFlags()
     * @return whether the activity launched succeed
     */
    public boolean launchActivity(String className, int flags) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        //        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(ComponentName.unflattenFromString(className));
        intent.setFlags(flags);
        mContext.startActivity(intent);

        final String[] classShortName = className.split("\\.");
        return waitForActivity(classShortName[classShortName.length - 1], TIMEOUT_DEFAULT_VALUE);
    }

    /**
     * @param activityName
     *            e.g.com.baidu.resmanager.filemanager.FileManagerActivity
     * @param timeout
     *            the delay millisecond waiting for activity
     * @return whether the activity appears
     */
    public boolean waitForActivity(String activityName, long timeout) {
        if (timeout < 0) {
            print("param error: timeout < 0");
            return false;
        }

        final long endTime = System.currentTimeMillis() + timeout;
        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                return false;
            }

            String topActivity = getTopActivity();

            if (topActivity.toLowerCase().contains(activityName.toLowerCase())) {
                return true;
            }
            sleep(500);
        }
    }

    /**
     * wait for view appearing by id and use
     * LocalLib.SEARCHMODE_COMPLETE_MATCHING and index=0
     * 
     * @param id
     *            the id of target views
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForViewById(String id, long timeout) {
        return waitForViewById(id, 0, timeout);
    }

    /**
     * wait for view appearing by id and use
     * LocalLib.SEARCHMODE_COMPLETE_MATCHING
     * 
     * @param id
     *            the id of target views
     * @param index
     *            the text of target views
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForViewById(String id, int index, long timeout) {
        return waitForView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index, timeout);
    }

    /**
     * wait for view appearing by text and use
     * LocalLib.SEARCHMODE_INCLUDE_MATCHING and index=0
     * 
     * @param text
     *            the text of target views
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForViewByText(String text, long timeout) {
        return waitForViewByText(text, 0, timeout);
    }

    /**
     * wait for view appearing by text and use
     * LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * 
     * @param text
     *            the text of target views
     * @param index
     *            the index of search result
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForViewByText(String text, int index, long timeout) {
        return waitForView("mText", text, LocalLib.SEARCHMODE_INCLUDE_MATCHING, index, timeout);
    }

    /**
     * wait for view appearing by text
     * 
     * @param text
     *            the text of target views
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of search result
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForViewByText(String text, int searchMode, int index, long timeout) {
        return waitForView("mText", text, searchMode, index, timeout);
    }

    /**
     * wait for view appearing
     * 
     * @param searchKey
     *            the key to search view
     * @param searchValue
     *            the value to search view
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of search result
     * @param timeout
     *            timeout of wait
     * @return whether the view appears
     */
    public boolean waitForView(String searchKey, String searchValue, int searchMode, int index,
            long timeout) {
        try {
            if (index < 0) {
                print("param error: index < 0");
                return false;
            }

            if (timeout < 0) {
                print("param error: timeout < 0");
                return false;
            }

            final long endTime = System.currentTimeMillis() + timeout;

            while (true) {
                long now = System.currentTimeMillis();
                final boolean timedOut = now > endTime;
                if (timedOut)
                    return false;

                if (iArms.checkView(searchKey, searchValue, searchMode, index + 1))
                    return true;
                sleep(500);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * enter a string
     * 
     * @param text
     */
    public void enterText(String text) {
        try {
            iArms.enterText(text);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        sleepMini();
    }

    /**
     * send a key evnet
     * 
     * @param keyCode
     *            e.g. KeyEvent.KEYCODE_HOME
     */
    public void pressKey(int keyCode) {
        try {
            iArms.pressKey(keyCode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        sleepMini();
    }

    /**
     * send a key evnet of long press
     * 
     * @param keyCode
     *            e.g. KeyEvent.KEYCODE_HOME
     */
    public void longPressKey(int keyCode) {
        try {
            iArms.longPressKey(keyCode);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        sleepMini();
    }

    /**
     * click on screen
     * 
     * @param x
     *            click.x
     * @param y
     *            click.y
     */
    public void clickScreen(int x, int y) {
        try {
            iArms.clickScreen(x, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * long click on screen
     * 
     * @param x
     *            click.x
     * @param y
     *            click.y
     * @param time
     *            time of long press
     */
    public void longClickScreen(int x, int y, int time) {
        try {
            iArms.clickLongScreen(x, y, time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * click on a view by id without auto-scroll and xOffset = 0, yOffset = 0,
     * timeout = 10000, index = 0
     * 
     * @param id
     *            id of view used to search
     * @return whether the view is clicked
     */
    public boolean clickViewById(String id) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0, null, 0);
    }

    /**
     * click on a view by id and xOffset = 0, yOffset = 0, timeout = 10000,
     * index = 0
     * 
     * @param id
     *            id of view used to search
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewById(String id, String scrollViewId, int scrollViewIndex) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0, scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by id and xOffset = 0, yOffset = 0, timeout = 10000
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewById(String id, int index, String scrollViewId, int scrollViewIndex) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0, scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by id and xOffset = 0, yOffset = 0
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewById(String id, int index, int timeout, String scrollViewId,
            int scrollViewIndex) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index, timeout, 0, 0, 0,
                scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by id
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param xOffset
     *            Offset.x of target view's center point
     * @param yOffset
     *            Offset.y of target view's center point
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewById(String id, int index, int timeout, int xOffset, int yOffset,
            String scrollViewId, int scrollViewIndex) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index, timeout, xOffset,
                yOffset, 0, scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by text without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000, index = 0, searchMode =
     * LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * 
     * @param text
     *            text of view used to search
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text) {
        return clickView("mText", text, LocalLib.SEARCHMODE_INCLUDE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0, null, 0);
    }

    /**
     * click on a view by text without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000, index = 0
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text, int searchMode) {
        return clickView("mText", text, searchMode, 0, TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0,
                null, 0);
    }

    /**
     * click on a view by text without offset and timeout = 10000, index = 0
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text, int searchMode, String scrollViewId,
            int scrollViewIndex) {
        return clickView("mText", text, searchMode, 0, TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0,
                scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by text without offset and timeout = 10000
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text, int searchMode, int index, String scrollViewId,
            int scrollViewIndex) {
        return clickView("mText", text, searchMode, index, TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0,
                0, scrollViewId, scrollViewIndex);
    }

    /**
     * click on a view by text without offset
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text, int searchMode, int index, int timeout,
            String scrollViewId, int scrollViewIndex) {
        return clickView("mText", text, searchMode, index, timeout, 0, 0, 0, scrollViewId,
                scrollViewIndex);
    }

    /**
     * click on a view by text
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param xOffset
     *            Offset.x of target view's center point
     * @param yOffset
     *            Offset.y of target view's center point
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickViewByText(String text, int searchMode, int index, int timeout,
            int xOffset, int yOffset, String scrollViewId, int scrollViewIndex) {
        return clickView("mText", text, searchMode, index, timeout, xOffset, yOffset, 0,
                scrollViewId, scrollViewIndex);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000, index = 0
     * 
     * @param id
     *            id of view used to search
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewById(String id, int time) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, time, null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewById(String id, int index, int time) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, time, null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewById(String id, int index, int timeout, int time) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index, timeout, 0, 0,
                time, null, 0);
    }

    /**
     * long click on a view without auto-scroll
     * 
     * @param id
     *            id of view used to search
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param xOffset
     *            Offset.x of target view's center point
     * @param yOffset
     *            Offset.y of target view's center point
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewById(String id, int index, int timeout, int xOffset, int yOffset,
            int time) {
        return clickView("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, index, timeout, xOffset,
                yOffset, time, null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000, index = 0, searchMode =
     * LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * 
     * @param text
     *            text of view used to search
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewByText(String text, int time) {
        return clickView("mText", text, LocalLib.SEARCHMODE_INCLUDE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, time, null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000, index = 0
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewByText(String text, int searchMode, int time) {
        return clickView("mText", text, searchMode, 0, TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, time,
                null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0,
     * timeout=10000
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewByText(String text, int searchMode, int index, int time) {
        return clickView("mText", text, searchMode, index, TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0,
                time, null, 0);
    }

    /**
     * long click on a view without auto-scroll and xOffset=0, yOffset = 0
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewByText(String text, int searchMode, int index, int timeout, int time) {
        return clickView("mText", text, searchMode, index, timeout, 0, 0, time, null, 0);
    }

    /**
     * long click on a view without auto-scroll
     * 
     * @param text
     *            text of view used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param xOffset
     *            Offset.x of target view's center point
     * @param yOffset
     *            Offset.y of target view's center point
     * @param time
     *            if time > 0, click will change to long click
     * @return whether the view is clicked
     */
    public boolean longClickViewByText(String text, int searchMode, int index, int timeout,
            int xOffset, int yOffset, int time) {
        return clickView("mText", text, searchMode, index, timeout, xOffset, yOffset, time, null, 0);
    }

    /**
     * click a view
     * 
     * @param searchKey
     *            name of view 's property used to search
     * @param searchValue
     *            value of view 's property used to search
     * @param searchMode
     *            the mode of search such as
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING and
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param index
     *            the index of target view
     * @param timeout
     *            timeout of dump
     * @param xOffset
     *            Offset.x of target view's center point
     * @param yOffset
     *            Offset.y of target view's center point
     * @param longClickTime
     *            if longClickTime > 0, click will change to long click
     * @param scrollViewId
     *            if want to click a item of {ListView, GridView, ScrollView},
     *            get the id of {ListView, GridView, ScrollView}
     * @param scrollViewIndex
     *            the index of target {ListView, GridView, ScrollView}
     * @return whether the view is clicked
     */
    public boolean clickView(String searchKey, String searchValue, int searchMode, int index,
            int timeout, int xOffset, int yOffset, int longClickTime, String scrollViewId,
            int scrollViewIndex) {
        boolean result = false;
        try {
            result = iArms.clickView(searchKey, searchValue, searchMode, index, timeout, xOffset,
                    yOffset, longClickTime, scrollViewId, scrollViewIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * drag from (mScreenHeight * 0.5f) to (mScreenHeight * 0.75f)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragQuarterScreenDown(int stepCount) {
        final float x = mScreenWidth / 2.0f;
        final float fromY = mScreenHeight * 0.5f;
        final float toY = mScreenHeight * 0.75f;

        drag(x, x, fromY, toY, stepCount);
    }

    /**
     * drag from (mScreenHeight * 0.5f) to (mScreenHeight * 0.25f)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragQuarterScreenUp(int stepCount) {
        final float x = mScreenWidth / 2.0f;
        final float fromY = mScreenHeight * 0.5f;
        final float toY = mScreenHeight * 0.25f;

        drag(x, x, fromY, toY, stepCount);
    }

    /**
     * drag from (mScreenWidth * 0.5f) to (mScreenWidth * 0.75f)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragQuarterScreenRight(int stepCount) {
        final float y = mScreenHeight / 2.0f;
        final float fromX = mScreenWidth * 0.5f;
        final float toX = mScreenWidth * 0.75f;

        drag(fromX, toX, y, y, stepCount);
    }

    /**
     * drag from (mScreenWidth * 0.5f) to (mScreenWidth * 0.25f)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragQuarterScreenLeft(int stepCount) {
        final float y = mScreenHeight / 2.0f;
        final float fromX = mScreenWidth * 0.5f;
        final float toX = mScreenWidth * 0.25f;

        drag(fromX, toX, y, y, stepCount);
    }

    /**
     * drag from (mScreenHeight * 0.5f) to (mScreenHeight)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragHalfScreenDown(int stepCount) {
        final float x = mScreenWidth / 2.0f;
        final float fromY = mScreenHeight * 0.5f;
        final float toY = mScreenHeight;

        drag(x, x, fromY, toY, stepCount);
    }

    /**
     * drag from (mScreenHeight * 0.5f) to (0)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragHalfScreenUp(int stepCount) {
        final float x = mScreenWidth / 2.0f;
        final float fromY = mScreenHeight * 0.5f;
        final float toY = 0;

        drag(x, x, fromY, toY, stepCount);
    }

    /**
     * drag from (mScreenWidth * 0.5f) to (mScreenWidth)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragHalfScreenRight(int stepCount) {
        final float y = mScreenHeight / 2.0f;
        final float fromX = mScreenWidth * 0.5f;
        final float toX = mScreenWidth;

        drag(fromX, toX, y, y, stepCount);
    }

    /**
     * drag from (mScreenWidth * 0.5f) to (0)
     * 
     * @param stepCount
     *            How many move steps to include in the drag
     */
    public void dragHalfScreenLeft(int stepCount) {
        final float y = mScreenHeight / 2.0f;
        final float fromX = mScreenWidth * 0.5f;
        final float toX = 0;

        drag(fromX, toX, y, y, stepCount);
    }

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        try {
            iArms.drag(fromX, toX, fromY, toY, stepCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * only for CafeTestCase.tearDown() or testcase that doesn't inherit
     * CafeTestCase and should be called only one time
     */
    public void waitForAllDumpCompleted() {
        try {
            iArms.waitForAllDumpCompleted();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SystemClock.sleep(time);
     * 
     * @param time
     *            sleep time
     */
    public void sleep(int time) {
        SystemClock.sleep(time);
    }

    /**
     * sleep 200ms
     */
    public void sleepMini() {
        SystemClock.sleep(200);
    }

    /**
     * set screen still light
     */
    public void setScreenOn() {
        try {
            iArms.setScreenOn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * replaced by pressKey()
     * 
     * @param keycode
     */
    @Deprecated
    public void sendKeyEvent(int keycode) {
        try {
            iArms.sendKeyEvent(keycode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param className
     *            ex. "com.baidu.baiduclock.BaiduClock"
     * @param timeout
     *            delay. in terms of milliseconds
     * @return true we got it
     */
    public boolean waitforTopActivity(String className, long timeout) {
        boolean result = false;

        try {
            result = iArms.waitforTopActivity(className, timeout);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return result;
    }

    /**
     * factory reset with erase sd card
     */
    public void factoryResetWithEraseSD() {
        try {
            iArms.factoryResetWithEraseSD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * format sd card
     */
    public void formatSD() {
        try {
            iArms.formatSD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set the screen stay awake
     * 
     * @param true: stay awake
     * 
     */
    public void setScreenStayAwake(boolean isAwake) {
        try {
            iArms.setScreenStayAwake(isAwake);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set the timeout to lock screen
     * 
     * @param milisecond
     *            : 15000/30000/60000/120000/600000/1800000 can be used
     * 
     */
    public void setScreenTimeOut(int milisecond) {
        try {
            iArms.setScreenTimeOut(milisecond);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set the screen unlock mode is none
     */
    public void setScreenUnlockSecurityNone() {
        try {
            iArms.setScreenUnlockSecurityNone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * change language
     * 
     * @param language
     *            : zh---Chinese en---English
     */
    public void changeLanguage(String language) {
        try {
            iArms.changeLanguage(language);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * you must use "isPackageInstalled(String)" to judge if it is installed
     * finish. otherwise you can use public void InstallapkSync(String filename)
     * 
     * @param filename
     *            : the apk name you want to install. for example:
     *            "/data/data/baidumap.apk"
     */
    /*
     * the sd card location is not permited!  because the service use "android:sharedUserId="android.uid.system""
     * */
    public void installApk(String filename) {
        try {
            iArms.installApk(filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * uninstall an apk. before this you can use isPackageInstalled(String) to
     * detect that the package is installed or not.
     * 
     * @param packageName
     *            : packagename you want to uninstall,
     */
    public void uninstallApk(String packageName) {
        try {
            iArms.uninstallApk(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * check a package is installed or not.
     * 
     * @param packageName
     *            : packagename you want to detect
     * @return true installed false not installed
     */
    public boolean isPackageInstalled(String packageName) {
        boolean ret = false;
        try {
            ret = iArms.isPackageInstalled(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * install a apk in sdcard, return until it install finish.
     * 
     * @param filename
     *            : apk location you want to install ,"/sdcard/baidu.apk"
     * @param timeout
     *            : the max time you can suffer
     * @return true installed false not installed or timeout
     */
    public boolean installApkSync(String filename, long timeout) {
        boolean ret = false;
        try {
            ret = iArms.installApkSync(filename, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * get system property same as "adb shell get prop"
     * 
     * @param key
     *            e.g. ro.baidu.build.software
     * @return the property
     */
    public String getSystemProperties(String key) {
        String str = "";
        try {
            str = iArms.getSystemProperties(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * set system property same as "adb shell set prop"
     * 
     * @param key
     *            e.g. ro.baidu.build.software
     * @param val
     *            e.g. cafe
     */
    public void setSystemProperties(String key, String val) {
        try {
            iArms.setSystemProperties(key, val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get status of input method
     * 
     * @return true means VISIBLE; false means GONE
     */
    public boolean getInputMethodStatus() {
        boolean ret = false;
        try {
            ret = iArms.getInputMethodStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * get view's properties from focused window
     * 
     * @param searchKey
     *            property's name use to search
     * @param searchValue
     *            property's value use to search
     * @param searchMode
     *            LocalLib.SEARCHMODE_COMPLETE_MATCHING
     *            LocalLib.SEARCHMODE_INCLUDE_MATCHING
     * @param targetNumber
     *            target index in search result. Beacuse a pair of searchKey and
     *            searchValue can return not only one search result.
     * @param getKeys
     *            property's name use to get
     * @param getNew
     *            True means start a new dump. If UI has changed, use true.
     *            False means get propery from last dump. If UI has not changed,
     *            use false.
     * 
     * @return String[] getValues
     */
    public String[] getViewProperties(String searchKey, String searchValue, int searchMode,
            int targetNumber, String[] getKeys, boolean getNew) {
        String[] ret = null;
        try {
            ret = iArms.getViewProperties(searchKey, searchValue, searchMode, targetNumber,
                    getKeys, getNew);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * get focused window name
     * 
     * @return the name of focused window
     */
    public String getFocusedWindow() {
        String ret = "";
        try {
            ret = iArms.getFocusedWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * check the given processname alive or not
     * 
     * @return true if it alive
     */
    public boolean checkProcessAlive(String processName) {
        boolean ret = false;
        try {
            ret = iArms.checkProcessAlive(processName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * check nomarketappsallowed is available
     * 
     * @return 1 set 0 noset -1 unknow default is not set
     * @throws RemoteException
     */
    public int getNonMarketAppsAllowed() {
        int ret = -1;
        try {
            ret = iArms.getNonMarketAppsAllowed();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * set nomarketappsallowed is available
     * 
     * @param true enable false disable
     * @throws RemoteException
     */
    public void setNonMarketAppsAllowed(boolean enabled) {
        try {
            iArms.setNonMarketAppsAllowed(enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * check bluetooth state
     * 
     * @return true enalbed false disabled
     */
    public boolean isBluetoothEnabled() {
        boolean ret = false;
        try {
            ret = iArms.isBluetoothEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * set bluetooth state
     * 
     * @param true enabled false disabled
     */
    public void setBluetoothState(boolean enabled) {
        try {
            iArms.setBluetoothState(enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Have the system immediately kill all background processes associated with
     * the given package. This is the same as the kernel killing those processes
     * to reclaim memory; the system will take care of restarting these
     * processes in the future as needed.
     * 
     * @param packageName
     */
    public void killBackgroundProcesses(String packageName) {
        try {
            iArms.killBackgroundProcesses(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lists all of the available windows in the system
     * 
     * @return the window list
     */
    public String[] getWindowList() {
        String[] ret = null;
        try {
            ret = iArms.getWindowList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * clear application's user data same as click
     * SystemSettings->Applications->Manage Applications->XXXX
     * application->Clear Data; suggest to use this function in teardown
     * 
     * @param packageName
     *            application's packageName
     */
    public boolean clearApplicationUserData(String packageName) {
        try {
            return iArms.clearApplicationUserData(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * change package's pemission state for dynamic permission
     * 
     * @param packageName
     *            target package name
     * @param permissionName
     *            permission name
     * @param state
     *            ALWAYS_ASK_DISABLE = 0; ALWAYS_DISENABLE = 1;
     *            ALWAYS_ASK_ENABLE = 2; ALWAYS_ENABLE = 3;
     */
    public void updatePackagePermission(String packageName, String permissionName, int state) {
        try {
            iArms.updatePackagePermission(packageName, permissionName, state);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get package's pemissions
     * 
     * @param packageName
     *            target package name
     * @return permissions string
     */
    public String[] getPermissionsForPackage(String packageName) {
        String[] ret = null;
        try {
            ret = iArms.getPermissionsForPackage(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public int getAutoTimeState() {
        int ret = 0;
        try {
            ret = iArms.getAutoTimeState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setAutoTimeEnabled() {
        try {
            iArms.setAutoTimeEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setAutoTimeDisabled() {
        try {
            iArms.setAutoTimeDisabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int      ICON_MORE                 = 0;
    public static final int      ICON_COM_BAIDU_SYNC       = 1;
    public static final int      ICON_COM_BAIDU_INPUT      = 2;
    public static final int      ICON_HEADSET              = 3;
    public static final int      ICON_COM_ANDROID_SYSTEMUI = 4;
    public static final int      ICON_SYNC_FAILING         = 5;
    public static final int      ICON_SYNC_ACTIVE          = 6;
    public static final int      ICON_GPS                  = 7;
    public static final int      ICON_BLUETOOTH            = 8;
    public static final int      ICON_TTY                  = 9;
    public static final int      ICON_VOLUME               = 10;
    public static final int      ICON_WIFI                 = 11;
    public static final int      ICON_CDMA_ERI             = 12;
    public static final int      ICON_DATA_CONNECTION      = 13;
    public static final int      ICON_PHONE_SIGNAL         = 14;
    public static final int      ICON_BATTERY              = 15;
    public static final int      ICON_ALARM_CLOCK          = 16;

    public static final String[] iconName                  = { "more", //ICON_MORE = 0
            "com.baidu.sync",//ICON_COM_BAIDU_SYNC = 1
            "com.baidu.input", //ICON_COM_BAIDU_INPUT = 2
            "headset", //ICON_HEADSET = 3
            "com.android.systemui", //ICON_COM_ANDROID_SYSTEMUI = 4;
            "sync_failing", //ICON_SYNC_FAILING = 5
            "sync_active", //ICON_SYNC_ACTIVE = 6
            "gps", //ICON_GPS = 7
            "bluetooth", //ICON_BLUETOOTH = 8
            "tty", //ICON_TTY = 9
            "volume", //ICON_VOLUME = 10
            "wifi", //ICON_WIFI = 11
            "cdma_eri", //ICON_CDMA_ERI = 12
            "data_connection", //ICON_DATA_CONNECTION = 13
            "phone_signal", //ICON_PHONE_SIGNAL = 14
            "battery", //ICON_BATTERY = 15
            "alarm_clock" //ICON_ALARM_CLOCK = 16
                                                           };

    /**
     * get icon state on statusbar
     * 
     * @param slot
     *            mSlot of StatusBarIconView e.g. 3G icon's name is
     *            data_connection
     * @return state of icon; true means VISIBLE, false means GONE
     */
    public boolean getStatusBarIconState(int slot) {
        boolean ret = false;
        try {
            ret = iArms.getStatusBarIconState(iconName[slot]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * get log information, by calling logcat with parameters "-d -v -time"
     * 
     * @return string array, including log lines
     */
    public String[] getLog() {
        return getLog(new String[] { "logcat", "-d", "-v", "time" });
    }

    /**
     * get log information, by calling logcat with parameters "-d -v -time" and
     * filterSpecs
     * 
     * @param filterspecs
     *            string including a series of <tag>[:priority]
     * @return string array, including log lines
     */
    public String[] getLog(String filterSpecs) {
        return getLog(new String[] { "logcat", "-d", "-v", "time", "-s", filterSpecs });
    }

    /**
     * get log information, by calling logcat with specified parameters
     * 
     * @param command
     *            sample here: new String[]{"logcat", "-d", "-v", "time", "-s",
     *            "MyTest:I"}
     * @return string array, including log lines
     */
    public String[] getLog(String[] command) {
        String[] ret = null;
        try {
            ret = iArms.getLog(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * clear (flush) the entire log, by calling "logcat -c"
     */
    public void clearLog() {
        try {
            iArms.clearLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * diff log
     * 
     * @param logStart
     *            , log in start time
     * @param logEnd
     *            , log in end time
     * @return String array, diff log between start time and end time
     */
    public String[] diffLog(String[] logStart, String[] logEnd) {
        /*for(int i = logStart.length - 1; i >= 0; i--) {
            Log.i("checkByLog", "logStart[" + i + "] = " + logStart[i]);
        }

        for(int i= logEnd.length - 1; i >= 0; i--) {
            Log.i("checkByLog", "logEnd[" + i + "] = " + logEnd[i]);
        }
        */
        int lengthStart = logStart.length;
        if (0 == lengthStart) {
            return logEnd;
        }

        int lengthEnd = logEnd.length;
        ArrayList<String> diffLogLines = new ArrayList<String>();

        for (int i = lengthEnd - 1; i >= 0; i--) {
            if (logEnd[i].equals(logStart[lengthStart - 1])) {
                break;
            }

            diffLogLines.add(logEnd[i]);
        }

        return diffLogLines.toArray(new String[diffLogLines.size()]);
    }

    /**
     * take full screen cap,the function use /system/bin/screencap cmd
     */
    public void screenCap(String prefix) {
        try {
            iArms.screenCap(prefix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[] getCenterXY(String[] coordinates) {
        int[] centerXY = new int[2];
        int x = Integer.valueOf(coordinates[0]);
        int y = Integer.valueOf(coordinates[1]);
        int width = Integer.valueOf(coordinates[2]);
        int height = Integer.valueOf(coordinates[3]);
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        centerXY[0] = centerX;
        centerXY[1] = centerY;

        return centerXY;
    }

    /**
     * get view's center point coordinate by id
     * 
     * @param id
     * @return coordinate array
     */
    public int[] getViewCenterPointById(String id) {
        return getCenterXY(getViewProperties("mID", id, LocalLib.SEARCHMODE_COMPLETE_MATCHING, 1,
                new String[] { "coordinate" }, true));
    }

    /**
     * get view's center point coordinate by id
     * 
     * @param text
     * @return coordinate array
     */
    public int[] getViewCenterPointByText(String text) {
        return getCenterXY(getViewProperties("mText", text, LocalLib.SEARCHMODE_COMPLETE_MATCHING,
                1, new String[] { "coordinate" }, true));
    }

    /**
     * @return Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC |
     *         Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
     */
    public int getScreenBrightnessMode() {
        int ret = 0;
        try {
            ret = iArms.getScreenBrightnessMode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param mode
     *            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC |
     *            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
     */
    public void setScreenBrightnessMode(int mode) {
        try {
            iArms.setScreenBrightnessMode(mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param provider
     *            LocationManager.GPS_PROVIDER |
     *            LocationManager.NETWORK_PROVIDER
     * @param enabled
     *            true | false
     */
    public void setLocationProviderEnabled(int provider, boolean enabled) {
        try {
            if (provider == GPS_PROVIDER) {
                iArms.setLocationProviderEnabled(LocationManager.GPS_PROVIDER, enabled);
            } else if (provider == NETWORK_PROVIDER) {
                iArms.setLocationProviderEnabled(LocationManager.NETWORK_PROVIDER, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param provider
     *            LocationManager.GPS_PROVIDER |
     *            LocationManager.NETWORK_PROVIDER
     * @return true | false
     */
    public boolean isLocationProviderEnabled(int provider) {
        boolean ret = false;
        try {
            if (provider == GPS_PROVIDER) {
                ret = iArms.isLocationProviderEnabled(LocationManager.GPS_PROVIDER);
            } else if (provider == NETWORK_PROVIDER) {
                ret = iArms.isLocationProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean isAccelerometerRotationEnabled() {
        boolean ret = false;
        try {
            ret = iArms.isAccelerometerRotationEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setAccelerometerRotationEnabled(boolean enabled) {
        try {
            iArms.setAccelerometerRotationEnabled(enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getBackgroundDataState() {
        boolean ret = false;
        try {
            ret = iArms.getBackgroundDataState();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setBackgroundDataSetting(boolean enabled) {
        try {
            iArms.setBackgroundDataSetting(enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getMasterSyncAutomatically() {
        boolean ret = false;
        try {
            ret = iArms.getMasterSyncAutomatically();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setMasterSyncAutomatically(boolean sync) {
        try {
            iArms.setMasterSyncAutomatically(sync);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param name
     *            account name e.g. BIT_Eric
     * @param type
     *            account type e.g. com.baidu
     */
    public void deleteAccount(String name, String type) {
        try {
            iArms.deleteAccount(name, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recovery() {
        try {
            iArms.recovery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCurrentTaskActivitiesNumber() {
        int ret = 0;
        try {
            ret = iArms.getCurrentTaskActivitiesNumber();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void setStatusBarHeight(int height) {
        try {
            iArms.setStatusBarHeight(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isViewServerOpen() {
        boolean ret = false;
        try {
            ret = iArms.isViewServerOpen();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Judge whether top activity is home.
     */
    public boolean isHome() {
        boolean ret = false;
        try {
            ret = iArms.isHome();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * ping 8.8.8.8
     * 
     * @return
     */
    public boolean isNetworkEnable() {
        boolean ret = false;
        try {
            ret = iArms.isNetworkEnable();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public String getTopPackage() {
        String ret = "";
        try {
            ret = iArms.getTopPackage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void copyAssets(String dist) {
        try {
            iArms.copyAssets(dist);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void expandStatusBar() {
        try {
            iArms.expandStatusBar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStringByName(String name) {
        String ret = "";
        try {
            ret = iArms.getStringByName(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
