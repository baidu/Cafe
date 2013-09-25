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

package com.baidu.cafe.local;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ScrollView;
import android.widget.TabWidget;
import android.widget.TextView;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.CafeTestRunner;
import com.baidu.cafe.local.record.ViewRecorder;
import com.baidu.cafe.local.traveler.APPTraveler;
import com.baidu.cafe.local.traveler.Logger;
import com.baidu.cafe.utils.ReflectHelper;
import com.baidu.cafe.utils.ShellExecute;
import com.baidu.cafe.utils.ShellExecute.CommandResult;
import com.baidu.cafe.utils.Strings;
import com.jayway.android.robotium.solo.Solo;
import com.jayway.android.robotium.solo.WebElement;

import dalvik.system.DexFile;

/**
 * It can help you as below.
 * 
 * 1.get or set a object's private property and invoke a object's private
 * function
 * 
 * 2.find view by text or resid
 * 
 * 3.get views which is generated dynamically
 * 
 * 4.record human operations and generate Cafe codes
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-5-17
 * @version
 * @todo
 */

public class LocalLib extends Solo {
    public final static int       SEARCHMODE_COMPLETE_MATCHING = 1;
    public final static int       SEARCHMODE_DEFAULT           = 1;
    public final static int       SEARCHMODE_INCLUDE_MATCHING  = 2;

    private final static int      WAIT_INTERVAL                = 1000;
    private final static int      MINISLEEP                    = 100;
    private final static int      SMALL_WAIT_TIMEOUT           = 10000;

    public static String          mTestCaseName                = null;
    public static String          mPackageName                 = null;
    public static int[]           mTheLastClick                = new int[2];
    public static Instrumentation mInstrumentation;

    public RecordReplay           recordReplay                 = null;

    private boolean               mHasBegin                    = false;
    private ArrayList<View>       mViews                       = null;
    private Activity              mActivity;
    private Context               mContext                     = null;

    public LocalLib(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);
        mInstrumentation = instrumentation;
        mActivity = activity;
        mContext = instrumentation.getContext();
        mTheLastClick[0] = -1;
        mTheLastClick[1] = -1;
        recordReplay = new RecordReplay();
    }

    private static void print(String message) {
        if (Log.IS_DEBUG) {
            Log.i("LocalLib", message);
        }
    }

    /**
     * invoke object's private method
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param methodName
     *            : name of the target method
     * @param parameterTypes
     *            : types of the target method's parameters
     * @param parameters
     *            : parameters of the target method
     * @return result of invoked method
     * 
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object invoke(Object owner, String targetClass, String methodName,
            Class<?>[] parameterTypes, Object[] parameters) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        return ReflectHelper.invoke(owner, targetClass, methodName, parameterTypes, parameters);
    }

    /**
     * Set object's field with custom value even it's private.
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param fieldName
     *            : name of the target field
     * @param value
     *            : new value of the target field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public void setField(Object owner, String targetClass, String fieldName, Object value)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        ReflectHelper.setField(owner, targetClass, fieldName, value);
    }

    /**
     * get object's private property
     * 
     * @param owner
     *            : target object
     * @param classLevel
     *            : 0 means itself, 1 means it's father, and so on...
     * @param fieldName
     *            : name of the target field
     * @return value of the target field
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Object getField(Object owner, String targetClass, String fieldName)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        return ReflectHelper.getField(owner, targetClass, fieldName);
    }

    /**
     * get object's private property by type
     * 
     * @param owner
     *            target object
     * @param classLevel
     *            0 means itself, 1 means it's father, and so on...
     * @param typeString
     *            e.g. java.lang.String
     * @return ArrayList<String> of property's name
     */
    public ArrayList<String> getFieldNameByType(Object owner, String targetClass, Class<?> type) {
        return ReflectHelper.getFieldNameByType(owner, targetClass, type);
    }

    /**
     * @param owner
     *            target object
     * @param classLevel
     *            0 means itself, 1 means it's father, and so on...
     * @param valueType
     *            e.g. String.class
     * @param value
     *            value of the target fields
     * @return ArrayList<String> of property's name
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public ArrayList<String> getFieldNameByValue(Object owner, String targetClass,
            Class<?> valueType, Object value) throws IllegalArgumentException,
            IllegalAccessException {
        return ReflectHelper.getFieldNameByValue(owner, targetClass, valueType, value);
    }

    /**
     * hook listeners on all views for generating Cafe code automatically
     */
    public void beginRecordCode() {
        new ViewRecorder(this).beginRecordCode();
    }

    /**
     * Get listener from view. e.g. (OnClickListener) getListener(view,
     * "mOnClickListener"); means get click listener. Listener is a private
     * property of a view, that's why this function is written.
     * 
     * @param view
     *            target view
     * @param targetClass
     *            the class which fieldName belong to
     * @param fieldName
     *            target listener. e.g. mOnClickListener, mOnLongClickListener,
     *            mOnTouchListener, mOnKeyListener
     * @return listener object; null means no listeners has been found
     */
    public Object getListener(View view, Class<?> targetClass, String fieldName) {
        int level = countLevelFromViewToFather(view, targetClass);
        if (-1 == level) {
            return null;
        }

        try {
            if (!(view instanceof AdapterView) && Build.VERSION.SDK_INT > 14) {// API Level 14: Android 4.0
                Object mListenerInfo = ReflectHelper.getField(view, targetClass.getName(),
                        "mListenerInfo");
                return null == mListenerInfo ? null : ReflectHelper.getField(mListenerInfo, null,
                        fieldName);
            } else {
                return ReflectHelper.getField(view, targetClass.getName(), fieldName);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // eat it
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method is used to replace listener.setOnListener().
     * listener.setOnListener() is probably overrided by application, so its
     * behavior can not be expected.
     * 
     * @param view
     * @param targetClass
     * @param fieldName
     * @param value
     */
    public void setListener(View view, Class<?> targetClass, String fieldName, Object value) {
        int level = countLevelFromViewToFather(view, targetClass);
        if (-1 == level) {
            return;
        }

        try {
            if (!(view instanceof AdapterView) && Build.VERSION.SDK_INT > 14) {// API
                // Level:
                // 14.
                // Android
                // 4.0
                Object mListenerInfo = ReflectHelper.getField(view, targetClass.getName(),
                        "mListenerInfo");
                if (null == mListenerInfo) {
                    return;
                }
                ReflectHelper.setField(mListenerInfo, null, fieldName, value);
            } else {
                ReflectHelper.setField(view, targetClass.getName(), fieldName, value);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // eat it
            // e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * find parent until parent is father or java.lang.Object(to the end)
     * 
     * @param view
     *            target view
     * @param father
     *            target father
     * @return positive means level from father; -1 means not found
     */
    public int countLevelFromViewToFather(View view, Class<?> father) {
        if (null == view) {
            return -1;
        }
        int level = 0;
        Class<?> originalClass = view.getClass();
        // find its parent
        while (true) {
            if (originalClass.equals(Object.class)) {
                return -1;
            } else if (originalClass.equals(father)) {
                return level;
            } else {
                level++;
                originalClass = originalClass.getSuperclass();
            }
        }
    }

    public String getViewText(View view) {
        try {
            Method method = view.getClass().getMethod("getText");
            return (String) (method.invoke(view));
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // eat it
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            // eat it
        }
        return "";
    }

    /**
     * find views via view's text, it only needs part of target view's text
     * 
     * @param text
     *            the text of the view
     * @return a ArrayList<View> contains views found
     */
    @Deprecated
    public ArrayList<View> findViewsByText(String text) {
        ArrayList<View> allViews = getViews();
        ArrayList<View> views = new ArrayList<View>();
        int viewNumber = allViews.size();

        for (int i = 0; i < viewNumber; i++) {
            View view = allViews.get(i);
            String t = getViewText(view);
            if (t.indexOf(text) != -1) {
                views.add(view);
            }
        }
        return views;
    }

    /**
     * call this function before new views appear
     */
    public void getNewViewsBegin() {
        mViews = getViews();
        mHasBegin = true;
    }

    /**
     * call this function after new views appear
     * 
     * @return A ArrayList<View> contains views which are new. Null means no new
     *         views
     */
    public ArrayList<View> getNewViewsEnd() {
        if (!mHasBegin) {
            return null;
        }

        ArrayList<View> views = getViews();
        ArrayList<View> diffViews = new ArrayList<View>();
        int sizeOfNewViews = views.size();
        int sizeOfOldViews = mViews.size();
        boolean duplicate;

        for (int i = 0; i < sizeOfNewViews; i++) {
            duplicate = false;
            for (int j = 0; j < sizeOfOldViews; j++) {
                if (views.get(i).equals(mViews.get(j))) {
                    duplicate = true;
                }
            }
            if (!duplicate) {
                diffViews.add(views.get(i));
            }
        }

        return diffViews;
    }

    public String getRIdNameByValue(String packageName, int value) {
        Class<?> idClass = Strings.getRClass(packageName, "id");
        if (null == idClass) {
            return "";
        }
        try {
            for (Field field : idClass.getDeclaredFields()) {
                Integer id = (Integer) field.get(idClass.newInstance());
                if (id == value) {
                    return field.getName();
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get R.string.yourTargetString from test package
     * 
     * @param stringName
     *            name of your target string
     * @return string value
     */
    public String getTestRString(String stringName) {
        return mContext.getResources().getString(
                Strings.getRStringId(mContext.getPackageName(), stringName));
    }

    /**
     * get R.string.yourTargetString from tested package
     * 
     * @param stringName
     *            name of your target string
     * @return string value
     */
    @Deprecated
    public String getTestedRString(String stringName) {
        return getString(Strings.getRStringId(mActivity.getPackageName(), stringName));
    }

    /**
     * you can use this function when getActivtiy is hanging. When you want to
     * reinit solo you should recall public void init(Activity macy)
     * 
     * @param activityName
     *            example: the activity "TestAcy" you wanted, the param is
     *            "TestAcy.class.getName()"
     * @return activity
     */
    public Activity getActivityAsync(String activityName) {
        return mInstrumentation.waitForMonitor(mInstrumentation.addMonitor(activityName, null,
                false));
    }

    /**
     * run shell command with tested app's permission
     * 
     * @param command
     *            e.g. new String[]{"ls", "-l"}
     * @param directory
     *            e.g. "/sdcard"
     * @return the result string of the command
     */
    public static CommandResult executeOnDevice(String command, String directory) {
        return new ShellExecute().execute(command, directory);
    }

    /**
     * run shell command with tested app's permission
     * 
     * @param command
     *            e.g. new String[]{"ls", "-l"}
     * @param directory
     *            e.g. "/sdcard"
     * @param timeout
     *            Millis. e.g. 5000 means 5s
     * 
     * @return the result string of the command
     */
    public static CommandResult executeOnDevice(String command, String directory, long timeout) {
        return new ShellExecute().execute(command, directory, timeout);
    }

    /**
     * Waits for a view to vanish
     * 
     * @param resId
     *            the id you see in hierarchy . for example in Launcher
     *            "id/workspace" timeout is default SMALL_WAIT_TIMEOUT scroll is
     *            default true only visible is default true
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId) {
        return waitForViewVanishById(resId, 0, SMALL_WAIT_TIMEOUT, true);
    }

    /**
     * Waits for a view to vanish
     * 
     * @param resId
     *            the id you see in hierarchy . for example in Launcher
     *            "id/workspace" timeout is default SMALL_WAIT_TIMEOUT scroll is
     *            default true only visible is default true
     * @param index
     *            specify resId with a given index.
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId, int index) {
        return waitForViewVanishById(resId, index, SMALL_WAIT_TIMEOUT, true);
    }

    /**
     * Waits for a view to vanish
     * 
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param index
     *            specify resId with a given index.
     * @param timeout
     *            the delay milliseconds scroll is default true only visible is
     *            default true
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId, int index, long timeout) {
        return waitForViewVanishById(resId, index, timeout, true);
    }

    /**
     * Waits for a view vanished
     * 
     * @param resName
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay milliseconds
     * @param scroll
     *            true you want to scroll
     * @param onlyvisible
     *            true we only deal with the view visible
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resName, int index, long timeout, boolean scroll) {
        Long end = System.currentTimeMillis() + timeout;
        while (true) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            if (!waitforViewByResName(resName, index, WAIT_INTERVAL, scroll)) {
                return true;
            }
        }
    }

    /**
     * Waits for a text to vanish.
     * 
     * @param text
     *            the text to wait for
     * @return {@code true} if text is shown and {@code false} if it is not
     *         shown before the timeout
     * 
     */
    public boolean waitForTextVanish(String text) {
        return waitForTextVanish(text, 0, 8000, false);
    }

    /**
     * Waits for a text to vanish.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown.
     *            {@code 0} means any number of matches
     * @return {@code true} if text is shown and {@code false} if it is not
     *         shown before the timeout
     * 
     */
    public boolean waitForTextVanish(String text, int minimumNumberOfMatches) {
        return waitForTextVanish(text, minimumNumberOfMatches, 8000, false);
    }

    /**
     * Waits for a text to vanish.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown.
     *            {@code 0} means any number of matches
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @return {@code true} if text is shown and {@code false} if it is not
     *         shown before the timeout
     * 
     */
    public boolean waitForTextVanish(String text, int minimumNumberOfMatches, long timeout) {
        return waitForTextVanish(text, minimumNumberOfMatches, timeout, false);
    }

    /**
     * Waits for a text to vanish.
     * 
     * @param text
     *            the text to wait for
     * @param minimumNumberOfMatches
     *            the minimum number of matches that are expected to be shown.
     *            {@code 0} means any number of matches
     * @param timeout
     *            the amount of time in milliseconds to wait
     * @param scroll
     *            {@code true} if scrolling should be performed
     * @return {@code true} if text is shown and {@code false} if it is not
     *         shown before the timeout
     * 
     */
    public boolean waitForTextVanish(String text, int minimumNumberOfMatches, long timeout,
            boolean scroll) {
        Long end = System.currentTimeMillis() + timeout;
        while (true) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            if (!waitForText(text, minimumNumberOfMatches, WAIT_INTERVAL, scroll)) {
                return true;
            }
        }
    }

    /**
     * Waits for value from WaitCallBack.getActualVaule() equaling to expect
     * value until time is out.
     * 
     * @param expect
     * @param callBack
     * @return true: WaitCallBack.getActualVaule() equals to expectation; false:
     *         WaitCallBack.getActualVaule() differs from expectation
     */
    public boolean waitEqual(String expect, WaitCallBack callBack) {
        return waitEqual(expect, callBack, 10000);
    }

    public interface WaitCallBack {
        String getActualValue();
    }

    /**
     * Waits for value from WaitCallBack.getActualVaule() equaling to expect
     * value until time is out.
     * 
     * @param expect
     * @param callBack
     * @param timeout
     * @return true: WaitCallBack.getActualVaule() equals to expectation; false:
     *         WaitCallBack.getActualVaule() differs from expectation
     */
    public boolean waitEqual(String expect, WaitCallBack callBack, long timeout) {
        Long end = System.currentTimeMillis() + timeout;

        while (true) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            if (expect.equals(callBack.getActualValue())) {
                return true;
            }
        }
    }

    /**
     * zoom screen
     * 
     * @param start
     *            the start position e.g. new int[]{0,0,1,2}; means two pointers
     *            start at {0,0} and {1,2}
     * @param end
     *            the end position e.g. new int[]{100,110,200,220}; means two
     *            pointers end at {100,110} and {200,220}
     */
    public void zoom(int[] start, int[] end) {
        sendMultiTouchMotionEvent(2, start, end, 10, 0, 0, 0);
    }

    /**
     * send a Multi-Touch Motion Event
     * 
     * @param pointerNumber
     *            the number of pointer
     * @param start
     *            the start position e.g. new int[]{0,0,1,2}; means two pointers
     *            start at {0,0} and {1,2}
     * @param end
     *            the end position e.g. new int[]{100,110,200,220}; means two
     *            pointers end at {100,110} and {200,220}
     * @param step
     *            the move step
     * @param downDelay
     *            the delay after down event was sent
     * @param moveDelay
     *            the delay after each move event was sent
     * @param upDelay
     *            the delay before sending up event
     */
    @SuppressLint("Recycle")
    @SuppressWarnings("deprecation")
    public void sendMultiTouchMotionEvent(int pointerNumber, int[] start, int[] end, int step,
            int downDelay, int moveDelay, int upDelay) {

        double[] delta = new double[pointerNumber * 2];
        int[] pointerIds = new int[pointerNumber];
        PointerCoords[] pointerPositions = new PointerCoords[pointerNumber];

        int temp = 0;
        for (int i = 0; i < pointerNumber; i++) {
            pointerPositions[i] = new PointerCoords();
            pointerPositions[i].pressure = 1.0f;

            temp = i * 2;
            delta[temp] = (end[temp] - start[temp]) / (double) step;
            pointerPositions[i].x = start[temp];

            temp++;
            delta[temp] = (end[temp] - start[temp]) / (double) step;
            pointerPositions[i].y = start[temp];

            pointerIds[i] = i;
        }

        long myTime = SystemClock.uptimeMillis();
        mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime,
                MotionEvent.ACTION_DOWN, pointerNumber, pointerIds, pointerPositions, 0, 0.1f,
                0.1f, 0, 0, 0, 0));
        this.sleep(downDelay);

        for (int i = 0; i < step; i++) {
            for (int j = 0; j < pointerNumber; j++) {
                temp = j * 2;
                pointerPositions[j].x = (float) (start[temp] + delta[temp] * (i + 1));

                temp++;
                pointerPositions[j].y = (float) (start[temp] + delta[temp] * (i + 1));
            }

            myTime = SystemClock.uptimeMillis();
            mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime,
                    MotionEvent.ACTION_MOVE, pointerNumber, pointerIds, pointerPositions, 0, 0.1f,
                    0.1f, 0, 0, 0, 0));

            this.sleep(moveDelay);
        }

        this.sleep(upDelay);
        myTime = SystemClock.uptimeMillis();
        mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime, MotionEvent.ACTION_UP,
                pointerNumber, pointerIds, pointerPositions, 0, 0.1f, 0.1f, 0, 0, 0, 0));
    }

    /**
     * set CheckedTextView checked or not
     * 
     * @param index
     * @param checked
     * @return if set ok return true
     */
    public boolean setCheckedTextView(int index, boolean checked) {
        ArrayList<CheckedTextView> checkedTextViews = getCurrentViews(CheckedTextView.class);
        if (index < checkedTextViews.size()) {
            final CheckedTextView checkedTextView = checkedTextViews.get(index);
            final boolean fChecked = checked;
            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    checkedTextView.setChecked(fChecked);
                }
            });
            return true;
        }
        return false;
    }

    /**
     * Returns an ArrayList with the Tab located in the current activity
     * 
     * @return ArrayList of the Tabs contained in the current activity
     */
    public ArrayList<TabWidget> getCurrentTabs() {
        ArrayList<TabWidget> tabList = new ArrayList<TabWidget>();
        ArrayList<View> viewList = getCurrentViews();
        for (View view : viewList) {
            if (view instanceof android.widget.TabWidget) {
                tabList.add((TabWidget) view);
            }
        }
        return tabList;
    }

    /**
     * This method returns a tab with a certain index.
     * 
     * @param index
     *            the index of the Tab
     * @return the tab with the specific index
     */
    public TabWidget getTab(int index) {
        ArrayList<TabWidget> tabList = getCurrentTabs();
        TabWidget tab = null;
        try {
            tab = tabList.get(index);
        } catch (Throwable e) {
        }
        return tab;
    }

    /**
     * Click on a tab with a certain item
     * 
     * @param index
     *            the index of the tab
     * @param item
     *            the item of the tab will be clicked
     */
    public void clickOnTab(int index, int item) {
        TabWidget tab = null;
        try {
            tab = getTab(index);
            Assert.assertTrue("Tab is null", tab != null);
            clickOnView(tab.getChildAt(item));
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue("Index is not valid", false);
        }
    }

    /**
     * click on screen, the point is on the right
     */
    public void clickOnScreenRight() {
        float x = getDisplayX();
        float y = getDisplayY();
        clickOnScreen(x / 4, y / 2);
    }

    /**
     * click on screen, the point is on the left
     */
    public void clickOnScreenLeft() {
        float x = getDisplayX();
        float y = getDisplayY();
        clickOnScreen(x - x / 4, y / 2);
    }

    /**
     * click on screen, the point is on the up
     */
    public void clickOnScreenUp() {
        float x = getDisplayX();
        float y = getDisplayY();
        clickOnScreen(x / 2, y / 4);
    }

    /**
     * click on screen, the point is on the down
     */
    public void clickOnScreenDown() {
        float x = getDisplayX();
        float y = getDisplayY();
        clickOnScreen(x / 2, y - y / 4);
    }

    /**
     * drag on screen to right
     */
    public void dragScreenToRight(int stepCount) {
        float x = getDisplayX();
        float y = getDisplayY();
        drag(x - x / 4, x / 4, y / 2, y / 2, stepCount);
    }

    /**
     * drag on screen to Left
     */
    public void dragScreenToLeft(int stepCount) {
        float x = getDisplayX();
        float y = getDisplayY();
        drag(x / 4, x - x / 4, y / 2, y / 2, stepCount);
    }

    /**
     * drag on screen to up
     */
    public void dragScreenToUp(int stepCount) {
        float x = getDisplayX();
        float y = getDisplayY();
        drag(x / 2, x / 2, y - y / 4, y / 4, stepCount);
    }

    /**
     * drag on screen to Down
     */
    public void dragScreenToDown(int stepCount) {
        float x = getDisplayX();
        float y = getDisplayY();
        drag(x / 2, x / 2, y / 4, y - y / 4, stepCount);
    }

    /**
     * wait for a specified view
     * 
     * @param resName
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace" timeout is default 3000 scroll is default true
     *            onlyVisible is default true
     * @return true we get it
     */
    public boolean waitforViewByResName(String resName) {
        return waitforViewByResName(resName, 0, SMALL_WAIT_TIMEOUT, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resName
     *            the name you see in hierarchy. for example in Launcher
     *            "id/workspace" timeout is default 3000 scroll is default true
     *            onlyVisible is default true
     * @return true we get it
     */
    public boolean waitforViewByResName(String resName, int index) {
        return waitforViewByResName(resName, index, SMALL_WAIT_TIMEOUT, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resName
     *            the name you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay millisecond scroll is default true onlyVisible is
     *            default true
     * @return true we get it
     */
    public boolean waitforViewByResName(String resName, int index, long timeout) {
        return waitforViewByResName(resName, index, timeout, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resName
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay millisecond
     * @param scroll
     *            true you want to scroll
     * @param onlyVisible
     *            true we only deal with the view visible
     * @return true we get it
     */
    public boolean waitforViewByResName(String resName, int index, long timeout, boolean scroll) {
        final long endTime = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < endTime) {
            View view = getViewByResName(resName, 0);

            if (view != null) {
                return true;
            }

            try {
                Object down = ReflectHelper.getField(scroller, null, "DOWN");
                // mScroller.scroll(mScroller.DOWN)
                if (scroll
                        && !(Boolean) ReflectHelper.invoke(scroller, null, "scroll",
                                new Class[] { int.class }, new Object[] { down })) {
                    continue;
                }
                ReflectHelper.invoke(sleeper, null, "sleep", new Class[] {}, new Object[] {});// mSleeper.sleep();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Click the first specified view by resource name.
     * 
     * @param resName
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @return true we got it
     */
    public void clickViewByResName(String resName) {
        clickViewByResName(resName, 0);
    }

    /**
     * Click a specified view by resource name.
     * 
     * @param resName
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param index
     *            Clicks on an resId with a given index.
     */
    public void clickViewByResName(String resName, int index) {
        final View view = getViewByResName(resName, index);
        Assert.assertTrue("null == view at" + Log.getThreadInfo(), null != view);
        clickOnView(view);
    }

    /**
     * Get the first view by resource name
     * 
     * @param resName
     *            resource name
     * @return null means not found
     */
    public View getViewByResName(String resName) {
        return getViewByResName(resName, 0);
    }

    /**
     * @param resId
     * @param index
     * @return null means not found
     */
    public View getViewByResName(String resId, int index) {
        ArrayList<View> views = getCurrentViews();
        int count = 0;
        for (View view : views) {
            if (getResName(view).equals(resId)) {
                count++;
            }
            if (count - 1 == index) {
                return view;
            }
        }
        return null;
    }

    /**
     * @param view
     * @return empty string means no id
     */
    private String getResName(View view) {
        int resid = view.getId();
        if (View.NO_ID == resid) {
            return "";
        }

        try {
            // view.getResources().getResourceName(resid); sometimes throws java.lang.NullPointerException
            String resIdString = getCurrentActivity().getResources().getResourceName(resid);
            return resIdString.split(":")[1].trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * This api is only for ViewRecorder not for user.
     * 
     * @param view
     * @return -1 means no res id or view is not found
     */
    public int getResIdIndex(View targetView) {
        int index = -1;
        String resId = getResName(targetView);
        if ("".equals(resId)) {
            return index;
        }
        for (View view : getCurrentViews()) {
            if (getResName(view).equals(resId)) {
                index++;
            }
            if (view.equals(targetView)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Search text from parent view
     * 
     * @param parent
     *            parent view
     * @param text
     *            text you want to search
     * @param searchMode
     *            include SEARCHMODE_COMPLETE_MATCHING, SEARCHMODE_DEFAULT and
     *            SEARCHMODE_INCLUDE_MATCHING
     * @return true means found otherwise false
     */
    @SuppressWarnings("unchecked")
    public boolean searchTextFromParent(View parent, String text, int searchMode) {
        try {
            ArrayList<TextView> textViews = (ArrayList<TextView>) ReflectHelper.invoke(viewFetcher,
                    null, "getCurrentViews", new Class[] { Class.class, View.class }, new Object[] {
                            TextView.class, parent });// mViewFetcher.getCurrentViews(TextView.class, parent);
            for (TextView textView : textViews) {
                switch (searchMode) {
                case SEARCHMODE_COMPLETE_MATCHING:
                    if (textView.getText().equals(text)) {
                        return true;
                    }
                    break;
                case SEARCHMODE_INCLUDE_MATCHING:
                    if (textView.getText().toString().contains(text)) {
                        return true;
                    }
                    break;
                default:
                    Assert.assertTrue("Unknown searchMode!" + Log.getThreadInfo(), false);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Take an activity snapshot named 'timestamp', and you can get it by adb
     * pull /data/data/<packagename>/xxxxx.jpg .
     */
    public void screenShotNamedTimeStamp() {
        screenShot(getTimeStamp());
    }

    public void screenShotNamedCaseName(String suffix) {
        screenShot(mTestCaseName + "_" + suffix);
    }

    public void screenShotNamedSuffix(String suffix) {
        screenShot(getTimeStamp() + "_" + suffix);
    }

    private static String getTimeStamp() {
        Time localTime = new Time("Asia/Hong_Kong");
        localTime.setToNow();
        return localTime.format("%Y-%m-%d_%H-%M-%S");
    }

    public void screenShot(final String fileName) {
        String packagePath = CafeTestCase.mTargetFilesDir;
        File targetFilesDir = new File(packagePath);
        if (!targetFilesDir.exists()) {
            targetFilesDir.mkdir();
        }

        // chmod for adb pull /data/data/<package_name>/files .
        // executeOnDevice("chmod 777 " + packagePath, "/", 200);
        runOnMainSync(new Runnable() {
            public void run() {
                takeActivitySnapshot(fileName + ".jpg");
            }
        });
    }

    public void takeWebViewSnapshot(final WebView webView, final String savePath) {
        // SnapshotHelper.takeWebViewSnapshot(webView, savePath);
        SnapshotHelper.dumpPic(webView, savePath);
    }

    /**
     * screencap can only be invoked from shell not app process
     */
    // public void screencap(String fileName) {
    // String path = String.format("screencap -p %s/%s.png",
    // mInstrumentation.getTargetContext()
    // .getFilesDir().toString(), fileName);
    // executeOnDevice(path, "/system/bin");
    // }

    /**
     * Take an activity snapshot.
     */
    public void takeActivitySnapshot(final String path) {
        View decorView = getRecentDecorView();
        /*
         * try { invokeObjectMethod(this, 2, "wrapAllGLViews", new Class[] {
         * View.class }, new Object[] { decorView });//
         * solo.wrapAllGLViews(decorView); } catch (SecurityException e) {
         * e.printStackTrace(); } catch (IllegalArgumentException e) {
         * e.printStackTrace(); } catch (NoSuchMethodException e) {
         * e.printStackTrace(); ReflectHelper.listObject(this, 2); } catch
         * (IllegalAccessException e) { e.printStackTrace(); } catch
         * (InvocationTargetException e) { e.printStackTrace(); }
         */
        SnapshotHelper.takeViewSnapshot(decorView, path);
    }

    public View getRecentDecorView() {
        View[] views = getWindowDecorViews();

        if (null == views || 0 == views.length) {
            print("0 == views.length at getRecentDecorView");
            return null;
        }

        View recentDecorview = getRecentDecorView(views);
        if (null == recentDecorview) {
            // print("null == rview; use views[0]: " + views[0]);
            recentDecorview = views[0];
        }
        return recentDecorview;
    }

    /**
     * get all class names from a package via its dex file
     * 
     * @param packageName
     *            e.g. "com.baidu.cafe"
     * @return names of classes
     */
    public ArrayList<String> getAllClassNamesFromPackage(String packageName) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
            String path = mContext.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA).sourceDir;
            DexFile dexfile = new DexFile(path);
            Enumeration<String> entries = dexfile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement();
                if (name.indexOf('$') == -1) {
                    classes.add(name);
                }
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    public void hideInputMethod() {
        for (EditText editText : getCurrentViews(EditText.class)) {
            hideInputMethod(editText);
        }
    }

    public void hideInputMethod(EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public void showInputMethod(EditText editText) {
        InputMethodManager inputMethodManager = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(editText.getWindowToken(),
                InputMethodManager.SHOW_FORCED, 0);
    }

    public ActivityInfo[] getActivitiesFromPackage(String packageName) {
        ActivityInfo[] activities = null;
        try {
            activities = mContext.getPackageManager().getPackageInfo(packageName,
                    PackageManager.GET_ACTIVITIES).activities;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return activities;
    }

    /**
     * Returns the WindorDecorViews shown on the screen
     * 
     * @return the WindorDecorViews shown on the screen
     */
    public View[] getWindowDecorViews() {
        try {
            return (View[]) ReflectHelper.invoke(viewFetcher, null, "getWindowDecorViews",
                    new Class[] {}, new Object[] {});// mViewFetcher.getActiveDecorView();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the most recent DecorView
     * 
     * @param views
     *            the views to check
     * @return the most recent DecorView
     */
    public View getRecentDecorView(View[] views) {
        try {
            return (View) ReflectHelper.invoke(viewFetcher, null, "getRecentDecorView",
                    new Class[] { View[].class }, new Object[] { views });
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * print FPS of current activity at logcat with TAG FPS
     */
    public void traceFPS() {
        FPSTracer.trace(this);
    }

    /**
     * count how many bytes from tcp app received until now
     * 
     * @param packageName
     * @return
     */
    public static int getPackageRcv(String packageName) {
        return NetworkUtils.getPackageRcv(packageName);
    }

    /**
     * count how many bytes from tcp app sent until now
     * 
     * @param packageName
     * @return
     */
    public static int getPackageSnd(String packageName) {
        return NetworkUtils.getPackageSnd(packageName);
    }

    public String getAppNameByPID(int pid) {
        ActivityManager manager = (ActivityManager) mInstrumentation.getTargetContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        for (RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return "";
    }

    public float getDisplayX() {
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public float getDisplayY() {
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public <T extends View> ArrayList<T> removeInvisibleViews(ArrayList<T> viewList) {
        ArrayList<T> tmpViewList = new ArrayList<T>(viewList.size());
        for (T view : viewList) {
            if (view != null && view.isShown() && isInScreen(view) && !isSize0(view)) {
                tmpViewList.add(view);
            }
        }
        return tmpViewList;
    }

    boolean hasFocus = false;

    /**
     * set focus on a view
     * 
     * @param view
     * @return
     */
    public boolean requestFocus(final View view) {
        if (null == view) {
            return false;
        }

        runOnMainSync(new Runnable() {
            public void run() {
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                hasFocus = view.requestFocus();
            }
        });
        return hasFocus;
    }

    /**
     * These classes can not be used directly, only their class names can be
     * used.Because of com.android.internal.view.menu.MenuView.ItemView can not
     * be compiled with sdk.
     */
    final static String[] MENU_INTERFACES = new String[] { "android.view.MenuItem",
            "com.android.internal.view.menu.MenuView" };

    /**
     * judge a view wether is a menu.
     * 
     * @param view
     * @return true means it is a menu, otherwise return fasle
     */
    public boolean isMenu(View view) {
        return ReflectHelper.getInterfaces(view, MENU_INTERFACES).size() > 0 ? true : false;
    }

    public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean visible) {
        ArrayList<T> views = getCurrentViews(classToFilterBy);
        return visible ? removeInvisibleViews(views) : views;
    }

    @SuppressWarnings("unchecked")
    public <T extends View> ArrayList<T> getViews(Class<T> classToFilterBy,
            boolean onlySufficientlyVisible) {
        ArrayList<T> targetViews = new ArrayList<T>();
        try {
            ArrayList<View> views = (ArrayList<View>) ReflectHelper.invoke(viewFetcher, null,
                    "getViews", new Class[] { View.class, boolean.class }, new Object[] { null,
                            onlySufficientlyVisible });// viewFetcher.getViews(null, false);
            for (View view : views) {
                if (view != null && classToFilterBy.isAssignableFrom(view.getClass())) {
                    targetViews.add(classToFilterBy.cast(view));
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return targetViews;
    }

    public void clickViaPerformClick(final View view, final boolean longClick) {
        Assert.assertTrue("null == view at" + Log.getThreadInfo(), null != view);

        view.post(new Runnable() {
            public void run() {
                int[] xy = getViewCenter(view);
                try {
                    boolean ret = false;
                    if (longClick) {
                        ret = view.performLongClick();
                    } else {
                        ret = view.performClick();
                    }
                    print("clickViaPerformClick:" + ret + " " + xy[0] + "," + xy[1] + " " + view);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static int[] getViewCenter(View view) {
        if (null == view) {
            return new int[] { -1, -1 };
        }
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        float x = xy[0] + (view.getWidth() / 2.0f);
        float y = xy[1] + (view.getHeight() / 2.0f);

        return new int[] { (int) x, (int) y };
    }

    /**
     * Sets an {@code EditText} text. This method is protected by assert.
     * 
     * @param index
     *            the index of the {@code EditText}
     * @param text
     *            the text that should be set
     * @param keepPreviousText
     *            true means append text after old text
     */
    public void enterText(int index, final String text, final boolean keepPreviousText) {
        ArrayList<EditText> editTexts = removeInvisibleViews(getCurrentViews(EditText.class));
        Assert.assertTrue(
                String.format("editTexts.size()[%s] < index[%s]", editTexts.size(), index)
                        + Log.getThreadInfo(), editTexts.size() > index);
        final EditText editText = editTexts.get(index);
        Assert.assertTrue("null == editText [" + index + "]", null != editText);
        Assert.assertTrue("EditText is not enabled [" + index + "]", editText.isEnabled());

        final String previousText = editText.getText().toString();
        runOnMainSync(new Runnable() {
            public void run() {
                editText.setInputType(0);
                editText.performClick();
                if (keepPreviousText) {
                    editText.setText(previousText + text);
                } else {
                    editText.setText(text);
                }
                editText.setCursorVisible(false);
            }
        });
        sleep(500);
        // showInputMethod(editText);
    }

    public void runOnMainSync(Runnable r) {
        mInstrumentation.runOnMainSync(r);
    }

    public void runOnUiThread(Runnable r) {
        getCurrentActivity().runOnUiThread(r);
    }

    public Instrumentation getInstrumentation() {
        return mInstrumentation;
    }

    /**
     * @param R
     * @return
     */
    public View getViewByRString(String R) {
        Class<?> idClass = Strings.getRClass(mActivity.getPackageName(), "id");
        if (null == idClass) {
            return null;
        }

        try {
            Integer id = (Integer) idClass.getDeclaredField(R).get(idClass.newInstance());
            if (null == id) {
                return null;
            }
            // getCurrentActivity().findViewById(id) dose not work
            return getRecentDecorView().findViewById(id);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public View getFocusView(ArrayList<View> views) {
        for (View view : views) {
            if (view.isFocused()) {
                return view;
            }
        }
        return null;
    }

    public boolean isInScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int leftX = location[0];
        int righX = location[0] + view.getWidth();
        int leftY = location[1];
        int righY = location[1] + view.getHeight();
        return righX < 0 || leftX > getDisplayX() || righY < 0 || leftY > getDisplayY() ? false
                : true;
    }

    public <T extends View> ArrayList<T> removeOutOfScreenViews(ArrayList<T> viewList) {
        ArrayList<T> views = new ArrayList<T>(viewList.size());
        for (T view : viewList) {
            if (view != null && isInScreen(view)) {
                views.add(view);
            }
        }
        return views;
    }

    /**
     * judge a view wether be covered
     * 
     * @param view
     * @return
     */
    public boolean isViewCovered(final View view) {
        View currentView = view;
        Rect currentViewRect = new Rect();
        boolean partVisible = currentView.getGlobalVisibleRect(currentViewRect);
        boolean totalHeightVisible = (currentViewRect.bottom - currentViewRect.top) >= view
                .getMeasuredHeight();
        boolean totalWidthVisible = (currentViewRect.right - currentViewRect.left) >= view
                .getMeasuredWidth();
        boolean totalViewVisible = partVisible && totalHeightVisible && totalWidthVisible;
        // if any part of the view is clipped by any of its parents,return true
        if (!totalViewVisible) {
            return true;
        }

        while (currentView.getParent() instanceof ViewGroup) {
            ViewGroup currentParent = (ViewGroup) currentView.getParent();
            // if the parent of view is not visible,return true
            if (currentParent.getVisibility() != View.VISIBLE) {
                return true;
            }

            int start = indexOfViewInParent(currentView, currentParent);
            for (int i = start + 1; i < currentParent.getChildCount(); i++) {
                Rect viewRect = new Rect();
                view.getGlobalVisibleRect(viewRect);
                View otherView = currentParent.getChildAt(i);
                Rect otherViewRect = new Rect();
                otherView.getGlobalVisibleRect(otherViewRect);
                // if view intersects its older brother(covered),return true
                if (Rect.intersects(viewRect, otherViewRect)) {
                    return true;
                }
            }
            currentView = currentParent;
        }
        return false;
    }

    private int indexOfViewInParent(View view, ViewGroup parent) {
        int index;
        for (index = 0; index < parent.getChildCount(); index++) {
            if (parent.getChildAt(index) == view)
                break;
        }
        return index;
    }

    /**
     * This method will cost 100ms to judge whether scrollview stoped.
     * 
     * @param scrollView
     * @return true means scrolling is stop, otherwise return fasle
     */
    public boolean isScrollStoped(final ScrollView scrollView) {
        int x1 = scrollView.getScrollX();
        int y1 = scrollView.getScrollY();
        sleep(100);
        int x2 = scrollView.getScrollX();
        int y2 = scrollView.getScrollY();
        return x1 == x2 && y1 == y2 ? true : false;
    }

    public boolean isSize0(final View view) {
        return view.getHeight() == 0 || view.getWidth() == 0;
    }

    /**
     * This class is only for ViewRecorder not for user.
     */
    public class RecordReplay {

        /**
         * Simulate touching a specific location and dragging to a new location
         * by percent.
         * 
         * @param fromX
         *            X coordinate of the initial touch, in screen coordinates
         * @param toX
         *            Xcoordinate of the drag destination, in screen coordinates
         * @param fromY
         *            X coordinate of the initial touch, in screen coordinates
         * @param toY
         *            Y coordinate of the drag destination, in screen
         *            coordinates
         * @param stepCount
         *            stepCount How many move steps to include in the drag
         * 
         */
        public void dragPercent(float fromXPersent, float toXPersent, float fromYPersent,
                float toYPersent, int stepCount) {
            float fromX = toScreenX(fromXPersent);
            float toX = toScreenX(toXPersent);
            float fromY = toScreenY(fromYPersent);
            float toY = toScreenY(toYPersent);
            // long begin = System.currentTimeMillis();
            drag(fromX, toX, fromY, toY, stepCount);
            print(String.format("fromX:%s toX:%s fromY:%s toY:%s", fromX, toX, fromY, toY));
            // print("duration:" + (System.currentTimeMillis() - begin) + " step:" + stepCount);
        }

        public float toScreenX(float persent) {
            return getDisplayX() * persent;
        }

        public float toScreenY(float persent) {
            return getDisplayY() * persent;
        }

        public float toPercentX(float x) {
            return x / getDisplayX();
        }

        public float toPercentY(float y) {
            return y / getDisplayY();
        }

        private final static String CLASSNAME_DECORVIEW = "com.android.internal.policy.impl.PhoneWindow$DecorView";

        public String getFamilyString(View v) {
            View view = v;
            String familyString = "";
            while (view.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) view.getParent();
                if (null == parent) {
                    print("null == parent at getFamilyString");
                    return rmTheLastChar(familyString);
                }
                if (Build.VERSION.SDK_INT >= 14
                        && parent.getClass().getName().equals(CLASSNAME_DECORVIEW)) {
                } else {
                    familyString += getChildIndex(parent, view) + "-";
                }
                view = parent;
            }

            return rmTheLastChar(familyString);
        }

        private String rmTheLastChar(String str) {
            return str.length() == 0 ? str : str.substring(0, str.length() - 1);
        }

        private int getChildIndex(ViewGroup parent, View child) {
            int countInvisible = 0;
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (parent.getChildAt(i).equals(child)) {
                    return i - countInvisible;
                }
                if (parent.getChildAt(i).getVisibility() != View.VISIBLE) {
                    countInvisible++;
                }
            }
            return -1;
        }

        /**
         * only for getting selected view from AdapterView at clickInList(final
         * int position, String... args)
         */
        private View targetViewInList = null;

        /**
         * @param position
         * @param args
         *            it could be familyString or (resid, index)
         */
        public void clickInList(final int position, String... args) {
            try {
                AdapterView<?> targetView = null;
                if (args.length == 1) {
                    targetView = (AdapterView<?>) waitForView("android.widget.AdapterView", args[0]);
                } else if (args.length == 2) {
                    targetView = (AdapterView<?>) waitForView(args[0], args[1]);
                } else {
                    print("invalid parameters at clickInList");
                }

                final AdapterView<?> adapterView = targetView;
                Assert.assertTrue("null == adapterView at" + Log.getThreadInfo(),
                        null != adapterView);

                targetViewInList = null;
                final long end = System.currentTimeMillis() + 1000;
                runOnMainSync(new Runnable() {

                    @Override
                    public void run() {
                        while (null == targetViewInList && System.currentTimeMillis() < end) {
                            // solution A
                            adapterView.setSelection(position);
                            adapterView.requestFocusFromTouch();
                            adapterView.setSelection(position);
                            sleep(300);// wait setSelection is done
                            targetViewInList = adapterView.getSelectedView();

                            // solution B
                            if (null == targetViewInList
                                    || adapterView.getPositionForView(targetViewInList) != position) {
                                for (int i = 0; i < adapterView.getChildCount(); i++) {
                                    View child = adapterView.getChildAt(i);
                                    if (adapterView.getPositionForView(child) == position) {
                                        print("child index: " + i);
                                        print("getLastVisiblePosition:"
                                                + adapterView.getLastVisiblePosition());
                                        targetViewInList = child;
                                    }
                                }
                            }
                        }
                    }
                });

                // this sleep is necessary
                sleep(1000);
                mTheLastClick = getViewCenter(targetViewInList);
                sleep(1000);
                int[] center = mTheLastClick;
                print("click list[" + adapterView + "] on " + center[0] + ", " + center[1]);
                print("targetViewInList:" + targetViewInList);
                clickOnScreen(center[0], center[1]);
                targetViewInList.performClick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Waits for a WebElement by family string.
         * 
         * @param familyString
         *            timeout how long the function waits if there is no
         *            satisfied web element scroll true if need to scroll the
         *            screen, false otherwise
         * @return WebElement wait for
         */
        @SuppressWarnings("unchecked")
        public WebElement waitForWebElementByFamilyString(String familyString, int timeout,
                boolean scroll) {
            final long endTime = SystemClock.uptimeMillis() + timeout;
            while (true) {
                final boolean timedOut = SystemClock.uptimeMillis() > endTime;
                try {
                    if (timedOut) {
                        // searcher.logMatchesFound(familyString);
                        ReflectHelper.invoke(searcher, null, "logMatchesFound",
                                new Class[] { String.class }, new Object[] { familyString });
                        return null;
                    }
                    ReflectHelper.invoke(sleeper, null, "sleep", new Class[] {}, new Object[] {});
                    String js = "function familyString(s) {var e=document.body;var a=s.split('-');"
                            + "for(var i in a) {e=e.childNodes[parseInt(a[i])];}"
                            + "if(e != null){var id=e.id;var text=e.textContent;"
                            + "var name=e.getAttribute('name');var className=e.className;"
                            + "var tagName=e.tagName;var rect=e.getBoundingClientRect();"
                            + "prompt(id+';,'+text+';,'+name+';,'+className+';,'+tagName+';"
                            + ",'+rect.left+';,'+rect.top+';,'+rect.width+';,'+rect.height);}finished();}"
                            + "familyString('" + familyString + "');";
                    // executeJavaScriptFunction(js);
                    boolean javaScriptWasExecuted = (Boolean) ReflectHelper.invoke(webUtils, null,
                            "executeJavaScriptFunction", new Class[] { String.class },
                            new Object[] { js });
                    // getSufficientlyShownWebElements(javaScriptWasExecuted);
                    ArrayList<WebElement> viewsFromScreen = (ArrayList<WebElement>) ReflectHelper
                            .invoke(webUtils, null, "getSufficientlyShownWebElements",
                                    new Class[] { boolean.class },
                                    new Object[] { javaScriptWasExecuted });
                    List<WebElement> webElements = (List<WebElement>) ReflectHelper.getField(
                            searcher, null, "webElements");
                    // searcher.addViewsToList(webElements, viewsFromScreen);
                    ReflectHelper
                            .invoke(searcher, null, "addViewsToList", new Class[] { List.class,
                                    List.class }, new Object[] { webElements, viewsFromScreen });

                    // searcher.getViewFromList(webElements, 1);
                    WebElement webElementToReturn = (WebElement) ReflectHelper.invoke(searcher,
                            null, "getViewFromList", new Class[] { List.class, int.class },
                            new Object[] { webElements, 1 });

                    if (webElementToReturn != null) {
                        return webElementToReturn;
                    }

                    Object down = ReflectHelper.getField(scroller, null, "DOWN");
                    if (scroll) {
                        // mScroller.scroll(mScroller.DOWN)
                        ReflectHelper.invoke(scroller, null, "scroll", new Class[] { int.class },
                                new Object[] { down });
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Clicks on the WebElement by the given family string.
         * 
         * @param webElement
         *            the WebElement to click
         * 
         */
        public void clickOnWebElementByFamilyString(String familyString) {
            WebElement webElement = waitForWebElementByFamilyString(familyString, WAIT_TIMEOUT,
                    true);
            Assert.assertTrue("There is no web element with familyString : " + familyString,
                    webElement != null);
            clickOnWebElement(webElement);
        }

        /**
         * Enters text in a WebElement by family string
         * 
         * @param familyString
         *            the String object, used to locates an identified element
         * @param text
         *            the text to enter
         * 
         */
        public void enterTextInWebElementByFamilyString(String familyString, String text) {
            if (waitForWebElementByFamilyString(familyString, WAIT_TIMEOUT, false) == null) {
                Assert.assertTrue("There is no web element with familyString : " + familyString,
                        false);
            }
            String js = "function enterTextByFamilyString(s,t) {var e=document;var a=s.split('-');for(var i in a) {e=e.childNodes[parseInt(a[i])];}if(e != null){e.value=t}finished();}"
                    + "enterTextByFamilyString('" + familyString + "','" + text + "');";

            try {
                ReflectHelper.invoke(webUtils, null, "executeJavaScriptFunction",
                        new Class[] { String.class }, new Object[] { js });// executeJavaScriptFunction(js);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        private View pickViewByFamilyString(String className, String familyString) {
            ArrayList<View> views = getCurrentViews();
            for (View view : views) {
                if (getFamilyString(view).equals(familyString)) {
                    if (null == className) {
                        return view;
                    }
                    String viewClassName = view.getClass().getName();
                    try {
                        if (viewClassName.equals(className)
                                || Class.forName(className).isAssignableFrom(view.getClass())) {
                            return view;
                        }
                    } catch (ClassNotFoundException e) {
                        // ignore it
                    }
                }
            }
            return null;
        }

        /**
         * for debug when get view failed
         */
        private void printViews(String characteristic) {
            ArrayList<View> invisibleViews = getCurrentViews();
            for (View view : invisibleViews) {
                String familyString = getFamilyString(view);
                String resId = getResName(view);
                String star = "";
                if (familyString.equals(characteristic) || resId.equals(characteristic)) {
                    star = "*";
                    int[] xy = new int[2];
                    view.getLocationOnScreen(xy);
                    print(xy[0] + "," + xy[1]);
                    print("isSize0:" + isSize0(view));
                    print("isShown:" + view.isShown());
                }
                print(String.format("%s[%s][%s][%s][%s]", star, familyString, resId, view,
                        getViewText(view)));
            }
        }

        /**
         * This method is protected by assert.
         * 
         * @param familyString
         * @param text
         */
        public void waitForTextByFamilyString(String familyString, String text) {
            View view = waitForView(null, familyString);
            String actual = getViewText(view);
            Assert.assertTrue(String.format("Except text [%s], Actual text [%s]", text, actual),
                    actual.equals(text));
        }

        private final static int WAIT_TIMEOUT = 20000;

        /**
         * This method is protected by assert. If arg1 contains "id/", arg1 will
         * be judged to resid otherwise it will be judged to className.
         * 
         * @param arg1
         *            it could be className or resid
         * @param arg2
         *            it could be familyString or index
         * @return the view picked
         */
        public View waitForView(String arg1, String arg2) {
            boolean useResId = arg1 != null && arg1.contains("id/") ? true : false;
            long endTime = System.currentTimeMillis() + WAIT_TIMEOUT;
            while (System.currentTimeMillis() < endTime) {
                View targetView = null;
                // it must be the same as ViewRecorder.getTargetViews()
                if (useResId) {
                    targetView = getViewByResName(arg1, Integer.valueOf(arg2));
                } else {
                    targetView = pickViewByFamilyString(arg1, arg2);
                }

                if (targetView != null) {
                    return targetView;
                }

                sleep(500);
            }

            if (useResId) {
                printViews(arg1);
            } else {
                printViews(arg2);
            }
            Assert.assertTrue(String.format("waitForView failed! arg1[%s] arg2[%s]", arg1, arg2),
                    false);
            return null;
        }

        /**
         * Clicks on a {@code View} of a specific class with a certain
         * familyString or a specific resource id with a index.
         * 
         * This method is protected by assert.
         * 
         * @param arg1
         *            it could be className or resid
         * @param arg2
         *            it could be familyString or index
         * @param longClick
         *            true means long click
         */
        public void clickOn(String arg1, String arg2, boolean longClick) {
            try {
                View view = waitForView(arg1, arg2);
                clickViaPerformClick(view, longClick);
                // clickOnView();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void clickOnExpandableListView(final String familyString, final int flatListPosition) {
            final ExpandableListView expandableListView = (ExpandableListView) waitForView(
                    "android.widget.ExpandableListView", familyString);
            Assert.assertTrue("null == adapterView at" + Log.getThreadInfo(),
                    null != expandableListView);

            runOnMainSync(new Runnable() {

                @Override
                public void run() {
                    View v = expandableListView.getChildAt(flatListPosition);
                    mTheLastClick = getViewCenter(v);
                    long id = expandableListView.getItemIdAtPosition(flatListPosition);
                    expandableListView.performItemClick(v, flatListPosition, id);
                }
            });
        }

        public void scrollListToLine(final int line, String... args) {
            AbsListView targetView = null;
            if (args.length == 1) {
                targetView = (AbsListView) waitForView("android.widget.AbsListView", args[0]);
            } else if (args.length == 2) {
                targetView = (AbsListView) waitForView(args[0], args[1]);
            } else {
                print("invalid parameters at clickInList");
            }
            Assert.assertTrue("null == absListView at" + Log.getThreadInfo(), null != targetView);

            try {
                ReflectHelper.invoke(scroller, null, "scrollListToLine", new Class[] {
                        AbsListView.class, int.class }, new Object[] { targetView, line });
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        public void scrollScrollViewTo(final String familyString, final int x, final int y) {
            final ScrollView scrollView = (ScrollView) waitForView("android.widget.ScrollView",
                    familyString);
            runOnMainSync(new Runnable() {
                public void run() {
                    scrollView.scrollBy(x, y);
                }
            });
        }
    }

    public static double countDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.abs(x1 - x2) * Math.abs(x1 - x2) + Math.abs(y1 - y2)
                * Math.abs(y1 - y2));
    }

    /**
     * get view index by its class at current activity
     * 
     * @param view
     * @return -1 means not found;otherwise is then index of view
     */
    public int getCurrentViewIndex(View view) {
        if (null == view) {
            return -1;
        }

        ArrayList<? extends View> views = removeInvisibleViews(getCurrentViews(view.getClass()));
        for (int i = 0; i < views.size(); i++) {
            if (views.get(i).equals(view)) {
                return i;
            }
        }
        return -1;
    }

    public void dumpPage() {
        ArrayList<String> webElementsString = getWebElementsString();
        print("############# dumpPage begin #################");
        for (String line : webElementsString) {
            print(line);
        }
        print("############# dumpPage end #################");
    }

    public ArrayList<String> getWebElementsString() {
        ArrayList<String> webElementsString = new ArrayList<String>();
        ArrayList<WebElement> elements = getCurrentWebElements();

        for (WebElement element : elements) {
            StringBuilder sb = new StringBuilder();
            sb.append('(').append(element.getLocationX()).append(',')
                    .append(element.getLocationY()).append(") , {tagName : ")
                    .append(element.getTagName()).append("} , {id : ").append(element.getId())
                    .append("} , {className : ").append(element.getClassName())
                    .append("} , {name : ").append(element.getName()).append("} , {text : ")
                    .append(element.getText()).append('}');
            webElementsString.add(sb.toString());
        }

        return webElementsString;
    }

    private static String            mUrl   = null;
    private static ArrayList<String> mTexts = new ArrayList<String>();

    /**
     * dump text of activity including webview
     * 
     * @param saveDuplicate
     *            true means save duplicate text
     */
    public void dumpActivityText(boolean saveDuplicate) {
        String activity = getCurrentActivity().getClass().getName();
        for (TextView textView : getCurrentViews(TextView.class)) {
            String text = textView.getText().toString();
            if ("".equals(text)) {
                continue;
            }
            int[] xy = new int[2];
            textView.getLocationOnScreen(xy);
            float size = textView.getTextSize();
            String color = "0x" + Integer.toHexString(textView.getTextColors().getDefaultColor());

            if (saveDuplicate) {
                print(String.format("[%s][%s][%s]", activity, size, text));
                continue;
            } else if (!isContains(text)) {
                mTexts.add(text);
                print(String.format("[%s][%s,%s][%s][%s][%s]", activity, xy[0], xy[1], size, color,
                        text));
            }
        }

        // dump web
        print(new Strings(getWebElementsString()).toString());
        final ArrayList<WebView> webViews = getCurrentViews(WebView.class);
        if (webViews.size() < 1) {
            return;
        }
        runOnMainSync(new Runnable() {

            @Override
            public void run() {
                mUrl = webViews.get(0).getUrl();
            }
        });
        if (!isContains(mUrl)) {
            mTexts.add(mUrl);
            print(String.format("[URL][%s][%s]", activity, mUrl));
        }
    }

    private boolean isContains(String target) {
        for (String str : mTexts) {
            if (str.equals(target)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("Recycle")
    public void clickViewWithoutAssert(View view) {
        if (null == view) {
            Logger.println("null == view at clickViewWithoutAssert");
            return;
        }

        ArrayList<TextView> textViews = APPTraveler.local.getCurrentViews(TextView.class, view);
        for (TextView textView : textViews) {
            String text = textView.getText().toString();
            if (!"".equals(text)) {
                Logger.printTextln(String.format("Click On [%s]", text));
                break;
            }
        }

        int[] xy = getViewCenter(view);
        int x = xy[0];
        int y = xy[1];
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y,
                0);
        MotionEvent event2 = MotionEvent
                .obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        try {
            mInstrumentation.sendPointerSync(event);
            mInstrumentation.sendPointerSync(event2);
            sleep(MINISLEEP);
        } catch (SecurityException e) {
            e.printStackTrace();
            // Assert.assertTrue("Click can not be completed!", false);
        }
    }

    /**
     * @param depth
     * @param username
     * @param password
     */
    public void travel(int depth, String username, String password) {
        new APPTraveler(CafeTestCase.remote, this, username, password).travel(depth);
    }

    public void travel(int depth) {
        travel(depth, null, null);
    }

    public void travel() {
        travel(4, null, null);
    }

    public String getStringFromArguments(String key) {
        return CafeTestRunner.mArguments.getString(key);
    }
}
