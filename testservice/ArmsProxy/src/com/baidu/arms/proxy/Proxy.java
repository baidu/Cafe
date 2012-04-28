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

package com.baidu.arms.proxy;

import java.io.File;
import java.util.ArrayList;

import android.app.Service;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.Toast;

/**
 * @author gaolichuang02@baidu.com
 * @date 2011-07-26
 */

/**
 * This class does not have android:sharedUserId="android.uid.system" deal with
 * intent! through broadcast following features: 1. sd card operation
 */
public class Proxy extends Service {

    String                        ACTION_NAME    = "baidu.cafe.arms";
    private static final String   LOG_TAG        = "Proxy";
    private volatile ProxyHandler mHandler;
    private volatile Looper       mServiceLooper;
    private static final int      MSG_INSTALLAPK = 101;
    private static final int      MSG_SCREENCAP  = 102;

    private Context               mcontext;

    public Proxy() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mcontext = this;

        HandlerThread thread = new HandlerThread(LOG_TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mHandler = new ProxyHandler(mServiceLooper);

    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d(LOG_TAG, "Received start id " + startId + ": " + intent);
        Log.d(LOG_TAG, "proxy receive the request!");
        handleIntent(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Received start id " + startId + ": " + intent);
        Log.d(LOG_TAG, "proxy receive the request!");
        handleIntent(intent);
        return 1;
    }

    void handleIntent(Intent intent) {
        if (null == intent) {
            Log.d(LOG_TAG, "intent is null");
            return;
        }

        String action = intent.getAction();
        if (null == action) {
            Log.d(LOG_TAG, "no action in intent");
            return;
        }
        String oper = intent.getStringExtra(MyIntent.EXTRA_OPERATION);
        if (MyIntent.EXTRA_INSTALL.equals(oper)) {
            mHandler.removeMessages(MSG_INSTALLAPK);
            Message msg = mHandler.obtainMessage();
            msg.obj = intent.getStringExtra(MyIntent.EXTRA_ARG1);
            msg.what = MSG_INSTALLAPK;
            mHandler.sendMessage(msg);
            Log.d(LOG_TAG, "send the message that msg.obj is " + msg.obj);
        }
        ////////
        if (MyIntent.EXTRA_SCREENCAP.equals(oper)) {
            mHandler.removeMessages(MSG_SCREENCAP);
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_SCREENCAP;
            msg.obj = intent.getStringExtra(MyIntent.EXTRA_ARG1);
            mHandler.sendMessage(msg);
            Log.d(LOG_TAG, "send the message that screen cap");
        }
        ////////
    }

    private final class ProxyHandler extends Handler {
        public ProxyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG, "handleMessage: " + msg.what + " thread " + Thread.currentThread().getId());
            switch (msg.what) {
            case MSG_INSTALLAPK:
                //handleNetworkReady();
                Intent begin = new Intent(MyIntent.ACTION_INSTALL_BEGIN);
                sendBroadcast(begin);
                Installapk((String) msg.obj);
                Intent installfinish = new Intent(MyIntent.ACTION_INSTALL_END);
                sendBroadcast(installfinish);
                Toast.makeText(Proxy.this, "service can update ui", Toast.LENGTH_LONG).show();
                break;
            ////////////
            case MSG_SCREENCAP:
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    Log.e("ArmProxy", "this no valid sd card");
                    return;
                }
                Intent screencapbegin = new Intent(MyIntent.ACTION_SCREENCAP_BEGIN);
                sendBroadcast(screencapbegin);
                ArrayList<String> org = FileUtil.listDir("/mnt/sdcard/DCIM/ScreenShot", "png");
                screencast();
                ArrayList<String> now = FileUtil.listDir("/mnt/sdcard/DCIM/ScreenShot", "png");
                ArrayList<String> ret = FileUtil.getDiff(now, org);
                if (null == ret) {
                    Log.d(LOG_TAG, "null == ret at MSG_SCREENCAP");
                    return;
                }
                if (ret.size() == 1) {
                    String prefix = (String) msg.obj;
                    FileUtil.rename(ret.get(0), prefix);
                }
                Intent screencapfinish = new Intent(MyIntent.ACTION_SCREENCAP_END);
                sendBroadcast(screencapfinish);
                Log.d(LOG_TAG, "finish screencast");
                break;
            ////////////
            }
            super.handleMessage(msg);
        }
    }

    //////////////////
    String screencast() {
        String result = null;
        String[] args = { "/system/bin/screencap" };
        result = ShellExecute.execute(args, "system/bin/");
        return result;
    }

    //////////////////
    void Installapk(String filename) {
        Log.d(LOG_TAG, "Install apk: file name is " + filename);
        int installFlags = 0;
        //    	installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
        PackageManager pm = getPackageManager();
        //  PackageInstallObserver observer = new PackageInstallObserver();
        Uri uri = Uri.fromFile(new File(filename));
        PackageParser.Package mPkgInfo = getPackageInfo(uri);
        pm.installPackage(uri, null, 0, mPkgInfo.packageName);
    }

    public static PackageParser.Package getPackageInfo(Uri packageURI) {
        final String archiveFilePath = packageURI.getPath();
        PackageParser packageParser = new PackageParser(archiveFilePath);
        File sourceFile = new File(archiveFilePath);
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        PackageParser.Package pkg = packageParser.parsePackage(sourceFile, archiveFilePath, metrics, 0);
        // Nuke the parser reference.
        //parseError = packageParser.getParseError();
        packageParser = null;
        return pkg;
    }
}
