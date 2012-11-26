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

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;

import com.baidu.cafe.remote.Armser;
import com.baidu.cafe.CafeExceptionHandler.ExceptionCallBack;
import com.baidu.cafe.local.Log;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.ShellExecute;
import com.baidu.cafe.local.ShellExecute.CommandResult;

import android.app.Activity;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.Window;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-11
 * @version
 * @todo
 */
public class CafeTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> implements ExceptionCallBack {

    protected static Armser                 remote                       = null;
    protected static LocalLib               local                        = null;

    public final static int                 SCREEN_ORIENTATION_PORTRAIT  = 0;
    public final static int                 SCREEN_ORIENTATION_LANDSCAPE = 1;

    private final static String             TAG                          = "CafeTestCase";
    private static String                   mPackageName                 = null;
    private Thread.UncaughtExceptionHandler orignal                      = null;
    private TearDownHelper                  mTearDownHelper              = null;
    private boolean                         mIsViewServerOpen            = false;

    /**
     * For Android version number > 2.1
     * 
     * @param activityClass
     */
    public CafeTestCase(Class<T> activityClass) {
        super(activityClass);
    }

    /**
     * For Android version number <= 2.1
     * 
     * @param packageName
     * @param activityClass
     */
    public CafeTestCase(String packageName, Class<T> activityClass) {
        super(packageName, activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.init(this, Log.DEFAULT);
        remote = new Armser(getInstrumentation().getContext());
        remote.bind(getInstrumentation().getContext());
        remote.setStatusBarHeight(getStatusBarHeight());
        local = new LocalLib(getInstrumentation(), getActivity());
        mPackageName = local.getCurrentActivity().getPackageName();
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
        initForJUnitReportTestRunner();
    }

    // chmod for com.zutubi.android.junitreport.JUnitReportTestRunner
    private void initForJUnitReportTestRunner() {
        CommandResult cr = new ShellExecute().execute("chmod 777 -R /data/data/" + mPackageName, "/");
        if (cr.ret != 0) {
            Log.i("initForJUnitReportTestRunner failed");
            Log.i(cr.console.toString());
        }
    }

    private int getStatusBarHeight() {
        Rect rect = new Rect();
        getActivity().getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWindowVisibleDisplayFrame(rect);
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

    /**
     * rewrite junit.framework.assert copy from
     * external/junit/src/junit/framework/Assert.java
     */

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * AssertionFailedError with the given message.
     */
    static public void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * AssertionFailedError.
     */
    static public void assertTrue(boolean condition) {
        assertTrue(null, condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * AssertionFailedError with the given message.
     */
    static public void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * AssertionFailedError.
     */
    static public void assertFalse(boolean condition) {
        assertFalse(null, condition);
    }

    private static String getAddress() {
        String methodName = null;
        int distance = 4;

        while (true) {
            methodName = Thread.currentThread().getStackTrace()[distance].getMethodName();
            if (!methodName.startsWith("assert") && !methodName.startsWith("fail")) {
                break;
            }
            distance++;
            if (distance > 10) {
                Log.d("distance > 10");
                break;
            }
        }

        return methodName;
    }

    /**
     * Fails a test with the given message.
     */
    static public void fail(String message) {
        local.screenShotNamedPrefix(getAddress());
        throw new AssertionFailedError(message);
    }

    /**
     * Fails a test with no message.
     */
    static public void fail() {
        fail(null);
    }

    /**
     * Asserts that two objects are equal. If they are not an
     * AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null)
            return;
        if (expected != null && expected.equals(actual))
            return;
        failNotEquals(message, expected, actual);
    }

    /**
     * Asserts that two objects are equal. If they are not an
     * AssertionFailedError is thrown.
     */
    static public void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two Strings are equal.
     */
    static public void assertEquals(String message, String expected, String actual) {
        if (expected == null && actual == null)
            return;
        if (expected != null && expected.equals(actual))
            return;
        throw new ComparisonFailure(message, expected, actual);
    }

    /**
     * Asserts that two Strings are equal.
     */
    static public void assertEquals(String expected, String actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two doubles are equal concerning a delta. If they are not an
     * AssertionFailedError is thrown with the given message. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(String message, double expected, double actual, double delta) {
        if (Double.compare(expected, actual) == 0)
            return;
        if (!(Math.abs(expected - actual) <= delta))
            failNotEquals(message, new Double(expected), new Double(actual));
    }

    /**
     * Asserts that two doubles are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(double expected, double actual, double delta) {
        assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that two floats are equal concerning a delta. If they are not an
     * AssertionFailedError is thrown with the given message. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(String message, float expected, float actual, float delta) {
        // handle infinity specially since subtracting to infinite values gives NaN and the
        // the following test fails
        if (Float.isInfinite(expected)) {
            if (!(expected == actual))
                failNotEquals(message, new Float(expected), new Float(actual));
        } else if (!(Math.abs(expected - actual) <= delta))
            failNotEquals(message, new Float(expected), new Float(actual));
    }

    /**
     * Asserts that two floats are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    static public void assertEquals(float expected, float actual, float delta) {
        assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that two longs are equal. If they are not an AssertionFailedError
     * is thrown with the given message.
     */
    static public void assertEquals(String message, long expected, long actual) {
        assertEquals(message, new Long(expected), new Long(actual));
    }

    /**
     * Asserts that two longs are equal.
     */
    static public void assertEquals(long expected, long actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two booleans are equal. If they are not an
     * AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, boolean expected, boolean actual) {
        assertEquals(message, Boolean.valueOf(expected), Boolean.valueOf(actual));
    }

    /**
     * Asserts that two booleans are equal.
     */
    static public void assertEquals(boolean expected, boolean actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two bytes are equal. If they are not an AssertionFailedError
     * is thrown with the given message.
     */
    static public void assertEquals(String message, byte expected, byte actual) {
        assertEquals(message, new Byte(expected), new Byte(actual));
    }

    /**
     * Asserts that two bytes are equal.
     */
    static public void assertEquals(byte expected, byte actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two chars are equal. If they are not an AssertionFailedError
     * is thrown with the given message.
     */
    static public void assertEquals(String message, char expected, char actual) {
        assertEquals(message, new Character(expected), new Character(actual));
    }

    /**
     * Asserts that two chars are equal.
     */
    static public void assertEquals(char expected, char actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two shorts are equal. If they are not an
     * AssertionFailedError is thrown with the given message.
     */
    static public void assertEquals(String message, short expected, short actual) {
        assertEquals(message, new Short(expected), new Short(actual));
    }

    /**
     * Asserts that two shorts are equal.
     */
    static public void assertEquals(short expected, short actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two ints are equal. If they are not an AssertionFailedError
     * is thrown with the given message.
     */
    static public void assertEquals(String message, int expected, int actual) {
        assertEquals(message, new Integer(expected), new Integer(actual));
    }

    /**
     * Asserts that two ints are equal.
     */
    static public void assertEquals(int expected, int actual) {
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that an object isn't null.
     */
    static public void assertNotNull(Object object) {
        assertNotNull(null, object);
    }

    /**
     * Asserts that an object isn't null. If it is an AssertionFailedError is
     * thrown with the given message.
     */
    static public void assertNotNull(String message, Object object) {
        assertTrue(message, object != null);
    }

    /**
     * Asserts that an object is null.
     */
    static public void assertNull(Object object) {
        assertNull(null, object);
    }

    /**
     * Asserts that an object is null. If it is not an AssertionFailedError is
     * thrown with the given message.
     */
    static public void assertNull(String message, Object object) {
        assertTrue(message, object == null);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not an
     * AssertionFailedError is thrown with the given message.
     */
    static public void assertSame(String message, Object expected, Object actual) {
        if (expected == actual)
            return;
        failNotSame(message, expected, actual);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not the
     * same an AssertionFailedError is thrown.
     */
    static public void assertSame(Object expected, Object actual) {
        assertSame(null, expected, actual);
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object an AssertionFailedError is thrown with the given
     * message.
     */
    static public void assertNotSame(String message, Object expected, Object actual) {
        if (expected == actual)
            failSame(message);
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object an AssertionFailedError is thrown.
     */
    static public void assertNotSame(Object expected, Object actual) {
        assertNotSame(null, expected, actual);
    }

    static public void failSame(String message) {
        String formatted = "";
        if (message != null)
            formatted = message + " ";
        fail(formatted + "expected not same");
    }

    static public void failNotSame(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null)
            formatted = message + " ";
        fail(formatted + "expected same:<" + expected + "> was not:<" + actual + ">");
    }

    static public void failNotEquals(String message, Object expected, Object actual) {
        fail(format(message, expected, actual));
    }

    static String format(String message, Object expected, Object actual) {
        String formatted = "";
        if (message != null)
            formatted = message + " ";
        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

}
