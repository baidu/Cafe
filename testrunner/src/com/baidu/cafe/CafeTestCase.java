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

import junit.framework.Assert;
import android.app.Activity;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.Window;

import com.baidu.cafe.CafeExceptionHandler.ExceptionCallBack;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.Log;
import com.baidu.cafe.remote.Armser;
import com.baidu.cafe.utils.ShellExecute;
import com.baidu.cafe.utils.ShellExecute.CallBack;
import com.baidu.cafe.utils.ShellExecute.CommandResult;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-11
 * @version
 * @todo
 */
public class CafeTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> implements
        ExceptionCallBack {

    public static Armser                    remote                       = null;
    protected static LocalLib               local                        = null;

    public final static int                 SCREEN_ORIENTATION_PORTRAIT  = 0;
    public final static int                 SCREEN_ORIENTATION_LANDSCAPE = 1;
    public final static int                 TIMEOUT_GET_ACTIVITY         = 1000 * 10;

    public static Class<?>                  mActivityClass               = null;
    public static String                    mTargetFilesDir              = "";

    private final static String             TAG                          = "CafeTestCase";
    private static String                   mPackageName                 = null;
    private Thread.UncaughtExceptionHandler orignal                      = null;
    private TearDownHelper                  mTearDownHelper              = null;
    private boolean                         mIsViewServerOpen            = false;
    private long                            mBeginTime;
    private int                             mPackageRcv;
    private int                             mPackageSnd;
    private Activity                        mActivity                    = null;

    /**
     * only for Android version number > 2.1
     * 
     * @param activityClass
     */
    public CafeTestCase(Class<T> activityClass) {
        super(activityClass);
        mActivityClass = activityClass;
    }

    /**
     * For Android full version
     * 
     * @param packageName
     * @param activityClass
     */
    public CafeTestCase(String packageName, Class<T> activityClass) {
        super(packageName, activityClass);
        mActivityClass = activityClass;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.init(this, Log.DEFAULT);
        mTargetFilesDir = getInstrumentation().getTargetContext().getFilesDir().toString();
        remote = new Armser(getInstrumentation().getContext());
        remote.bind(getInstrumentation().getContext());
        //        launchActivityIfNotAvailable();
        remote.setStatusBarHeight(getStatusBarHeight());
        String command = "chmod 777 " + mTargetFilesDir;
        CommandResult cr = LocalLib.executeOnDevice(command, "/", 1000);
        if (null == cr) {
            Log.i(command + " failed!");
        } else {
            Log.i(command + " " + (cr.ret == 0 ? "success" : "failed!\n" + cr.console.toString()));
        }
        remote.copyAssets(mTargetFilesDir);
        local = new LocalLib(getInstrumentation(), getActivity());
        LocalLib.mPackageName = mPackageName = local.getCurrentActivity().getPackageName();
        orignal = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CafeExceptionHandler(orignal, this));
        if (remote.isViewServerOpen()) {
            mIsViewServerOpen = true;
            mTearDownHelper = new TearDownHelper(remote);
        } else {
            mIsViewServerOpen = false;
            Log.i("View server is not open !!!");
            Log.i("remote.clickXXX() can not work !!!");
        }
        mBeginTime = System.currentTimeMillis();
        mPackageRcv = LocalLib.getPackageRcv(mPackageName);
        mPackageSnd = LocalLib.getPackageSnd(mPackageName);
    }

    private void launchActivityIfNotAvailable() {
        if (null == remote) {
            Log.i("null == remote at launchActivityIfNotAvailable");
            return;
        }
        int count = 0;

        while (count < 6) {
            count++;
            Boolean ret = ShellExecute.doInTimeout(new CallBack<Boolean>() {

                @Override
                public Boolean runInTimeout() throws InterruptedException {
                    mActivity = getActivity();
                    return true;
                }
            }, TIMEOUT_GET_ACTIVITY);

            if (null == ret) {
                // press home key
                remote.pressKey(KeyEvent.KEYCODE_HOME);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // launch activity
                String activityName = mActivityClass.getName();
                String cmd = String.format("am start -a android.intent.action.MAIN -n %s/%s",
                        mActivityClass.getPackage().getName(), activityName);
                Log.i("execute cmd " + cmd);
                new ShellExecute().execute(cmd, "/", 3000);
            } else {
                Log.i("get activity success");
                return;
            }
        }

        Assert.assertTrue("get activity failed!!!", false);
    }

    private int getStatusBarHeight() {
        Rect rect = new Rect();
        getActivity().getWindow().findViewById(Window.ID_ANDROID_CONTENT)
                .getWindowVisibleDisplayFrame(rect);
        Log.i("" + rect.top);
        return rect.top;
    }

    @Override
    protected void tearDown() throws Exception {
        if (mIsViewServerOpen) {
            mTearDownHelper.backToHome();
            mTearDownHelper.killWindowsFromBirthToNow();
            mTearDownHelper = null;
            remote.waitForAllDumpCompleted();
        }

        try {
            local.finalize();
            local = null;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // It costs too much time.
        //remote.unbind(getInstrumentation().getContext());

        if (orignal != null) {
            Thread.setDefaultUncaughtExceptionHandler(orignal);
            orignal = null;
        }

        getActivity().finish();
        remote = null;
        super.tearDown();
    }

    /**
     * This function will be called when the program to be tested has crashed.
     * You can add you own operation when crash has happened. Subclasses that
     * override this method. You can call super.callWhenExceptionHappen(),it
     * will take a screencap when crash is happening.
     */
    @Override
    public void callWhenExceptionHappen() {
        Log.i(TAG, "XXXXXXXXXX--my exceptionhandler callback--XXXXXXXXXXXXXXXXXXXX");
    }

}
