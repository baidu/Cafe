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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.backup.BackupManager;
import android.app.StatusBarManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.PowerManager.WakeLock;
import android.os.storage.IMountService;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Contacts;
import android.provider.Settings;
import android.provider.Contacts.People;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.WindowManager;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;

import com.android.internal.os.PowerProfile;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.storage.ExternalStorageFormatter;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.widget.LockPatternUtils;
import com.baidu.cafe.utils.ReflectHelper;
import com.baidu.cafe.utils.ShellExecute;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.Process;
import java.lang.Runtime;
import java.lang.System;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide system services to get and set system state.
 */
public class SystemLib {

    private final String         PREFERENCE_NAME = "arms";
    private static final boolean DEBUG           = false;

    private Context              mContext;
    private TelephonyManager     mTelephonyManager;
    private KeyguardManager      mKeyguardManager;
    private BatteryState         mBatteryState;
    private AudioManager         mAudioManager;
    private PowerManager         mPowerManager;
    private PackageManager       mPackageManager;
    private WifiManager          mWifiManager;
    private IntentFilter         mIntentFilter;
    private IMountService        mMountService   = null;
    private WakeLock             mWakeLock;
    private ActivityManager      mActivityManager;
    private StatusBarManager     mStatusBarManager;

    public SystemLib(Context context) {
        mContext = context;
        mStatusBarManager = (StatusBarManager) mContext.getSystemService("statusbar");
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mBatteryState = new BatteryState(mContext);
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_DIM_WAKE_LOCK, "Test Acquired!");
        //mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK,
        //        "Test Acquired!");
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * light up the screen
     */
    public void setScreenOn() {
        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_DIM_WAKE_LOCK, "Test Acquired!");
        mWakeLock.acquire();
        mWakeLock.release();
    }

    /**
     * 
     * kill background processes according to package name
     * 
     * @param packageName
     *            the package name what the processes belong to
     */
    public void killBackgroundProcesses(String packageName) {
        mActivityManager.killBackgroundProcesses(packageName);
    }

    /**
     * get accounts'name
     * 
     * @return the account's name
     */
    public String getAccountName() {
        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccounts();
        return accounts.length > 0 ? accounts[0].name : "";
    }

    /**
     * get account's type
     * 
     * @return the account's type
     */
    public String getAccountType() {
        AccountManager accountManager = AccountManager.get(mContext);
        Account[] accounts = accountManager.getAccounts();
        return accounts.length > 0 ? accounts[0].type : "";

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningActivities() {
        return "";
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningServices() {
        return "";
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOff() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOn() {

    }

    /**
     * check whether the screen is locked
     * 
     * @return true if the the screen is locked, false if unlocked
     */
    public boolean isScreenLocked() {
        return mKeyguardManager.inKeyguardRestrictedInputMode();
    }

    /**
     * lock the screen
     */
    public void setScreenLocked() {
        mKeyguardManager.newKeyguardLock("").reenableKeyguard();
    }

    /**
     * unlock the screen
     */
    public void setScreenUnlocked() {
        mKeyguardManager.newKeyguardLock("").disableKeyguard();
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
        String status = "";
        mBatteryState.init();
        status = mBatteryState.getStatus();
        mBatteryState.deinit();
        return status;
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
        String health = "";
        mBatteryState.init();
        health = mBatteryState.getHealth();
        mBatteryState.deinit();
        return health;
    }

    /**
     * get battery present
     * 
     * @return true or false indicating whether a battery is present
     */
    public boolean getBatteryPresent() {
        boolean present = true;
        mBatteryState.init();
        present = mBatteryState.getPresent();
        mBatteryState.deinit();
        return present;
    }

    /**
     * get battery level
     * 
     * @return integer containing the current battery level from 0 to battery
     *         scale which can get by getBatteryScale() function
     */
    public int getBatteryLevel() {
        int level = 0;
        mBatteryState.init();
        level = mBatteryState.getLevel();
        mBatteryState.deinit();
        return level;
    }

    /**
     * get battery scale
     * 
     * @return integer containing the maximum battery level
     */
    public int getBatteryScale() {
        int scale = 0;
        mBatteryState.init();
        scale = mBatteryState.getScale();
        mBatteryState.deinit();
        return scale;
    }

    /**
     * get resource id of battery small icon
     * 
     * @return integer containing the resource ID of a small status bar icon
     *         indicating the current battery state
     */
    public int getBatteryIconsmall() {
        int icon_small = 0;
        mBatteryState.init();
        icon_small = mBatteryState.getIcon_small();
        mBatteryState.deinit();
        return icon_small;
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
        String plugged = "";
        mBatteryState.init();
        plugged = mBatteryState.getPlugged();
        mBatteryState.deinit();
        return plugged;
    }

    /**
     * get battery voltage
     * 
     * @return integer containing the current battery voltage level
     */
    public int getBatteryVoltage() {
        int voltage = 0;
        mBatteryState.init();
        voltage = mBatteryState.getVoltage();
        mBatteryState.deinit();
        return voltage;
    }

    /**
     * get battery temperature
     * 
     * @return integer containing the current battery temperature
     */
    public int getBatteryTemperature() {
        int temperature = 0;
        mBatteryState.init();
        temperature = mBatteryState.getTemperature();
        mBatteryState.deinit();
        return temperature;
    }

    /**
     * get battery technology
     * 
     * @return string describing the technology of the current battery
     */
    public String getBatteryTechnology() {
        String technology = "";
        mBatteryState.init();
        technology = mBatteryState.getTechnology();
        mBatteryState.deinit();
        return technology;
    }

    /**
     * get bluetooth address
     * 
     * @return 1. string of bluetooth addresss<br/>
     *         2. "device not BT capable"<br/>
     *         3. "Unavailable"<br/>
     */
    public String getBlueToothAddress() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();

        if (bluetooth == null) {
            return "device not BT capable";
        }

        String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
        return TextUtils.isEmpty(address) ? "Unavailable" : address;
    }

    /**
     * get the build version
     * 
     * @return the build version
     */
    public String getBuildVersion() {
        return Build.VERSION.RELEASE;
    }

    /**
     * get baseband version
     * 
     * e.g. "32.41.00.32H_5.08.00.04"
     */
    public String getBaseBandVersion() {
        return SystemProperties.get("gsm.version.baseband", "Unknown");
    }

    /**
     * get device model
     * 
     * Full BMOS on Passion
     */
    public String getDeviceModel() {
        return Build.MODEL;
    }

    /**
     * get build number
     * 
     * @return string <br/>
     *         e.g "full_passion-userdebug 2.3.3 GRI40 1.2.23-11395 test-keys"
     */
    public String getBuildNumber() {
        return Build.DISPLAY;
    }

    /**
     * get the kernel version
     * 
     * @return the kernel version
     */
    public String getKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX = "\\w+\\s+" + /* ignore: Linux */
            "\\w+\\s+" + /* ignore: version */
            "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
            "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
            "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
            "([^\\s]+)\\s+" + /* group 3: #26 */
            "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
            "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.print("Regex did not match on /proc/version: " + procVersionStr);
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                Log.print("Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "Unavailable";
            } else {
                return (new StringBuilder(m.group(1)).append('\n').append(m.group(2)).append(' ')
                        .append(m.group(3)).append('\n').append(m.group(4))).toString();
            }
        } catch (IOException e) {
            Log.print("IO Exception when getting kernel version for Device Info screen\n" + e);

            return "Unavailable";
        }
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void recordVideo() {

    }

    /**
     * add a contact
     * 
     * @param name
     *            : the contact's show name
     * @param phone
     *            : the contact's phone number
     * @return the uri string of the contact
     */
    public String addContact(String name, String phone) {

        ContentValues values = new ContentValues();
        values.put(People.NAME, name);
        Uri contactUri = mContext.getContentResolver().insert(People.CONTENT_URI, values);
        Uri numberUri = Uri.withAppendedPath(contactUri, People.Phones.CONTENT_DIRECTORY);
        values.clear();

        values.put(Contacts.Phones.TYPE, People.Phones.TYPE_MOBILE);
        values.put(People.NUMBER, phone);
        mContext.getContentResolver().insert(numberUri, values);
        return contactUri.toString();
    }

    /**
     * delete a contact with the uri
     * 
     * @param uriStr
     *            : the contact's uri string
     * @return the changed record number in database
     */
    public int deleteContact(String uriStr) {
        return mContext.getContentResolver().delete(Uri.parse(uriStr), null, null);

    }

    /**
     * @param command
     *            "rm mnt/sdcard/2.txt" "cp data/app/*.* mnt/sdcard/."
     *            "dumpsys cpuinfo" "cat /proc/version"
     *            "am instrument -w com.example.android.apis.tests/android.test.InstrumentationTestRunner"
     * 
     * @return
     */
    private boolean shell(String command) {
        String[] params = { "/system/xbin/sh", "-c", "" };
        params[2] = command;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(params);

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            String result = "\n";
            while ((line = in.readLine()) != null) {
                result += line + "\n";
            }
            Log.print("command result : " + result);
            process.waitFor();
        } catch (Exception e) {
            Log.print("Unexpected error - Here is what I know: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * NOT READY YET, COMING SOON...
     * 
     * @return
     */
    public boolean cp() {
        return false;
    }

    /**
     * NOT READY YET, COMING SOON...
     * 
     * @return
     */
    public boolean rm() {
        return false;
    }

    /**
     * NOT READY YET, COMING SOON...
     * 
     * @return
     */
    public boolean mv() {
        return false;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void playVideo() {
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
        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audio.getMode(); //MODE_NORMAL, MODE_RINGTONE, MODE_IN_CALL, MODE_IN_COMMUNICATION
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
        return mAudioManager.getStreamVolume(streamType);
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
        return mAudioManager.getRingerMode();
    }

    /**
     * check if any music is active
     * 
     * @return true if any music tracks are active
     */
    public boolean isMusicActive() {
        return mAudioManager.isMusicActive();
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
        mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
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
        mAudioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI);
    }

    /**
     * set audio mute on
     * 
     * @param streamType
     */
    public void setAudioMuteOn(int streamType) {
        mAudioManager.setStreamMute(streamType, true);
    }

    /**
     * set audio mute off
     * 
     * @param streamType
     */
    public void setAudioMuteOff(int streamType) {
        mAudioManager.setStreamMute(streamType, false);
    }

    /**
     * get available internal memory
     * 
     * @return byte value, can be formated to string by formatSize(long size)
     *         function<br/>
     *         e.g. 135553024
     */
    public long getMemoryInternalAvail() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void playAudio() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void makeCall() {

    }

    /**
     * make system go to sleep
     */
    public void goToSleep() {
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
    }

    /**
     * check whether the screen is on
     * 
     * @return true if the screen is on
     */
    public boolean isScreenOn() {
        return mPowerManager.isScreenOn();
    }

    /**
     * reboot system
     */
    public void reboot() {
        mPowerManager.reboot(null);
    }

    /**
     * reboot system to recovery mode
     */
    public void rebootToRecoveryMode() {
        mPowerManager.reboot("recovery");
    }

    /**
     * reboot system to bootloader
     */
    public void rebootToBootloader() {
        mPowerManager.reboot("bootloader");
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getSensorState() {
        return -1;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getTouchModeState() {
        return -1;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getVibrationState() {
        return -1;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setSensorOff() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setSensorOn() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setVibrationOff() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setVibrationOn() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void sendSms() {

    }

    /**
     * check whether storage card is valid
     * 
     * @return true if is valid
     */
    public boolean isStorageCardValid() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * check whether storage card is read only
     * 
     * @return true if is read only
     */
    public boolean isStorageCardReadOnly() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }

    /**
     * write a line of string to a file in the sdcard
     * 
     * @param filename
     *            : file name in the sdcard to write
     * @param line
     *            : string to write
     */
    // TODO: NEED TO CHECK
    public void writeLineToSdcard(String filename, String line) {
        final String outputFile = Environment.getExternalStorageDirectory().toString() + "/"
                + filename;
        try {
            FileWriter fstream = null;
            fstream = new FileWriter(outputFile, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(line + "\n");
            out.close();
            fstream.close();
            Log.print("write log: " + outputFile);
        } catch (Exception e) {
            Log.print("exception for write log");
        }
    }

    /**
     * get storage card total size
     * 
     * @return byte value, can be formated to string by formatSize(long size)
     *         function
     */
    public long getStorageCardSize() {
        long size = 0;
        String status = Environment.getExternalStorageState();

        if (status.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long totalBlocks = stat.getBlockCount();

                size = totalBlocks * blockSize;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    /**
     * get storage card available size
     * 
     * @return byte value, can be formated to string by formatSize(long size)
     *         function
     */
    public long getStorageCardAvail() {
        long size = 0;
        String status = Environment.getExternalStorageState();

        if (status.equals(Environment.MEDIA_MOUNTED)) {
            try {
                File path = Environment.getExternalStorageDirectory();
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long availableBlocks = stat.getAvailableBlocks();

                size = availableBlocks * blockSize;

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return size;
    }

    /**
     * check whether there is an application running in sdcard
     * 
     * @return true if has
     */
    public boolean hasAppsAccessingStorage() {
        try {
            String extStoragePath = Environment.getExternalStorageDirectory().toString();
            IMountService mountService = getMountService();
            int stUsers[] = mountService.getStorageUsers(extStoragePath);
            if (stUsers != null && stUsers.length > 0) {
                return true;
            }
            List<ApplicationInfo> list = mActivityManager.getRunningExternalApplications();
            if (list != null && list.size() > 0) {
                return true;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    private synchronized IMountService getMountService() {
        if (mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.print("Can't get mount service");
            }
        }
        return mMountService;
    }

    /**
     * mount external storage
     */
    public void mount() {
        IMountService mountService = getMountService();
        try {
            if (mountService != null) {
                mountService.mountVolume(Environment.getExternalStorageDirectory().toString());
            } else {
                Log.print("Mount service is null, can't mount");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * unmount external storage
     */
    public void unmount() {
        boolean force = true;
        IMountService mountService = getMountService();
        String extStoragePath = Environment.getExternalStorageDirectory().toString();
        //        try {
        //            mountService.unmountVolume(extStoragePath, force);
        //        } catch (RemoteException e) {
        //            e.printStackTrace();
        //        }
    }

    /**
     * NOT READY YET, COMING SOON...
     * 
     * @return
     */
    public String getClipBoardData() {
        return "";
    }

    /**
     * get the pixels of screens's width
     * 
     * @return the pixels of screens's width
     */
    public int getDisplayX() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * get the pixels of screens's height
     * 
     * @return the pixels of screens's width
     */
    public int getDisplayY() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(dm);
        return dm.heightPixels;
    }

    /**
     * get screen's brightness
     * 
     * @return screen's brightness
     */
    public int getScreenBrightness() {
        try {
            return Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * get screen brightness mode
     * 
     * @return screen brightness mode
     */
    public int getScreenBrightnessMode() {
        try {
            return Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * set screen brightness mode
     * 
     * @param mode
     *            : expected screen brightness mode
     */
    public void setScreenBrightnessMode(int mode) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    /**
     * set location provider enabled
     * 
     * @param provider
     * @param enabled
     */
    public void setLocationProviderEnabled(String provider, boolean enabled) {
        Settings.Secure
                .setLocationProviderEnabled(mContext.getContentResolver(), provider, enabled);
    }

    /**
     * check whether the location provider is enabled
     * 
     * @param provider
     * @return true if enabled
     */
    public boolean isLocationProviderEnabled(String provider) {
        return Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(), provider);
    }

    /**
     * check whether the accelerometer rotation is enabled
     * 
     * @return true if enabled
     */
    public boolean isAccelerometerRotationEnabled() {
        try {
            int accelerometerRotationEnabled = Settings.System.getInt(
                    mContext.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
            return accelerometerRotationEnabled == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * set accelerometer rotation
     * 
     * @param enabled
     */
    public void setAccelerometerRotationEnabled(boolean enabled) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }

    /**
     * get background data state
     * 
     * @return
     */
    public boolean getBackgroundDataState() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getBackgroundDataSetting();
    }

    /**
     * set background data
     * 
     * @param enabled
     */
    public void setBackgroundDataSetting(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setBackgroundDataSetting(enabled);
        try {
            // wait for setBackgroundDataSetting completed
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * get master sync automatically state
     * 
     * @return true if enabled
     */
    public boolean getMasterSyncAutomatically() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    /**
     * set master sync automatically state
     * 
     * @param sync
     */
    public void setMasterSyncAutomatically(boolean sync) {
        ContentResolver.setMasterSyncAutomatically(sync);
    }

    /**
     * NOT READY YET, COMING SOON...
     * 
     * @return
     */
    public String getSystemEnv() {
        return "";
    }

    /**
     * get system time
     * 
     * e.g. "Nov 29, 2011 4:46:28 PM"
     */
    public String getSystemTime() {
        return DateFormat.getDateTimeInstance().format(new Date().getTime());
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setAlarmClock() {

    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setClipBoardData() {
    }

    /**
     * set screen brightness
     * 
     * @param brightness
     *            , 0 ~ 255
     */
    public void setScreenBrightness(int brightness) {
        ((PowerManager) mContext.getSystemService(Context.POWER_SERVICE))
                .setBacklightBrightness(brightness);
    }

    /**
     * set system time
     * 
     * @param time
     *            , format "yyyy/MM/dd hh:mm:ss", e.g. "2011/11/25 17:30:00"
     */
    public void setSystemTime(String time) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
            Date temp = df.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(temp);

            SystemClock.setCurrentTimeMillis(cal.getTimeInMillis());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * get my phone number
     */
    public String getMyPhoneNumber() {
        return ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .getLine1Number();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getCallState() {
        return "";
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getContactsState() {
        return "";
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getImei() {
        //NOTE "imei" is the "Device ID" since it represents the IMEI in GSM and the MEID in CDMA
        //    return mPhone.getPhoneName().equals("CDMA") ? mPhone.getMeid() : mPhone.getDeviceId();
        return "";
    }

    /**
     * get current network type
     * 
     * @return EDGE, UMTS, etc...
     */
    public String getNetworkType() {
        // Whether EDGE, UMTS, etc...
        return SystemProperties.get(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, "Unknown");
    }

    /**
     * get current data state
     * 
     * @return "unknown", "Connected", "Suspended", "Connecting" or
     *         "Disconnected"
     */
    public String getDataState() {
        int state = mTelephonyManager.getDataState();
        String display = "unknown";

        switch (state) {
        case TelephonyManager.DATA_CONNECTED:
            display = "Connected";
            break;
        case TelephonyManager.DATA_SUSPENDED:
            display = "Suspended";
            break;
        case TelephonyManager.DATA_CONNECTING:
            display = "Connecting";
            break;
        case TelephonyManager.DATA_DISCONNECTED:
            display = "Disconnected";
            break;
        }

        return display;
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getOperatorState() {
        return "";
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
        return ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .getSimState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getSmsState() {
        return "";
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getFlightModeState() {
        return 0;
    }

    /**
     * set airplane mode
     * 
     * @param enable
     *            true means open; false means closed
     */
    public void setAirplaneMode(boolean enable) {
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON,
                enable ? 1 : 0);

        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enable);
        mContext.sendBroadcast(intent);
    }

    /**
     * set mobile data disabled
     */
    public void setDataConnectionDisabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(false);
    }

    /**
     * set mobile data enabled
     */
    public void setDataConnectionEnabled() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setMobileDataEnabled(true);
    }

    /**
     * formats a content size to be in the form of bytes, kilobytes, megabytes,
     * etc<br/>
     * 
     * @return e.g. formatSize(123456789) = "118MB"
     */

    public String formatSize(long size) {
        return Formatter.formatFileSize(mContext, size);
    }

    /**
     * get current wifi mac address
     */
    public String getWlanMacAddress() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        return !TextUtils.isEmpty(macAddress) ? macAddress : "Unavailable";
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
        return mWifiManager.getWifiState();
    }

    /**
     * check if wifi is currently enabled
     * 
     * @return true if enabled
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * set wifi disabled
     * 
     * @return true if set success
     */
    public boolean setWifiDisabled() {
        return mWifiManager.setWifiEnabled(false);
    }

    /**
     * set wifi enabled
     * 
     * @return true if set sucess
     */
    public boolean setWifiEnabled() {
        return mWifiManager.setWifiEnabled(true);
    }

    /**
     * set wifi disconnect
     * 
     * @return true if set sucess
     */
    public boolean setWifiDisconnect() {
        return mWifiManager.disconnect();
    }

    /**
     * set wifi reconnect
     * 
     * @return true if set sucess
     */
    public boolean setWifiReconnect() {
        return mWifiManager.reconnect();
    }

    /**
     * set wifi starting scanning now
     * 
     * @return true if set sucess
     */
    public boolean setWifiStartScan() {
        return mWifiManager.startScan();
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
    @Deprecated
    public String getServerIP() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE_NAME,
                Activity.MODE_PRIVATE);
        String str = sharedPreferences.getString("serverip", "");
        if (str.equals(""))
            return null;
        return str;
    }

    /**
     * get top activity
     * 
     * @return the name of top activity. ex. "com.baidu.baiduclock.BaiduClock"
     */
    public String getTopActivity() {
        return mActivityManager.getRunningTasks(1).get(0).topActivity.getClassName();
    }

    /**
     * get top activity's package name
     * 
     * @return e.g. "com.baidu.baiduclock"
     */
    public String getTopPackage() {
        return mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
    }

    /**
     * @param className
     *            ex. "com.baidu.baiduclock.BaiduClock"
     * @param timeout
     *            delay. in terms of milliseconds
     * @return true we got it
     */
    public boolean waitforTopActivity(String className, long timeout) {
        boolean flag = false;
        final long endTime = System.currentTimeMillis() + timeout;
        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                flag = false;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (className.equals(getTopActivity())) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * factory reset system with erase sdcard
     */
    public void factoryResetWithEraseSD() {
        Intent intent = new Intent(
                com.android.internal.os.storage.ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
        intent.setComponent(com.android.internal.os.storage.ExternalStorageFormatter.COMPONENT_NAME);
        mContext.startService(intent);
    }

    /**
     * change system language
     * 
     * @param language
     *            : language expected to change
     */
    public void changeLanguage(String language) {
        IActivityManager am = ActivityManagerNative.getDefault();
        Configuration config;
        try {
            config = am.getConfiguration();

            if ("zh".equals(language)) {
                config.locale = Locale.CHINA;
            } else {
                config.locale = Locale.US;
            }
            config.userSetLocale = true;

            am.updateConfiguration(config);
            BackupManager.dataChanged("com.android.providers.settings");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * check bluetooth enabled or not
     * 
     * @return true enabled false disabled
     * 
     */
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null)
            return false;//"device not BT capable";
        return bluetooth.isEnabled();
    }

    /**
     * set bluetooth state
     * 
     * @param true enabled false disabled
     * 
     */
    public void setBluetoothState(boolean enabled) {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null)
            return; //"device not BT capable";
        if (enabled) {
            bluetooth.enable();
        } else {
            bluetooth.disable();
        }
    }

    /**
     * set screen time out
     * 
     * @param milisecond
     */
    public void setScreenTimeOut(int milisecond) {
        try {
            android.provider.Settings.System.putInt(mContext.getContentResolver(),
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT, milisecond);
        } catch (NumberFormatException e) {
            Log.print("could not persist screen timeout setting :" + e);
        }
    }

    /**
     * set screen unlock security none
     */
    public void setScreenUnlockSecurityNone() {
        //        try {
        //            new LockPatternUtils(mContext).clearLock();
        //        } catch (Exception e) {
        //            //            e.printStackTrace();
        //        }
    }

    /**
     * set screen stay awake
     * 
     * @param isAwake
     *            : true if want to keep screen awake
     */
    public void setScreenStayAwake(boolean isAwake) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.STAY_ON_WHILE_PLUGGED_IN,
                isAwake ? (BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB)
                        : 0);
    }

    /**
     * format sdcard
     */
    public void formatSD() {
        Intent intent = new Intent(ExternalStorageFormatter.FORMAT_ONLY);
        intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
        mContext.startService(intent);
    }

    boolean        isinstallapkfinish = false;
    public boolean isregister         = false;

    /**
     * you must use "isPackageInstalled(String)" to judge if it is installed
     * finish. otherwise you can use public void InstallapkSync(String filename)
     * */
    public void installApk(String filename) {
        // systemLib.Installapk(filename);
        //        if (isregister == false) {
        //            isregister = true;
        //            IntentFilter intentFilter = new IntentFilter(MyIntent.ACTION_INSTALL_BEGIN);
        //            intentFilter.addAction(MyIntent.ACTION_INSTALL_END);
        //            mContext.registerReceiver(mReceiver, intentFilter);
        //        }
        //        //start the service
        //        Intent startservice = new Intent();
        //        startservice.setAction(MyIntent.ACTION_PROXY);
        //        startservice.putExtra(MyIntent.EXTRA_OPERATION, MyIntent.EXTRA_INSTALL);
        //        startservice.putExtra(MyIntent.EXTRA_ARG1, filename);
        //        Log.print("startservice intent is " + startservice);
        //        mContext.startService(startservice);
    }

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
                                                 @Override
                                                 public void onReceive(Context context,
                                                         Intent intent) {
                                                     if (intent.getAction().equals(
                                                             MyIntent.ACTION_INSTALL_BEGIN)) {
                                                         //  mediascantext.setText("Media Scanner started scanning " + intent.getData().getPath());
                                                         isinstallapkfinish = false;
                                                         Log.print("begin to install apk");
                                                     } else if (intent.getAction().equals(
                                                             MyIntent.ACTION_INSTALL_END)) {
                                                         //  mediascantext.setText("Media Scanner finished scanning " + intent.getData().getPath());  
                                                         //          sendBroadcast(new Intent("com.baidu.RES_SCAN_COMPLETED"));
                                                         isinstallapkfinish = true;
                                                         Log.print("end to install apk");

                                                     }
                                                 }
                                             };

    /**
     * check whether an app is installed or not
     * 
     * @param packageName
     *            : package name
     * @return true if installed already
     */
    public boolean isPackageInstalled(String packageName) {
        boolean flag = false;
        List<PackageInfo> packageInfoList = mPackageManager.getInstalledPackages(0);
        for (int i = 0; i < packageInfoList.size(); i++) {
            if (packageName.equals(packageInfoList.get(i).packageName)) {
                flag = true;
                break;
            }
        }

        if (isregister) {
            mContext.unregisterReceiver(mReceiver);
            isregister = false;
        }

        return flag;
    }

    /**
     * @param filename
     * @param timeout
     * @return
     */
    public boolean installApkSync(String filename, long timeout) {
        boolean ret = false;
        if (!isregister) {
            isregister = true;
            IntentFilter intentFilter = new IntentFilter(MyIntent.ACTION_INSTALL_BEGIN);
            intentFilter.addAction(MyIntent.ACTION_INSTALL_END);
            mContext.registerReceiver(mReceiver, intentFilter);
        }
        isinstallapkfinish = false;
        //start the service
        Intent startservice = new Intent();
        startservice.setAction(MyIntent.ACTION_PROXY);
        startservice.putExtra(MyIntent.EXTRA_OPERATION, MyIntent.EXTRA_INSTALL);
        startservice.putExtra(MyIntent.EXTRA_ARG1, filename);
        Log.print("startservice intent is " + startservice);
        mContext.startService(startservice);

        final long endTime = System.currentTimeMillis() + timeout;
        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                ret = false;
                break;
            }
            if (isinstallapkfinish) {
                ret = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * uninstall an app by its package name
     * 
     * @param packageName
     *            : package name
     */
    public void uninstallApk(String packageName) {
        mPackageManager.deletePackage(packageName, null, 0);
    }

    /**
     * set system properties
     * 
     * @param key
     * @param val
     */
    public void setSystemProperties(String key, String val) {
        // Whether EDGE, UMTS, etc...
        SystemProperties.set(key, val);
    }

    /**
     * get system properties : EDGE, UMTS, etc...
     * 
     * @param key
     * @return system properties
     */
    public String getSystemProperties(String key) {
        // Whether EDGE, UMTS, etc...
        return SystemProperties.get(key);
    }

    /**
     * get non-market-apps-allowed settings enabled or disabled
     * 
     * @return true if setting is enabled
     */
    public int getNonMarketAppsAllowed() {
        int type = -1;
        try {
            type = Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.INSTALL_NON_MARKET_APPS);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return type;
    }

    /**
     * set non-market-apps-allowed settings
     * 
     * @param enabled
     */
    public void setNonMarketAppsAllowed(boolean enabled) {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.INSTALL_NON_MARKET_APPS, enabled ? 1 : 0);
        } catch (Exception e) {
            // eat it 
        }
    }

    /**
     * clear an application's user data by the packagename
     * 
     * @param packageName
     * @return true if clear success
     */
    public boolean clearApplicationUserData(String packageName) {
        return mActivityManager.clearApplicationUserData(packageName, new ClearUserDataObserver());
    }

    class ClearUserDataObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            Log.print("ClearUserDataObserver Of Cafe");
            //final Message msg = mHandler.obtainMessage(CLEAR_USER_DATA);
            //msg.arg1 = succeeded?OP_SUCCESSFUL:OP_FAILED;
            //mHandler.sendMessage(msg);
        }
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
        Log.print("PackageName:" + packageName);
        Log.print("permission name:" + permissionName);

        // baidu only
        //        CPServiceManager.getInstance(mContext).updatePackagePermission(packageName, permissionName, state);
    }

    /**
     * get package's pemissions
     * 
     * @param packageName
     *            target package name
     * @return permissions string
     */
    public String[] getPermissionsForPackage(String packageName) {
        try {
            return mPackageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
        } catch (NameNotFoundException e) {
            Log.print("Could'nt retrieve permissions for package:" + packageName);
            return null;
        }
    }

    /**
     * clear the output of logcat
     */
    public void clearLog() {
        try {
            Runtime.getRuntime().exec(new String[] { "logcat", "-c" });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get the logs of executing the given command
     * 
     * @param command
     *            the command to be executed
     * @return the logs got
     */
    public String[] getLog(String[] command) {
        ArrayList<String> logLines = new ArrayList<String>();
        Process mLogcatProc = null;
        BufferedReader reader = null;
        if (null == command) {
            return null;
        }
        try {
            mLogcatProc = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                logLines.add(line);
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            return logLines.toArray(new String[logLines.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * delete an account
     * 
     * @param name
     *            : account name
     * @param type
     *            : account type
     */
    public void deleteAccount(String name, String type) {
        AccountManager am = (AccountManager) mContext.getSystemService(Context.ACCOUNT_SERVICE);
        Account mAccount = new Account(name, type);
        am.removeAccount(mAccount, new AccountManagerCallback<Boolean>() {
            public void run(AccountManagerFuture<Boolean> future) {
                boolean failed = true;
                try {
                    if (future.getResult()) {
                        failed = false;
                    }
                } catch (OperationCanceledException e) {
                    // handled below
                } catch (IOException e) {
                    // handled below
                } catch (AuthenticatorException e) {
                    // handled below
                }
            }
        }, null);
    }

    boolean isscreencapfinish = false;

    /**
     * take a screen capture at "/mnt/sdcard/DCIM/ScreenShot"
     * 
     * @param prefix
     *            means prefix string of png
     */
    public void screenCap(String prefix) {
        boolean ret = false;
        IntentFilter intentFilter = new IntentFilter(MyIntent.ACTION_SCREENCAP_BEGIN);
        intentFilter.addAction(MyIntent.ACTION_SCREENCAP_END);
        mContext.registerReceiver(mscreencapReceiver, intentFilter);
        isscreencapfinish = false;

        //start the service
        Intent startservice = new Intent();
        startservice.setAction(MyIntent.ACTION_PROXY);
        startservice.putExtra(MyIntent.EXTRA_OPERATION, MyIntent.EXTRA_SCREENCAP);
        startservice.putExtra(MyIntent.EXTRA_ARG1, prefix);

        Log.print("startservice intent is " + startservice);
        mContext.startService(startservice);
        long timeout = 3000;
        final long endTime = System.currentTimeMillis() + timeout;
        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                ret = false;
                break;
            }
            if (isscreencapfinish) {
                ret = true;
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.print("ret is " + ret + "false means screencap failed!");
        mContext.unregisterReceiver(mscreencapReceiver);
    }

    public final BroadcastReceiver mscreencapReceiver = new BroadcastReceiver() {
                                                          @Override
                                                          public void onReceive(Context context,
                                                                  Intent intent) {
                                                              if (intent
                                                                      .getAction()
                                                                      .equals(MyIntent.ACTION_SCREENCAP_BEGIN)) {
                                                                  //  mediascantext.setText("Media Scanner started scanning " + intent.getData().getPath());
                                                                  isscreencapfinish = false;
                                                                  Log.print("begin to screencap");
                                                              } else if (intent
                                                                      .getAction()
                                                                      .equals(MyIntent.ACTION_SCREENCAP_END)) {
                                                                  //  mediascantext.setText("Media Scanner finished scanning " + intent.getData().getPath());  
                                                                  //          sendBroadcast(new Intent("com.baidu.RES_SCAN_COMPLETED"));
                                                                  isscreencapfinish = true;
                                                                  Log.print("end to screencap");

                                                              }
                                                          }
                                                      };

    /**
     * @return height of status bar
     */
    public int getStatusBarHeight() {
        int statusBarHeight = 0;
        try {
            Log.print("Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= 14) {// API Level: 14. Android 4.0

            } else {
                statusBarHeight = mContext.getResources().getDimensionPixelSize(
                        com.android.internal.R.dimen.status_bar_height);
            }
        } catch (Exception e) {
            // When com.android.internal.R.dimen.status_bar_height can not be found at Android 4.0, eat the exception
            // It will be handle by 
        }

        return statusBarHeight;
    }

    public void recovery() {
        File RECOVERY_DIR = new File("/cache/recovery");
        File COMMAND_FILE = new File(RECOVERY_DIR, "command");
        File LOG_FILE = new File(RECOVERY_DIR, "log");
        RECOVERY_DIR.mkdirs(); // In case we need it
        COMMAND_FILE.delete(); // In case it's not writable
        LOG_FILE.delete();
        FileWriter command = null;
        try {
            command = new FileWriter(COMMAND_FILE);
            command.write("--wipe_data");
            command.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (command != null) {
                try {
                    command.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mPowerManager.reboot("recovery");
        //        mContext.sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));

        // non-android env
        //        IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
        //        try {
        //            pm.reboot("recovery");
        //        } catch (RemoteException e) {
        //            e.printStackTrace();
        //        }
    }

    public int getCurrentTaskActivitiesNumber() {
        return mActivityManager.getRunningTasks(1).get(0).numActivities;
    }

    public static final Uri APN_TABLE_URI = Uri.parse("content://telephony/carriers");

    /*
     * Insert a new APN entry into the system APN table
     * Require an apn name, and the apn address. More can be added.
     * Return an id (_id) that is automatically generated for the new apn entry.
     */
    @Deprecated
    public int insertAPN(String name, String apn_addr, String proxy, String port) {
        int id = -1;
        //        ContentResolver resolver = mContext.getContentResolver();
        //        ContentValues values = new ContentValues();
        //        values.put("name", name);
        //        values.put("apn", apn_addr);
        //        values.put(Telephony.Carriers.PROXY, proxy);
        //        values.put(Telephony.Carriers.PORT, port);
        //
        //        /*
        //         * The following three field values are for testing in Android emulator only
        //         * The APN setting page UI will ONLY display APNs whose 'numeric' filed is
        //         * TelephonyProperties.PROPERTY_SIM_OPERATOR_NUMERIC.
        //         * On Android emulator, this value is 310260, where 310 is mcc, and 260 mnc.
        //         * With these field values, the newly added apn will appear in system UI.
        //         */
        //        values.put("mcc", "310");
        //        values.put("mnc", "260");
        //        values.put("numeric", "310260");
        //
        //        Cursor c = null;
        //        try {
        //            resolver.delete(APN_TABLE_URI, "_id=?", null);
        //            Uri newRow = resolver.insert(APN_TABLE_URI, values);
        //            if (newRow != null) {
        //                c = resolver.query(newRow, null, null, null, null);
        //                Log.print("Newly added APN:");
        //                //                printAllData(c); //Print the entire result set
        //
        //                // Obtain the apn id
        //                int idindex = c.getColumnIndex("_id");
        //                c.moveToFirst();
        //                id = c.getShort(idindex);
        //                Log.print("New ID: " + id + ": Inserting new APN succeeded!");
        //                if (setDefaultAPN(id)) {
        //                    Log.print("Set apn to default success!");
        //                } else {
        //                    Log.print("Set apn to default fail!");
        //                }
        //            }
        //        } catch (SQLException e) {
        //            Log.print(e.getMessage());
        //        }
        //
        //        if (c != null)
        //            c.close();
        return id;
    }

    public static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

    /*
     * Set an apn to be the default apn for web traffic
     * Require an input of the apn id to be set
     */
    public boolean setDefaultAPN(int id) {
        boolean res = false;
        ContentResolver resolver = mContext.getContentResolver();
        ContentValues values = new ContentValues();

        //See /etc/apns-conf.xml. The TelephonyProvider uses this file to provide
        //content://telephony/carriers/preferapn URI mapping
        values.put("apn_id", id);
        try {
            resolver.update(PREFERRED_APN_URI, values, null, null);
            Cursor c = resolver.query(PREFERRED_APN_URI, new String[] { "name", "apn" }, "_id="
                    + id, null, null);
            if (c != null) {
                res = true;
                c.close();
            }
        } catch (SQLException e) {
            Log.print(e.getMessage());
        }
        return res;
    }

    /**
     * @return true means enabled; false menas disabled
     */
    public boolean isAdbEnabled() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.Secure.ADB_ENABLED) == 0 ? false : true;
        } catch (Exception e) {
            // eat it
        }
        return false;
    }

    public void setAdbEnabled(boolean enabled) {
        try {
            Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.ADB_ENABLED,
                    enabled ? 1 : 0);
        } catch (Exception e) {
            // eat it
        }
    }

    /**
     * NOTICE:multi-invoking is allowed.
     */
    public void keepState() {
        if (LockActivity.keep_state_enable) {
            Log.print("keepState is working...");
            return;
        }

        LockActivity.keep_state_enable = true;
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        if (!isAdbEnabled()) {
                            setAdbEnabled(true); // need android.permission.WRITE_SECURE_SETTINGS
                        }
                        //                        setNonMarketAppsAllowed(true); // android.permission.WRITE_SECURE_SETTINGS
                        //                        if (isAirplaneModeOn()) {
                        setAirplaneMode(false);
                        //                        }
                        if (!isWifiEnabled()) {
                            setWifiEnabled();
                        }
                        if (isScreenLocked()) {
                            setScreenUnlocked();
                        }
                        if (!isScreenOn()) {
                            setScreenOn();
                        }
                        if (isLockPatternEnabled()) {
                            setScreenUnlockSecurityNone();
                        }
                        setScreenStayAwake(true);

                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "keepState").start();
    }

    public boolean isAirplaneModeOn() {
        try {
            return Settings.Secure.getInt(mContext.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON) == 0 ? false : true;
        } catch (Exception e) {
            // eat it
        }
        return false;
    }

    public static class TimeLocker {
        private final static int UNLOCK_TIME = 2 * 60 * 1000;

        private static boolean   shouldLock  = true;

        public static void unlock() {
            if (!shouldLock) {
                return;
            }
            shouldLock = false;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(UNLOCK_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    shouldLock = true;
                }
            }).start();
        }

        public static boolean shouldLock() {
            return shouldLock;
        }
    }

    /**
     * NOTICE:multi-invoking is allowed.
     */
    public void lockDangerousActivity(String unlockPassword) {
        if (LockActivity.lock_activity_enable) {
            Log.print("lockDangerousActivity is working...");
            return;
        }

        LockActivity.lock_activity_enable = true;
        LockActivity.unlockPassword = unlockPassword;
        final String[] activities = new String[] { "com.android.settings"/*packageName*/,
                "com.miui.uac.AppListActivity", "com.htc.android.psclient.RestoreUsbSettings",
                "com.baidu.android.ota.ui.UpdateSettings", "com.android.updater.UpdaterSettings",
                "com.android.updater.MainActivity",
                "com.android.settings.framework.activity.HtcSettings" };

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        String topActivity = getTopActivity();
                        if (topActivity != null) {
                            for (String activity : activities) {
                                if (TimeLocker.shouldLock() && topActivity.contains(activity)) {
                                    Intent intent = new Intent("com.baidu.cafe.remote.lockactivity");
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                    Thread.sleep(500);
                                }
                            }
                        }
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "lockDangerousActivity").start();
    }

    /**
     * @return list of package names
     */
    private List<String> getHomePackageNames() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<String> names = new ArrayList<String>();

        for (ResolveInfo resolveInfo : mPackageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY)) {
            names.add(resolveInfo.activityInfo.packageName);
            //            Log.print(resolveInfo.activityInfo.packageName);
        }
        return names;
    }

    /**
     * Judge whether top activity is home.
     */
    public boolean isHome() {
        return getHomePackageNames().contains(
                mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName());
    }

    public boolean isLockPatternEnabled() {
        boolean isLockPatternEnabled = false;
        try {
            isLockPatternEnabled = new LockPatternUtils(mContext).isLockPatternEnabled();
        } catch (Exception e) {
            // eat it
        }
        return isLockPatternEnabled;
    }

    private int          mStatsType   = BatteryStats.STATS_SINCE_CHARGED;
    private PowerProfile mPowerProfile;
    BatteryStatsImpl     mStats;
    IBatteryStats        mBatteryInfo;
    private long         mStatsPeriod = 0;
    // How much the apps together have left WIFI running.
    private long         mAppWifiRunning;

    private void create() {
        mAppWifiRunning = 0;
        mPowerProfile = new PowerProfile(mContext);
        mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batteryinfo"));
        load();
    }

    /**
     * Battery capacity in milliAmpHour (mAh).
     */
    public void processAppBatteryUsage() {
        create();

        SensorManager sensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        final int which = mStatsType;
        final int speedSteps = mPowerProfile.getNumSpeedSteps();
        final double[] powerCpuNormal = new double[speedSteps];
        final long[] cpuSpeedStepTimes = new long[speedSteps];
        for (int p = 0; p < speedSteps; p++) {
            powerCpuNormal[p] = mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE, p);
        }
        final double averageCostPerByte = getAverageDataCost();
        long uSecTime = mStats.computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, which);
        long appWakelockTime = 0;
        //        BatterySipper osApp = null;
        mStatsPeriod = uSecTime;
        SparseArray<? extends Uid> uidStats = mStats.getUidStats();
        final int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            Uid u = uidStats.valueAt(iu);
            double power = 0;
            double highestDrain = 0;
            String packageWithHighestDrain = null;
            //mUsageList.add(new AppUsage(u.getUid(), new double[] {power}));
            Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
            long cpuTime = 0;
            long cpuFgTime = 0;
            long wakelockTime = 0;
            long gpsTime = 0;
            if (processStats.size() > 0) {
                // Process CPU time
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent : processStats
                        .entrySet()) {
                    Log.print("Process name = " + ent.getKey());
                    Uid.Proc ps = ent.getValue();
                    final long userTime = ps.getUserTime(which);
                    final long systemTime = ps.getSystemTime(which);
                    final long foregroundTime = ps.getForegroundTime(which);
                    cpuFgTime += foregroundTime * 10; // convert to millis
                    final long tmpCpuTime = (userTime + systemTime) * 10; // convert to millis
                    int totalTimeAtSpeeds = 0;
                    // Get the total first
                    for (int step = 0; step < speedSteps; step++) {
                        cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(step, which);
                        totalTimeAtSpeeds += cpuSpeedStepTimes[step];
                    }
                    if (totalTimeAtSpeeds == 0)
                        totalTimeAtSpeeds = 1;
                    // Then compute the ratio of time spent at each speed
                    double processPower = 0;
                    for (int step = 0; step < speedSteps; step++) {
                        double ratio = (double) cpuSpeedStepTimes[step] / totalTimeAtSpeeds;
                        processPower += ratio * tmpCpuTime * powerCpuNormal[step];
                    }
                    cpuTime += tmpCpuTime;
                    power += processPower;
                    if (packageWithHighestDrain == null || packageWithHighestDrain.startsWith("*")) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    } else if (highestDrain < processPower && !ent.getKey().startsWith("*")) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    }
                }
                Log.print("Max drain of " + highestDrain + " by " + packageWithHighestDrain);
            }
            if (cpuFgTime > cpuTime) {
                if (cpuFgTime > cpuTime + 10000) {
                    Log.print("WARNING! Cputime is more than 10 seconds behind Foreground time");
                }
                cpuTime = cpuFgTime; // Statistics may not have been gathered yet.
            }
            power /= 1000;

            // Process wake lock usage
            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
            for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> wakelockEntry : wakelockStats
                    .entrySet()) {
                Uid.Wakelock wakelock = wakelockEntry.getValue();
                // Only care about partial wake locks since full wake locks
                // are canceled when the user turns the screen off.
                BatteryStats.Timer timer = wakelock.getWakeTime(BatteryStats.WAKE_TYPE_PARTIAL);
                if (timer != null) {
                    wakelockTime += timer.getTotalTimeLocked(uSecTime, which);
                }
            }
            wakelockTime /= 1000; // convert to millis
            appWakelockTime += wakelockTime;

            // Add cost of holding a wake lock
            power += (wakelockTime * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 1000;

            // Add cost of data traffic
            long tcpBytesReceived = u.getTcpBytesReceived(mStatsType);
            long tcpBytesSent = u.getTcpBytesSent(mStatsType);
            power += (tcpBytesReceived + tcpBytesSent) * averageCostPerByte;

            // Add cost of keeping WIFI running.
            long wifiRunningTimeMs = u.getWifiRunningTime(uSecTime, which) / 1000;
            mAppWifiRunning += wifiRunningTimeMs;
            power += (wifiRunningTimeMs * mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 1000;

            // Process Sensor usage
            Map<Integer, ? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            for (Map.Entry<Integer, ? extends BatteryStats.Uid.Sensor> sensorEntry : sensorStats
                    .entrySet()) {
                Uid.Sensor sensor = sensorEntry.getValue();
                int sensorType = sensor.getHandle();
                BatteryStats.Timer timer = sensor.getSensorTime();
                long sensorTime = timer.getTotalTimeLocked(uSecTime, which) / 1000;
                double multiplier = 0;
                switch (sensorType) {
                case Uid.Sensor.GPS:
                    multiplier = mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                    gpsTime = sensorTime;
                    break;
                default:
                    android.hardware.Sensor sensorData = sensorManager.getDefaultSensor(sensorType);
                    if (sensorData != null) {
                        multiplier = sensorData.getPower();
                        Log.print("Got sensor " + sensorData.getName() + " with power = "
                                + multiplier);
                    }
                }
                power += (multiplier * sensorTime) / 1000;
            }

            //            Log.print("UID " + u.getUid() + ": power=" + power);
            Log.print("PACKAGE " + packageWithHighestDrain + ": power=" + power);

            //            // Add the app to the list if it is consuming power
            //            if (power != 0 || u.getUid() == 0) {
            //                BatterySipper app = new BatterySipper(getActivity(), mRequestQueue, mHandler,
            //                        packageWithHighestDrain, DrainType.APP, 0, u,
            //                        new double[] {power});
            //                app.cpuTime = cpuTime;
            //                app.gpsTime = gpsTime;
            //                app.wifiRunningTime = wifiRunningTimeMs;
            //                app.cpuFgTime = cpuFgTime;
            //                app.wakeLockTime = wakelockTime;
            //                app.tcpBytesReceived = tcpBytesReceived;
            //                app.tcpBytesSent = tcpBytesSent;
            //                if (u.getUid() == Process.WIFI_UID) {
            //                    mWifiSippers.add(app);
            //                } else if (u.getUid() == Process.BLUETOOTH_GID) {
            //                    mBluetoothSippers.add(app);
            //                } else {
            //                    mUsageList.add(app);
            //                }
            //                if (u.getUid() == 0) {
            //                    osApp = app;
            //                }
            //            }
            //            if (u.getUid() == Process.WIFI_UID) {
            //                mWifiPower += power;
            //            } else if (u.getUid() == Process.BLUETOOTH_GID) {
            //                mBluetoothPower += power;
            //            } else {
            //                if (power > mMaxPower) mMaxPower = power;
            //                mTotalPower += power;
            //            }
            //            if (DEBUG) Log.i(TAG, "Added power = " + power);
        }

        // The device has probably been awake for longer than the screen on
        // time and application wake lock time would account for.  Assign
        // this remainder to the OS, if possible.
        //        if (osApp != null) {
        //            long wakeTimeMillis = mStats.computeBatteryUptime(
        //                    SystemClock.uptimeMillis() * 1000, which) / 1000;
        //            wakeTimeMillis -= appWakelockTime - (mStats.getScreenOnTime(
        //                    SystemClock.elapsedRealtime(), which) / 1000);
        //            if (wakeTimeMillis > 0) {
        //                double power = (wakeTimeMillis
        //                        * mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 1000;
        //                osApp.wakeLockTime += wakeTimeMillis;
        //                osApp.value += power;
        //                osApp.values[0] += power;
        //                if (osApp.value > mMaxPower) mMaxPower = osApp.value;
        //                mTotalPower += power;
        //            }
        //        }
    }

    private void load() {
        try {
            byte[] data = mBatteryInfo.getStatistics();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = com.android.internal.os.BatteryStatsImpl.CREATOR.createFromParcel(parcel);
            mStats.distributeWorkLocked(BatteryStats.STATS_SINCE_CHARGED);
        } catch (RemoteException e) {
            Log.print("RemoteException:" + e);
        }
    }

    private double getAverageDataCost() {
        final long WIFI_BPS = 1000000; // TODO: Extract average bit rates from system 
        final long MOBILE_BPS = 200000; // TODO: Extract average bit rates from system
        final double WIFI_POWER = mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600;
        final double MOBILE_POWER = mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) / 3600;
        final long mobileData = mStats.getMobileTcpBytesReceived(mStatsType)
                + mStats.getMobileTcpBytesSent(mStatsType);
        final long wifiData = mStats.getTotalTcpBytesReceived(mStatsType)
                + mStats.getTotalTcpBytesSent(mStatsType) - mobileData;
        final long radioDataUptimeMs = mStats.getRadioDataUptime() / 1000;
        final long mobileBps = radioDataUptimeMs != 0 ? mobileData * 8 * 1000 / radioDataUptimeMs
                : MOBILE_BPS;

        double mobileCostPerByte = MOBILE_POWER / (mobileBps / 8);
        double wifiCostPerByte = WIFI_POWER / (WIFI_BPS / 8);
        if (wifiData + mobileData != 0) {
            return (mobileCostPerByte * mobileData + wifiCostPerByte * wifiData)
                    / (mobileData + wifiData);
        } else {
            return 0;
        }
    }

    public void printPackagePowerUsage() {
        //PowerTutorConnector ptc = new PowerTutorConnector(mContext);
        //ptc.connectToPowerTutor();
    }

    public boolean isNetworkEnable() {
        boolean ret = false;
        try {
            URL url = new URL("http://www.baidu.com/");
            InputStream in = url.openStream();
            in.close();
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 
     * for cafe.jar:com.baidu.cafe.local.SnapshotHelper.pressPointer(Bitmap
     * bitmap, int x, int y)
     * 
     * @param dist
     */
    public void copyAssets(String dist) {
        copyFile(dist, "pointer.png");
        copyFile(dist, "WebElementRecorder.js");
    }

    private void copyFile(String dir, String target) {
        try {
            String dist = dir + "/" + target;
            InputStream fis = mContext.getAssets().open(target);
            FileOutputStream fos = new FileOutputStream(dist);
            byte[] buff = new byte[1024];
            int readed = -1;
            while ((readed = fis.read(buff)) > 0)
                fos.write(buff, 0, readed);
            fis.close();
            fos.close();
            new ShellExecute().execute("chmod 777 " + dist, "/", 200);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void expandStatusBar() {
        try {
            ReflectHelper
                    .invoke(mStatusBarManager, null, "expand", new Class[] {}, new Object[] {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
