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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
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
import com.baidu.cafe.local.record.ViewRecorder;
import com.baidu.cafe.local.traveler.APPTraveler;
import com.baidu.cafe.local.traveler.Logger;
import com.baidu.cafe.utils.ReflectHelper;
import com.baidu.cafe.utils.ShellExecute;
import com.baidu.cafe.utils.ShellExecute.CommandResult;
import com.baidu.cafe.utils.Strings;
import com.jayway.android.robotium.solo.WebElement;

import dalvik.system.DexFile;

/**
 * It can help you as below.
 * 
 * 1.get or set a object's private property and invoke a object's private
 * function
 * 
 * 2.find view by text
 * 
 * 3.get views which is generated dynamically
 * 
 * 4.record hands operation and generate Cafe code
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-5-17
 * @version
 * @todo
 */

public class LocalLib extends SoloEx {
    public final static int       SEARCHMODE_COMPLETE_MATCHING = 1;
    public final static int       SEARCHMODE_DEFAULT           = 1;
    public final static int       SEARCHMODE_INCLUDE_MATCHING  = 2;
    public final static int       WAIT_INTERVAL                = 1000;

    public static String          mTestCaseName                = null;
    public static String          mPackageName                 = null;
    public static int[]           mTheLastClick                = new int[2];
    public static Instrumentation mInstrumentation;

    private final static int      MINISLEEP                    = 100;

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
    public Object invokeObjectMethod(Object owner, int classLevel, String methodName,
            Class<?>[] parameterTypes, Object[] parameters) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        return ReflectHelper.invoke(owner, classLevel, methodName, parameterTypes, parameters);
    }

    /**
     * set object's private property with custom value
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
    public void setObjectProperty(Object owner, int classLevel, String fieldName, Object value)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        ReflectHelper.setObjectProperty(owner, classLevel, fieldName, value);
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
    public static Object getObjectProperty(Object owner, int classLevel, String fieldName)
            throws SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        return ReflectHelper.getObjectProperty(owner, classLevel, fieldName);
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
    public static ArrayList<String> getPropertyNameByType(Object owner, int classLevel,
            Class<?> type) {
        return ReflectHelper.getPropertyNameByType(owner, classLevel, type);
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
    public static ArrayList<String> getPropertyNameByValue(Object owner, int classLevel,
            Class<?> valueType, Object value) throws IllegalArgumentException,
            IllegalAccessException {
        return ReflectHelper.getPropertyNameByValue(owner, classLevel, valueType, value);
    }

    /**
     * add listeners on all views for generating Cafe code automatically
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
                Object mListenerInfo = ReflectHelper
                        .getObjectProperty(view, level, "mListenerInfo");
                return null == mListenerInfo ? null : ReflectHelper.getObjectProperty(
                        mListenerInfo, 0, fieldName);
            } else {
                return ReflectHelper.getObjectProperty(view, level, fieldName);
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
                Object mListenerInfo = ReflectHelper
                        .getObjectProperty(view, level, "mListenerInfo");
                if (null == mListenerInfo) {
                    return;
                }
                ReflectHelper.setObjectProperty(mListenerInfo, 0, fieldName, value);
            } else {
                ReflectHelper.setObjectProperty(view, level, fieldName, value);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // System.err.println("view:" + view);
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

    private int getRStringId(String packageName, String stringName) {
        Class<?> stringClass = getRClass(packageName, "string");
        if (null == stringClass) {
            return -1;
        }
        try {
            return (Integer) stringClass.getDeclaredField(stringName)
                    .get(stringClass.newInstance());
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
        return -1;
    }

    public String getRIdNameByValue(String packageName, int value) {
        Class<?> idClass = getRClass(packageName, "id");
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
     * NOTICE: This method can not work at apk which is chaos.
     * 
     * @param packageName
     * @param className
     * @return
     */
    private Class<?> getRClass(String packageName, String className) {
        try {
            Class<?>[] classes = Class.forName(packageName + ".R").getDeclaredClasses();
            for (int i = 0; i < classes.length; i++) {
                if (classes[i].getName().indexOf("$" + className) != -1) {
                    return classes[i];
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
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
                getRStringId(mContext.getPackageName(), stringName));
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
        return getString(getRStringId(mActivity.getPackageName(), stringName));
    }

    /**
     * you can use this function when getActivtiy is hang. when you want to
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
     *            "id/workspace" timeout is default 8000 scroll is default true
     *            only visible is default true
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId) {
        return waitForViewVanishById(resId, 8000, true, true);
    }

    /**
     * Waits for a view to vanish
     * 
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay milliseconds scroll is default true only visible is
     *            default true
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId, long timeout) {
        return waitForViewVanishById(resId, timeout, true, true);
    }

    /**
     * Waits for a view to vanish
     * 
     * @param resId
     *            the id you see in hierarchy . for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay milliseconds
     * @param scroll
     *            true you want to scroll onlyvisible is default true
     * @return true we get it
     */
    public boolean waitForViewVanishById(String resId, long timeout, boolean scroll) {
        return waitForViewVanishById(resId, timeout, scroll, true);
    }

    /**
     * Waits for a view vanished
     * 
     * @param resId
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
    public boolean waitForViewVanishById(String resId, long timeout, boolean scroll,
            boolean onlyvisible) {
        Long end = System.currentTimeMillis() + timeout;
        while (true) {
            if (System.currentTimeMillis() > end) {
                return false;
            }
            if (!waitforViewById(resId, WAIT_INTERVAL, scroll, onlyvisible)) {
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
        ArrayList<View> viewList = getViews();
        for (View view : viewList) {
            if (view instanceof android.widget.TabWidget)
                tabList.add((TabWidget) view);
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
            if (tab == null) {
                Assert.assertTrue("Tab is null", false);
            }
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
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace" timeout is default 3000 scroll is default true
     *            onlyVisible is default true
     * @return true we get it
     */
    public boolean waitforViewById(String resId) {
        return waitforViewById(resId, 3000, true, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay millisecond scroll is default true onlyVisible is
     *            default true
     * @return true we get it
     */
    public boolean waitforViewById(String resId, long timeout) {
        return waitforViewById(resId, timeout, true, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param timeout
     *            the delay millisecond
     * @param scroll
     *            true you want to scroll onlyVisible is default true
     * @return true we get it
     */
    public boolean waitforViewById(String resId, long timeout, boolean scroll) {
        return waitforViewById(resId, timeout, scroll, true);
    }

    /**
     * wait for a specified view
     * 
     * @param resId
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
    public boolean waitforViewById(String resId, long timeout, boolean scroll, boolean onlyVisible) {
        final long endTime = System.currentTimeMillis() + timeout;

        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                return false;
            }

            final boolean isResIdShow = isResIdShow(resId, onlyVisible);

            if (isResIdShow) {
                return true;
            }

            if (scroll
                    && !(Boolean) invoke(mScroller, "scroll", new Class[] { int.class },
                            new Object[] { getField(mScroller, "DOWN") })) { // mScroller.scroll(mScroller.DOWN)
                continue;
            }
            invoke(mSleeper, "sleep"); // mSleeper.sleep();
        }
    }

    /**
     * click a specified view
     * 
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @return true we got it
     */
    public boolean clickViewById(String resId) {
        return clickViewById(resId, 0);
    }

    /**
     * @param resId
     *            the id you see in hierarchy. for example in Launcher
     *            "id/workspace"
     * @param index
     *            Clicks on an resId with a given index.
     * @return true we got it
     */
    public boolean clickViewById(String resId, int index) {
        final View view = getViewById(resId, index);

        if (null == view) {
            return false;
        }

        clickOnView(view);
        return true;
    }

    /**
     * Get view by ID
     * 
     * @param resId
     *            resource ID
     * @return null means not found
     */
    public View getViewById(String resId) {
        return getViewById(resId, 0, LocalLib.SEARCHMODE_COMPLETE_MATCHING);
    }

    /**
     * Get view by ID
     * 
     * @param resId
     *            resource ID
     * @param index
     *            the index of views
     * @return null means not found
     */
    public View getViewById(String resId, int index) {
        return getViewById(resId, index, LocalLib.SEARCHMODE_COMPLETE_MATCHING);
    }

    /**
     * Get View By Id
     * 
     * @param resId
     * @param index
     *            the index of views
     * @param searchMode
     *            include SEARCHMODE_COMPLETE_MATCHING, SEARCHMODE_DEFAULT and
     *            SEARCHMODE_INCLUDE_MATCHING
     * @return null means not found
     */
    public View getViewById(String resId, int index, int searchMode) {
        ArrayList<View> views = getViews();
        int number = 0;

        for (View view : views) {
            String strid = "";
            int resid = view.getId();
            if (false == view.isShown() || View.NO_ID == resid) {
                continue;
            }

            try {
                strid = view.getResources().getResourceName(resid);
                // print(strid +
                // "  views.get(i).getResources().getResourceName(resid) is " +
                // strid);
            } catch (Resources.NotFoundException e) {
                // print("resid num " + resid + " dose not have id");
                continue;
            }

            if (searchMode == LocalLib.SEARCHMODE_INCLUDE_MATCHING && strid.contains(resId)) {
                print("include mode;  strid is " + strid);
                number++;
            } else if (searchMode == LocalLib.SEARCHMODE_COMPLETE_MATCHING
                    && strid.split(":")[1].trim().equals(resId)) {
                print("complete mode; strid is " + strid);
                number++;
            }

            if (number - 1 == index) {
                return view;
            }
        }

        return null;
    }

    private boolean isResIdShow(String resId, boolean isVisiable) {
        boolean flag = false;
        ArrayList<View> viewArray = getViews();
        for (View view : viewArray) {
            if ((true == isVisiable)
                    && ((view.getVisibility() == View.GONE) || (view.getVisibility() == View.INVISIBLE))) {
                continue;
            }
            int resid = view.getId();
            // we only concern the shown view
            if (false == view.isShown() || View.NO_ID == resid) {
                continue;
            }
            String strid;
            try {
                strid = view.getResources().getResourceName(resid);
                // print(strid +
                // "  viewArray.get(i).getResources().getResourceName(resid) is "
                // + strid);
            } catch (Resources.NotFoundException e) {
                // print("resid num " + resid + " dose not have id");
                continue;
            }
            if (strid.contains(resId)) {
                flag = true;
                break;
            }
        }
        return flag;
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
        ArrayList<TextView> textViews = (ArrayList<TextView>) invoke(mViewFetcher,
                "getCurrentViews", new Class[] { Class.class, View.class }, new Object[] {
                        TextView.class, parent }); // mViewFetcher.getCurrentViews(TextView.class,
        // parent);

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
                print("Unknown searchMode!");
                return false;
            }
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
        File cafe = new File(packagePath);
        if (!cafe.exists()) {
            cafe.mkdir();
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
     *            e.g. "com.baidu.chunlei.exercise.test"
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
                String name = (String) entries.nextElement();
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

    public void isInputMethodShown() {
        InputMethodManager inputMethodManager = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        print("isAcceptingText:" + inputMethodManager.isAcceptingText());
        print("isActive:" + inputMethodManager.isActive());
        print("isFullscreenMode:" + inputMethodManager.isFullscreenMode());
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
    public static View[] getWindowDecorViews() {
        return (View[]) invoke(mViewFetcher, "getWindowDecorViews"); // mViewFetcher.getActiveDecorView();
    }

    /**
     * Returns the most recent DecorView
     * 
     * @param views
     *            the views to check
     * @return the most recent DecorView
     */
    public static View getRecentDecorView(View[] views) {
        return (View) invoke(mViewFetcher, "getRecentDecorView", new Class[] { View[].class },
                new Object[] { views });
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

    public boolean isMenu(View view) {
        return ReflectHelper.getObjectInterfaces(view, MENU_INTERFACES).size() > 0 ? true : false;
    }

    public <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, boolean visible) {
        ArrayList<T> views = getCurrentViews(classToFilterBy);
        return visible ? removeInvisibleViews(views) : views;
    }

    /**
     * Clicks on a {@code View} of a specific class, with a certain
     * familyString.
     * 
     * This method is protected by assert.
     * 
     * @param className
     *            what kind of {@code View} to click, e.g. {@code Button.class}
     *            or {@code ImageView.class}
     * @param familyString
     *            the family relationship of the {@code View} to be clicked
     * @param longClick
     *            true means long click
     */
    public <T extends View> void clickOn(String className, String familyString, boolean longClick) {
        try {
            View view = waitForViewByFamilyString(familyString, className);
            clickOnViewWithoutScroll(view, longClick);// waitForView is including
            // clickOnView
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T extends View> ArrayList<T> getViews(Class<T> classToFilterBy,
            boolean onlySufficientlyVisible) {
        ArrayList<T> targetViews = new ArrayList<T>();
        ArrayList<View> views = (ArrayList<View>) invoke(mViewFetcher, "getViews", new Class[] {
                View.class, boolean.class }, new Object[] { null, onlySufficientlyVisible });// viewFetcher.getViews(null,
        // false);
        for (View view : views) {
            if (view != null && classToFilterBy.isAssignableFrom(view.getClass())) {
                targetViews.add(classToFilterBy.cast(view));
            }
        }
        return targetViews;
    }

    public void clickViaPerformClick(final View view, final boolean longClick) {
        if (null == view) {
            print("null == view at clickViaPerformClick");
            return;
        }

        view.post(new Runnable() {
            public void run() {
                int[] xy = getViewCenter(view);
                print("clickViaPerformClick:" + xy[0] + "," + xy[1] + " " + view);
                print("familyString:" + getFamilyString(view));
                try {
                    if (longClick) {
                        view.performLongClick();
                    } else {
                        view.performClick();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    clickOnView(view);
                }
            }
        });
    }

    private final int SMALLTIMEOUT = 10000;

    public void clickOnViewWithoutScroll(View view, boolean longClick) {
        // invoke(mWaiter, "waitForView", new Class[] { View.class, int.class,
        // boolean.class },
        // new Object[] { view, SMALLTIMEOUT, false });//
        // waiter.waitForView(view, SMALLTIMEOUT, false);
        mTheLastClick = getViewCenter(view);
        // invoke(mClicker, "clickOnScreen", new Class[] { View.class }, new
        // Object[] { view });// clicker.clickOnScreen(view);
        clickViaPerformClick(view, longClick);
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

    private final int TIMEOUT = 20000;

    /**
     * This method is protected by assert.
     * 
     * @param familyString
     * @return
     */
    public View getViewByFamilyString(String familyString, String className) {
        long endTime = System.currentTimeMillis() + TIMEOUT;
        int decorViewIndex = Integer.valueOf("" + familyString.charAt(familyString.length() - 1));

        // get views from current activity
        while (System.currentTimeMillis() < endTime) {
            //            ArrayList<View> views = removeInvisibleViews(getCurrentViews(View.class));
            // it must be the same as ViewRecorder.getTargetViews()
            ArrayList<View> views = removeInvisibleViews(getCurrentViews(View.class,
                    LocalLib.getWindowDecorViews()[decorViewIndex]));
            for (View view : views) {
                if (getFamilyString(view).equals(familyString)) {
                    if (null == className) {
                        return view;
                    }
                    String viewClassName = view.getClass().getName();
                    try {
                        if (viewClassName.equals(className)
                                || Class.forName(className).isAssignableFrom(view.getClass())) {
                            print("decorViewIndex:" + decorViewIndex);
                            return view;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            sleep(500);
        }

        printViews(familyString);
        Assert.assertTrue(
                String.format("getViewByFamilyString == null! familyString[%s]", familyString),
                false);
        return null;
    }

    /**
     * for debug when get view failed
     */
    private void printViews(String targetFamilyString) {
        View[] decorViews = LocalLib.getWindowDecorViews();
        print("########## decorViews: " + decorViews.length);
        for (View decorView : decorViews) {
            ArrayList<View> invisibleViews = removeInvisibleViews(getCurrentViews(View.class,
                    decorView));
            for (View view : invisibleViews) {
                String familyString = getFamilyString(view);
                String star = "";
                if (familyString.equals(targetFamilyString)) {
                    star = "*";
                    int[] xy = new int[2];
                    view.getLocationOnScreen(xy);
                    print(xy[0] + "," + xy[1]);
                    print("isSize0:" + isSize0(view));
                    print("isShown:" + view.isShown());
                }
                print(String.format("%s[%s][%s][%s]", star, getFamilyString(view), view,
                        getViewText(view)));
            }
        }
    }

    /**
     * This method is protected by assert.
     * 
     * @param familyString
     * @param className
     * @return
     */
    public View waitForViewByFamilyString(String familyString, String className) {
        View view = getViewByFamilyString(familyString, className);
        String exceptViewClassName = view.getClass().getName();
        String assertString = String.format("Except view [%s], Actual view [%s]", className,
                exceptViewClassName);
        if (!exceptViewClassName.equals(className)) {
            printViews(familyString);
            Assert.assertTrue(assertString, false);
        }
        return view;
    }

    /**
     * This method is protected by assert.
     * 
     * @param familyString
     * @param text
     */
    public void waitForTextByFamilyString(String familyString, String text) {
        View view = getViewByFamilyString(familyString, null);
        String actual = getViewText(view);
        Assert.assertTrue(String.format("Except text [%s], Actual text [%s]", text, actual),
                actual.equals(text));
    }

    /**
     * Sets an {@code EditText} text
     * 
     * @param familyString
     *            the familyString of the {@code EditText}
     * @param text
     *            the text that should be set
     */
    public void enterText(String familyString, final String text, final boolean keepPreviousText) {
        final EditText editText = (EditText) getViewByFamilyString(familyString,
                "android.widget.EditText");

        if (null == editText) {
            Assert.assertTrue("null == editText [" + familyString + "]", false);
        }
        if (!editText.isEnabled()) {
            Assert.assertTrue("Edit text is not enabled [" + familyString + "]", false);
        }

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

    boolean isClicked = false;

    public boolean clickOnExpandableListView(final String familyString, final int flatListPosition) {
        final ExpandableListView expandableListView = (ExpandableListView) getViewByFamilyString(
                familyString, "android.widget.ExpandableListView");
        if (null == expandableListView) {
            print("null == adapterView");
            return false;
        }

        runOnMainSync(new Runnable() {

            @Override
            public void run() {
                View v = expandableListView.getChildAt(flatListPosition);
                mTheLastClick = getViewCenter(v);
                long id = expandableListView.getItemIdAtPosition(flatListPosition);
                isClicked = expandableListView.performItemClick(v, flatListPosition, id);
            }
        });
        return isClicked;
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
        Class<?> idClass = getRClass(mActivity.getPackageName(), "id");
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

    public void scrollListToLineWithFamilyString(final int line, final String familyString) {
        AbsListView absListView = null;
        try {
            absListView = (AbsListView) getViewByFamilyString(familyString,
                    "android.widget.AbsListView");
        } catch (Exception e) {
            e.printStackTrace();
            printViews(familyString);
            System.exit(1);
        }

        if (null == absListView) {
            print("null == absListView");
            return;
        }

        invoke(mScroller, "scrollListToLine", new Class[] { AbsListView.class, int.class },
                new Object[] { absListView, line });
    }

    public void scrollScrollViewToWithFamilyString(final String familyString, final int x,
            final int y) {
        final ScrollView scrollView = (ScrollView) getViewByFamilyString(familyString,
                "android.widget.ScrollView");
        runOnMainSync(new Runnable() {
            public void run() {
                scrollView.scrollBy(x, y);
            }
        });
    }

    /**
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
     * @return
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

    public boolean waitForView(String className, final int index, final int timeout,
            final boolean scroll) {
        boolean ret = false;
        try {
            Class<?> viewClass = Class.forName(className);
            ret = (Boolean) invoke(mWaiter, "waitForView", new Class[] { Class.class, int.class,
                    int.class, boolean.class }, new Object[] { viewClass, index, timeout, scroll });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        sleep(500);
        return ret;
    }

    final static String CLASSNAME_DECORVIEW = "com.android.internal.policy.impl.PhoneWindow$DecorView";

    public String getFamilyString(View v) {
        View view = v;
        String familyString = "";
        while (view.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null == parent) {
                print("null == parent at getFamilyString");
                return familyString;
            }
            if (Build.VERSION.SDK_INT >= 14
                    && parent.getClass().getName().equals(CLASSNAME_DECORVIEW)) {
            } else {
                familyString += getChildIndex(parent, view);
            }
            view = parent;
        }

        // add decorview index
        familyString += getDecorViewIndex(v);
        return familyString;
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

    private int getDecorViewIndex(View targetView) {
        View[] decorViews = LocalLib.getWindowDecorViews();
        if (decorViews.length == 1) {
            return 0;
        }

        for (int i = 0; i < decorViews.length; i++) {
            ArrayList<View> currentiews = getCurrentViews(View.class, decorViews[i]);
            for (View view : currentiews) {
                if (view.equals(targetView)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * use clickOnView() different from clickOn() which use performClick()
     * 
     * @param itemFamilyString
     */
    public void clickInListWithFamilyString(String itemFamilyString) {
        try {
            View view = getViewByFamilyString(itemFamilyString, null);
            mTheLastClick = getViewCenter(view);
            print("clickInListWithFamilyString:" + mTheLastClick[0] + "," + mTheLastClick[1]);
            clickOnView(view);
            // clickOnViewWithoutScroll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulate touching a specific location and dragging to a new location.
     * 
     * This method was copied from {@code TouchUtils.java} in the Android Open
     * Source Project, and modified here.
     * 
     * @param fromX
     *            X coordinate of the initial touch, in screen coordinates
     * @param toX
     *            Xcoordinate of the drag destination, in screen coordinates
     * @param fromY
     *            X coordinate of the initial touch, in screen coordinates
     * @param toY
     *            Y coordinate of the drag destination, in screen coordinates
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

    public static double getDistance(float x1, float y1, float x2, float y2) {
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
        // View[] views = getUniqueViews(getCurrentViews(view.getClass(),
        // true));
        // for (int i = 0; i < views.length; i++) {
        // if (views[i].equals(view)) {
        // return i;
        // }
        // }
        return -1;
    }

    /**
     * Waits for a WebElement by family string.
     * 
     * @param familyString
     *            timeout how long the function waits if there is no satisfied
     *            web element scroll true if need to scroll the screen, false
     *            otherwise
     * @return WebElement wait for
     */
    public WebElement waitForWebElementByFamilyString(String familyString, int timeout,
            boolean scroll) {
        final long endTime = SystemClock.uptimeMillis() + timeout;
        while (true) {
            final boolean timedOut = SystemClock.uptimeMillis() > endTime;

            if (timedOut) {
                // searcher.logMatchesFound(familyString);
                invoke(mSearcher, "logMatchesFound", new Class[] { String.class },
                        new Object[] { familyString });
                return null;
            }
            invoke(this.mSleeper, "sleep");
            String js = "function familyString(s) {var e=document;var a=s.split('-');for(var i in a) {e=e.childNodes[parseInt(a[i])];}if(e != null){var id=e.id;var text=e.textContent;var name=e.getAttribute('name');var className=e.className;var tagName=e.tagName;var rect=e.getBoundingClientRect();prompt(id+';,'+text+';,'+name+';,'+className+';,'+tagName+';,'+rect.left+';,'+rect.top+';,'+rect.width+';,'+rect.height);}finished();}"
                    + "familyString('" + familyString + "');";
            // executeJavaScriptFunction(js);
            boolean javaScriptWasExecuted = (Boolean) invoke(mWebUtils,
                    "executeJavaScriptFunction", new Class[] { String.class }, new Object[] { js });
            // getSufficientlyShownWebElements(javaScriptWasExecuted);
            ArrayList<WebElement> viewsFromScreen = (ArrayList<WebElement>) invoke(mWebUtils,
                    "getSufficientlyShownWebElements", new Class[] { boolean.class },
                    new Object[] { javaScriptWasExecuted });

            try {
                List<WebElement> webElements = (List<WebElement>) ReflectHelper.getObjectProperty(
                        mSearcher, 0, "webElements");
                // searcher.addViewsToList(webElements, viewsFromScreen);
                invoke(mSearcher, "addViewsToList", new Class[] { List.class, List.class },
                        new Object[] { webElements, viewsFromScreen });

                // searcher.getViewFromList(webElements, 1);
                WebElement webElementToReturn = (WebElement) invoke(mSearcher, "getViewFromList",
                        new Class[] { List.class, int.class }, new Object[] { webElements, 1 });

                if (webElementToReturn != null) {
                    return webElementToReturn;
                }

                if (scroll) {
                    // mScroller.scroll(mScroller.DOWN)
                    invoke(mScroller, "scroll", new Class[] { int.class },
                            new Object[] { getField(mScroller, "DOWN") });
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
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
        WebElement webElement = waitForWebElementByFamilyString(familyString, TIMEOUT, true);
        if (webElement == null) {
            Assert.assertTrue("There is no web element with familyString : " + familyString, false);
        }
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
        if (waitForWebElementByFamilyString(familyString, TIMEOUT, false) == null) {
            Assert.assertTrue("There is no web element with familyString : " + familyString, false);
        }
        String js = "function enterTextByFamilyString(s,t) {var e=document;var a=s.split('-');for(var i in a) {e=e.childNodes[parseInt(a[i])];}if(e != null){e.value=t}finished();}"
                + "enterTextByFamilyString('" + familyString + "','" + text + "');";
        // executeJavaScriptFunction(js);
        invoke(mWebUtils, "executeJavaScriptFunction", new Class[] { String.class },
                new Object[] { js });
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
            APPTraveler.local.sleep(MINISLEEP);
        } catch (SecurityException e) {
            e.printStackTrace();
            //            Assert.assertTrue("Click can not be completed!", false);
        }
    }
}
