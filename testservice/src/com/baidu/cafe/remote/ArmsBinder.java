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

import java.util.List;

import com.baidu.cafe.utils.Strings;

import android.app.Instrumentation;
import android.content.Context;

/**
 * This is a interface-class.
 * 
 * @author chengzhenyu@baidu.com
 * @date 2011-06-20
 */

public class ArmsBinder extends IRemoteArms.Stub {
    private SystemLib            mSystemLib            = null;
    private Context              mContext              = null;
    private Instrumentation      mInst                 = null;
    private ViewPropertyProvider mViewPropertyProvider = null;
    private UILib                mUILib                = null;

    ArmsBinder(Context context) {
        mSystemLib = new SystemLib(context);
        mContext = context;
        mViewPropertyProvider = new ViewPropertyProvider(mSystemLib);
        mUILib = new UILib(mViewPropertyProvider);
        mInst = new Instrumentation();
        setScreenReadyForTest();
    }

    public Instrumentation getInstrumentation() {
        return mInst;
    }

    private void setScreenReadyForTest() {
        mSystemLib.setScreenOn();
        mSystemLib.setScreenUnlocked();
        mSystemLib.setScreenStayAwake(true);
    }

    public String getAccountName() {
        return mSystemLib.getAccountName();
    }

    public String getAccountType() {
        return mSystemLib.getAccountType();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningActivities() {
        return mSystemLib.getAllRunningActivities();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getAllRunningServices() {
        return mSystemLib.getAllRunningServices();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOff() {
        mSystemLib.setInputMethodShowOff();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setInputMethodShowOn() {
        mSystemLib.setInputMethodShowOn();
    }

    public boolean isScreenLocked() {
        return mSystemLib.isScreenLocked();
    }

    public void setScreenLocked() {
        mSystemLib.setScreenLocked();
    }

    public void setScreenUnlocked() {
        mSystemLib.setScreenLocked();
    }

    public String getBatteryStatus() {
        return mSystemLib.getBatteryStatus();
    }

    public String getBatteryHealth() {
        return mSystemLib.getBatteryHealth();
    }

    public boolean getBatteryPresent() {
        return mSystemLib.getBatteryPresent();
    }

    public int getBatteryLevel() {
        return mSystemLib.getBatteryLevel();
    }

    public int getBatteryScale() {
        return mSystemLib.getBatteryScale();
    }

    public int getBatteryIconsmall() {
        return mSystemLib.getBatteryIconsmall();
    }

    public String getBatteryPlugged() {
        return mSystemLib.getBatteryPlugged();
    }

    public int getBatteryVoltage() {
        return mSystemLib.getBatteryVoltage();
    }

    public int getBatteryTemperature() {
        return mSystemLib.getBatteryTemperature();
    }

    public String getBatteryTechnology() {
        return mSystemLib.getBatteryTechnology();
    }

    public String getBlueToothAddress() {
        return mSystemLib.getBlueToothAddress();
    }

    public String getBuildVersion() {
        return mSystemLib.getBuildVersion();
    }

    public String getBaseBandVersion() {
        return mSystemLib.getBaseBandVersion();
    }

    public String getDeviceModel() {
        return mSystemLib.getDeviceModel();
    }

    public String getBuildNumber() {
        return mSystemLib.getBuildNumber();
    }

    public String getKernelVersion() {
        return mSystemLib.getKernelVersion();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void recordVideo() {
        mSystemLib.recordVideo();
    }

    public String addContact(String name, String phone) {
        return mSystemLib.addContact(name, phone);
    }

    public int deleteContact(String uriStr) {
        return mSystemLib.deleteContact(uriStr);
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public boolean cp() {
        return mSystemLib.cp();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public boolean rm() {
        return mSystemLib.rm();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public boolean mv() {
        return mSystemLib.mv();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void playVideo() {
        mSystemLib.playVideo();
    }

    public int getAudioMode() {
        return mSystemLib.getAudioMode();
    }

    public int getAudioVolume(int streamType) {
        return mSystemLib.getAudioVolume(streamType);
    }

    public int getRingtoneMode() {
        return mSystemLib.getRingtoneMode();
    }

    public boolean isMusicActive() {
        return mSystemLib.isMusicActive();
    }

    public void setAudioVolumeDown(int streamType) {
        mSystemLib.setAudioVolumeDown(streamType);
    }

    public void setAudioVolumeUp(int streamType) {
        mSystemLib.setAudioVolumeUp(streamType);
    }

    public void setAudioMuteOn(int streamType) {
        mSystemLib.setAudioMuteOn(streamType);
    }

    public void setAudioMuteOff(int streamType) {
        mSystemLib.setAudioMuteOff(streamType);
    }

    public long getMemoryInternalAvail() {
        return mSystemLib.getMemoryInternalAvail();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void playAudio() {
        mSystemLib.playAudio();
    }

    /**
     * TODO:NOT READY YET, COMING SOON...
     */
    public void makeCall() {
        mSystemLib.makeCall();
    }

    public void goToSleep() {
        mSystemLib.goToSleep();
    }

    public boolean isScreenOn() {
        return mSystemLib.isScreenOn();
    }

    public void reboot() {
        mSystemLib.reboot();
    }

    /**
     * reboot device to recovery mode
     */
    public void rebootToRecoveryMode() {
        mSystemLib.rebootToRecoveryMode();
    }

    /**
     * reboot device to bootloader
     */
    public void rebootToBootloader() {
        mSystemLib.rebootToBootloader();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getSensorState() {
        return mSystemLib.getSensorState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getTouchModeState() {
        return mSystemLib.getTouchModeState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getVibrationState() {
        return mSystemLib.getVibrationState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setSensorOff() {
        mSystemLib.setSensorOff();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setSensorOn() {
        mSystemLib.setSensorOn();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setVibrationOff() {
        mSystemLib.setVibrationOff();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setVibrationOn() {
        mSystemLib.setVibrationOn();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void sendSms() {
        mSystemLib.sendSms();
    }

    public boolean isStorageCardValid() {
        return mSystemLib.isStorageCardValid();
    }

    public boolean isStorageCardReadOnly() {
        return mSystemLib.isStorageCardReadOnly();
    }

    public void writeLineToSdcard(String filename, String line) {
        mSystemLib.writeLineToSdcard(filename, line);
    }

    public long getStorageCardSize() {
        return mSystemLib.getStorageCardSize();
    }

    public long getStorageCardAvail() {
        return mSystemLib.getStorageCardAvail();
    }

    public boolean hasAppsAccessingStorage() {
        return mSystemLib.hasAppsAccessingStorage();
    }

    public void mount() {
        mSystemLib.mount();
    }

    public void unmount() {
        mSystemLib.unmount();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getClipBoardData() {
        return mSystemLib.getClipBoardData();
    }

    public int getDisplayX() {
        return mSystemLib.getDisplayX();
    }

    public int getDisplayY() {
        return mSystemLib.getDisplayY();
    }

    public int getScreenBrightness() {
        return mSystemLib.getScreenBrightness();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getSystemEnv() {
        return mSystemLib.getSystemEnv();
    }

    public String getSystemTime() {
        return mSystemLib.getSystemTime();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setAlarmClock() {
        mSystemLib.setAlarmClock();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void setClipBoardData() {
        mSystemLib.setClipBoardData();
    }

    public void setScreenBrightness(int brightness) {
        mSystemLib.setScreenBrightness(brightness);
    }

    public void setSystemTime(String time) {
        mSystemLib.setSystemTime(time);
    }

    public String getMyPhoneNumber() {
        return mSystemLib.getMyPhoneNumber();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getCallState() {
        return mSystemLib.getCallState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getContactsState() {
        return mSystemLib.getContactsState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getImei() {
        return mSystemLib.getImei();
    }

    public String getNetworkType() {
        return mSystemLib.getNetworkType();
    }

    public String getDataState() {
        return mSystemLib.getDataState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getOperatorState() {
        return mSystemLib.getOperatorState();
    }

    public int getSimCardState() {
        return mSystemLib.getSimCardState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public String getSmsState() {
        return mSystemLib.getSmsState();
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public int getFlightModeState() {
        return mSystemLib.getFlightModeState();
    }

    public void setAirplaneMode(boolean enable) {
        mSystemLib.setAirplaneMode(enable);
    }

    public void setDataConnectionDisabled() {
        mSystemLib.setDataConnectionDisabled();
    }

    public void setDataConnectionEnabled() {
        mSystemLib.setDataConnectionEnabled();
    }

    public String formatSize(long size) {
        return mSystemLib.formatSize(size);
    }

    public String getWlanMacAddress() {
        return mSystemLib.getWlanMacAddress();
    }

    public int getWifiState() {
        return mSystemLib.getWifiState();
    }

    public boolean isWifiEnabled() {
        return mSystemLib.isWifiEnabled();
    }

    public boolean setWifiDisabled() {
        return mSystemLib.setWifiDisabled();
    }

    public boolean setWifiEnabled() {
        return mSystemLib.setWifiEnabled();
    }

    public boolean setWifiDisconnect() {
        return mSystemLib.setWifiDisconnect();
    }

    public boolean setWifiReconnect() {
        return mSystemLib.setWifiReconnect();
    }

    public boolean setWifiStartScan() {
        return mSystemLib.setWifiStartScan();
    }

    @Deprecated
    public String getServerIP() {
        return mSystemLib.getServerIP();
    }

    /**
     * run command by service side on PC
     * 
     * server is at phone and python client is at pc
     */
    @Deprecated
    public String runCmdOnServer(String command) {
        // TODO in future
        return "";
    }

    public boolean checkView(String searchKey, String searchValue, int searchMode, int targetNumber) {
        return mUILib.checkView(searchKey, searchValue, searchMode, targetNumber);
    }

    public void enterText(String text) {
        mUILib.enterText(text);
    }

    public void pressKey(int keyCode) {
        mUILib.pressKey(keyCode);
    }

    public void longPressKey(int keyCode) {
        mUILib.longPressKey(keyCode);
    }

    public void clickScreen(int x, int y) {
        mUILib.clickScreen(x, y);
    }

    public void clickLongScreen(int x, int y, int time) {
        mUILib.clickLongScreen(x, y, time);
    }

    public boolean clickView(String searchKey, String searchValue, int searchMode, int index,
            int timeout, int xOffset, int yOffset, int longClickTime, String scrollViewId,
            int scrollViewIndex) {
        return mUILib.clickView(searchKey, searchValue, searchMode, index, timeout, xOffset,
                yOffset, longClickTime, scrollViewId, scrollViewIndex);
    }

    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        mUILib.drag(fromX, toX, fromY, toY, stepCount);
    }

    public void waitForAllDumpCompleted() {
        mViewPropertyProvider.waitForAllDumpCompleted();
    }

    public String getTopActivity() {
        return mSystemLib.getTopActivity();
    }

    public void setScreenOn() {
        mSystemLib.setScreenOn();
    }

    public void sendKeyEvent(int keyCode) {
        mUILib.pressKey(keyCode);
    }

    public boolean waitforTopActivity(String className, long timeout) {
        return mSystemLib.waitforTopActivity(className, timeout);
    }

    public void factoryResetWithEraseSD() {
        mSystemLib.factoryResetWithEraseSD();
    }

    public void formatSD() {
        mSystemLib.formatSD();
    }

    public void setScreenStayAwake(boolean isAwake) {
        mSystemLib.setScreenStayAwake(isAwake);
    }

    public void setScreenTimeOut(int milisecond) {
        mSystemLib.setScreenTimeOut(milisecond);
    }

    public void setScreenUnlockSecurityNone() {
        mSystemLib.setScreenUnlockSecurityNone();
    }

    public void changeLanguage(String language) {
        mSystemLib.changeLanguage(language);
    }

    public void installApk(String filename) {
        mSystemLib.installApk(filename);
    }

    public void uninstallApk(String packageName) {
        mSystemLib.uninstallApk(packageName);
    }

    public boolean isPackageInstalled(String packageName) {
        return mSystemLib.isPackageInstalled(packageName);
    }

    public boolean installApkSync(String filename, long timeout) {
        return mSystemLib.installApkSync(filename, timeout);
    }

    public void setSystemProperties(String key, String val) {
        mSystemLib.setSystemProperties(key, val);
    }

    public String getSystemProperties(String key) {
        return mSystemLib.getSystemProperties(key);
    }

    public boolean getInputMethodStatus() {
        return mViewPropertyProvider.getInputMethodStatus();
    }

    public String[] getViewProperties(String searchKey, String searchValue, int searchMode,
            int targetNumber, String[] getKeys, boolean getNew) {
        return mViewPropertyProvider.getViewProperties(searchKey, searchValue, searchMode,
                targetNumber, getKeys, getNew);
    }

    public String getFocusedWindow() {
        return mViewPropertyProvider.getFocusedWindow();
    }

    public boolean checkProcessAlive(String processName) {
        return mViewPropertyProvider.checkProcessAlive(processName);
    }

    public int getNonMarketAppsAllowed() {
        return mSystemLib.getNonMarketAppsAllowed();
    }

    public void setNonMarketAppsAllowed(boolean enabled) {
        mSystemLib.setNonMarketAppsAllowed(enabled);
    }

    public boolean isBluetoothEnabled() {
        return mSystemLib.isBluetoothEnabled();
    }

    public void setBluetoothState(boolean enabled) {
        mSystemLib.setBluetoothState(enabled);
    }

    public void killBackgroundProcesses(String packageName) {
        mSystemLib.killBackgroundProcesses(packageName);
    }

    public String[] getWindowList() {
        return mViewPropertyProvider.getWindowList();
    }

    public boolean clearApplicationUserData(String packageName) {
        return mSystemLib.clearApplicationUserData(packageName);
    }

    /**
     * NOT READY YET, COMING SOON...
     */
    public void updatePackagePermission(String packageName, String permissionName, int state) {
        //        mSystemLib.updatePackagePermission(packageName, permissionName, state);
    }

    public String[] getPermissionsForPackage(String packageName) {
        return mSystemLib.getPermissionsForPackage(packageName);
    }

    public int getAutoTimeState() {
        return android.provider.Settings.System.getInt(mContext.getContentResolver(),
                android.provider.Settings.System.AUTO_TIME, 0);
    }

    public void setAutoTimeEnabled() {
        android.provider.Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.System.AUTO_TIME, 1);
    }

    public void setAutoTimeDisabled() {
        android.provider.Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.System.AUTO_TIME, 0);
    }

    public boolean getStatusBarIconState(String slotName) {
        return mViewPropertyProvider.getStatusBarIconState(slotName);
    }

    public String[] getLog(String[] command) {
        return mSystemLib.getLog(command);
    }

    public void clearLog() {
        mSystemLib.clearLog();
    }

    public int getScreenBrightnessMode() {
        return mSystemLib.getScreenBrightnessMode();
    }

    public void setScreenBrightnessMode(int mode) {
        mSystemLib.setScreenBrightnessMode(mode);
    }

    public void setLocationProviderEnabled(String provider, boolean enabled) {
        mSystemLib.setLocationProviderEnabled(provider, enabled);
    }

    public boolean isLocationProviderEnabled(String provider) {
        return mSystemLib.isLocationProviderEnabled(provider);
    }

    public boolean isAccelerometerRotationEnabled() {
        return mSystemLib.isAccelerometerRotationEnabled();
    }

    public void setAccelerometerRotationEnabled(boolean enabled) {
        mSystemLib.setAccelerometerRotationEnabled(enabled);
    }

    public boolean getBackgroundDataState() {
        return mSystemLib.getBackgroundDataState();
    }

    public void setBackgroundDataSetting(boolean enabled) {
        mSystemLib.setBackgroundDataSetting(enabled);
    }

    public boolean getMasterSyncAutomatically() {
        return mSystemLib.getMasterSyncAutomatically();
    }

    public void setMasterSyncAutomatically(boolean sync) {
        mSystemLib.setMasterSyncAutomatically(sync);
    }

    public void screenCap(String prefix) {
        mSystemLib.screenCap(prefix);
    }

    public void deleteAccount(String name, String type) {
        mSystemLib.deleteAccount(name, type);
    }

    public void recovery() {
        mSystemLib.recovery();
    }

    public int getCurrentTaskActivitiesNumber() {
        return mSystemLib.getCurrentTaskActivitiesNumber();
    }

    public void setStatusBarHeight(int height) {
        mViewPropertyProvider.setStatusBarHeight(height);
    }

    public boolean isViewServerOpen() {
        return ViewPropertyProvider.isViewServerOpen();
    }

    public int insertAPN(String name, String apn_addr, String proxy, String port) {
        return mSystemLib.insertAPN(name, apn_addr, proxy, port);
    }

    public boolean setDefaultAPN(int id) {
        return mSystemLib.setDefaultAPN(id);
    }

    public boolean isAdbEnabled() {
        return mSystemLib.isAdbEnabled();
    }

    public void setAdbEnabled(boolean enabled) {
        mSystemLib.setAdbEnabled(enabled);
    }

    public void keepState() {
        mSystemLib.keepState();
    }

    public boolean isHome() {
        return mSystemLib.isHome();
    }

    public void processAppBatteryUsage() {
        mSystemLib.processAppBatteryUsage();
    }

    public void printPackagePowerUsage() {
        mSystemLib.printPackagePowerUsage();
    }

    public void lockDangerousActivity(String unlockPassword) {
        mSystemLib.lockDangerousActivity(unlockPassword);
    }

    public boolean isAirplaneModeOn() {
        return mSystemLib.isAirplaneModeOn();
    }

    public boolean isNetworkEnable() {
        return mSystemLib.isNetworkEnable();
    }

    public String getTopPackage() {
        return mSystemLib.getTopPackage();
    }

    public void copyAssets(String dist) {
        mSystemLib.copyAssets(dist);
    }

    boolean isDumpAllLinesCompleted = false;

    public void dumpAllLines() {
        isDumpAllLinesCompleted = false;
        List<String> lines = mViewPropertyProvider.dumpAllLines();
        for (String line : lines) {
            Log.print(line);
        }
    }

    private final static int SEARCHMODE_COMPLETE_MATCHING = 1;
    private final static int TIMEOUT_DEFAULT_VALUE        = 10000;

    public boolean clickViewByText(String text) {
        return clickView("mText", text, SEARCHMODE_COMPLETE_MATCHING, 0,
                TIMEOUT_DEFAULT_VALUE/*10000*/, 0, 0, 0, null, 0);
    }

    public void expandStatusBar() {
        mSystemLib.expandStatusBar();
    }

    public String getStringByName(String name) {
        return mContext.getResources().getString(
                Strings.getRStringId(mContext.getPackageName(), name));
    }
}
