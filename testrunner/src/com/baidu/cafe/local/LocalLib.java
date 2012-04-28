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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

import junit.framework.Assert;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TimePicker;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.mockclient.MockConstant;
import com.baidu.cafe.mockclient.MockHelper;

import dalvik.system.DexFile;

/**
 * It can help you as below. 1.get or set a object's private property and invoke
 * a object's private function 2.find view by text 3.get views generated
 * dynamically 4.record hands operation and generate Robotium code
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-5-17
 * @version
 * @todo
 */

/**
 * NOTICE: Non YiOperator user should close B line and open A line! And remove
 * YiOperator.java.
 */

// public class LocalLib extends SoloEx { // A
public class LocalLib extends YiOperator { // B
    public final static int      SEARCHMODE_COMPLETE_MATCHING = 1;
    public final static int      SEARCHMODE_DEFAULT           = 1;
    public final static int      SEARCHMODE_INCLUDE_MATCHING  = 2;
    public final static int      WAIT_INTERVAL                = 1000;

    private boolean              mHasBegin                    = false;
    private ArrayList<View>      mViews                       = null;
    private OnClickListener      mOnClickListener;
    private OnLongClickListener  mOnLongClickListener;
    private OnTouchListener      mOnTouchListener;
    private OnKeyListener        mOnKeyListener;
    private Instrumentation      mInstrumentation;
    private Activity             mActivity;
    private Context              mContext                     = null;

    private final static String  TAG                          = "LocalLib";
    private final static boolean DEBUG                        = false;

    public LocalLib(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);
        mInstrumentation = instrumentation;
        mActivity = activity;
        mContext = instrumentation.getContext();
    }

    private void print(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
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
    public Object invokeObjectMethod(Object owner, int classLevel, String methodName, Class[] parameterTypes,
            Object[] parameters) throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        return PrivateOperator.invokeObjectMethod(owner, classLevel, methodName, parameterTypes, parameters);
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
            throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        PrivateOperator.setObjectProperty(owner, classLevel, fieldName, value);
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
    public static Object getObjectProperty(Object owner, int classLevel, String fieldName) throws SecurityException,
            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        return PrivateOperator.getObjectProperty(owner, classLevel, fieldName);
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
    public static ArrayList<String> getPropertyNameByType(Object owner, int classLevel, String typeString) {
        return PrivateOperator.getPropertyNameByType(owner, classLevel, typeString);
    }

    /**
     * add listeners on all views for generating robotium code automatically
     */
    public void beginRecordRobotiumCode() {
        ArrayList<View> allViews = getViews();
        int viewNumber = allViews.size();
        print("viewNumber=" + viewNumber);
        for (int i = 0; i < viewNumber; i++) {
            setAutoGenerateCodeListenerOnView(allViews.get(i));
        }
    }

    /**
     * find parent until parent is android.view.View or java.lang.Object
     * 
     * @param view
     *            target view
     * @return positive means level from android.view.View; -1 means not found
     */
    private int countLevelFromView(View view) {
        int level = 0;
        Class originalClass = view.getClass();
        // find its parent
        while (true) {
            if (originalClass.equals(Object.class)) {
                return -1;
            } else if (originalClass.equals(View.class)) {
                return level;
            } else {
                level++;
                originalClass = originalClass.getSuperclass();
            }
        }
    }

    private void generateCodeForClick(View view) {
        String code = "//view.text=" + getTextFromView(view) + "\n" + "clickOnView(findViewById(new Integer("
                + view.getId() + ")));";
        print(code);
    }

    private Object getListener(View view, int level, String fieldName) {
        try {
            return PrivateOperator.getObjectProperty(view, level, fieldName);
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

    private void setAutoGenerateCodeListenerOnView(View view) {
        int level = countLevelFromView(view);
        print("level:" + level);
        if (-1 == level) {
            return;
        }

        mOnClickListener = (OnClickListener) getListener(view, level, "mOnClickListener");
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (null != mOnClickListener) {
                    mOnClickListener.onClick(v);
                }
                generateCodeForClick(v);
                print("id:" + v.getId() + "\t click");
            }
        });

        mOnLongClickListener = (OnLongClickListener) getListener(view, level, "mOnLongClickListener");
        view.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (null != mOnLongClickListener) {
                    mOnLongClickListener.onLongClick(v);
                }
                print("id:" + v.getId() + "\t long_click");
                return false;
            }
        });

        mOnTouchListener = (OnTouchListener) getListener(view, level, "mOnTouchListener");
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (null != mOnTouchListener) {
                    mOnTouchListener.onTouch(v, event);
                }
                print("id:" + v.getId() + "\t" + event.toString());
                return false;
            }
        });

        mOnKeyListener = (OnKeyListener) getListener(view, level, "mOnKeyListener");
        view.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (null != mOnKeyListener) {
                    mOnKeyListener.onKey(v, keyCode, event);
                }
                print("id:" + v.getId() + "\t" + event.toString() + "\t" + keyCode);
                return false;
            }
        });
    }

    private String getTextFromView(View view) {
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
    public ArrayList<View> findViewsByText(String text) {
        ArrayList<View> allViews = getViews();
        ArrayList<View> views = new ArrayList<View>();
        int viewNumber = allViews.size();

        for (int i = 0; i < viewNumber; i++) {
            View view = allViews.get(i);
            String t = getTextFromView(view);
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
        try {
            Class[] classes = Class.forName(packageName + ".R").getDeclaredClasses();
            // find string class's index of R
            int stringIndex = 0;
            for (int i = 0; i < classes.length; i++) {
                if (classes[i].getName().indexOf("$string") != -1) {
                    stringIndex = i;
                }
            }
            return (Integer) classes[stringIndex].getDeclaredField(stringName).get(classes[stringIndex].newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * get R.string.yourTargetString from test package
     * 
     * @param stringName
     *            name of your target string
     * @return string value
     */
    public String getTestRString(String stringName) {
        Context context = mInstrumentation.getContext();
        return context.getResources().getString(getRStringId(context.getPackageName(), stringName));
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
        return mInstrumentation.waitForMonitor(mInstrumentation.addMonitor(activityName, null, false));
    }

    /**
     * LaunchActivity for sync, the flag is
     * android.content.Intent.FLAG_ACTIVITY_NEW_TASK, the action is
     * Intent.ACTION_MAIN the category is Intent.CATEGORY_LAUNCHER
     * 
     * @param packageName
     * @param activityName
     * @return activity
     */
    public Activity launchAcitvity(String packageName, String activityName) {
        Intent mIntent = new Intent();
        mIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.setAction(Intent.ACTION_MAIN);
        mIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mIntent.setComponent(new ComponentName(packageName, packageName + "." + activityName));
        return mInstrumentation.startActivitySync(mIntent);
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
    public String runShellOnDevice(String[] command, String directory) {
        return ShellExecute.execute(command, directory);
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
    public boolean waitForViewVanishById(String resId, long timeout, boolean scroll, boolean onlyvisible) {
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
    public boolean waitForTextVanish(String text, int minimumNumberOfMatches, long timeout, boolean scroll) {
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

    private ArrayList<Checkable> getAllCheckableViews() {
        ArrayList<View> allViews = getViews();
        ArrayList<Checkable> checkable = new ArrayList<Checkable>();

        for (View v : allViews) {
            if (v instanceof Checkable) {
                checkable.add((Checkable) v);
            }
        }

        return checkable;
    }

    /**
     * set a CheckableView to the special status
     * 
     * @param index
     *            the index of CheckableView, from 0
     * @param checked
     *            the status of CheckableView
     * @return true means operation succeed; false means index does not exist
     */
    public boolean setCheckableViewState(int index, boolean checked) {
        ArrayList<Checkable> checkBoxs = getAllCheckableViews();

        if (index <= checkBoxs.size()) {
            final Checkable checkBox = checkBoxs.get(index);
            final boolean fChecked = checked;
            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    checkBox.setChecked(fChecked);
                }
            });
            return true;
        }

        return false;
    }

    /**
     * get status of special CheckableView
     * 
     * @param index
     *            the index of CheckableView, from 0
     * @return the status of CheckableView
     */
    public boolean getCheckableViewState(int index) {
        ArrayList<Checkable> checkBoxs = getAllCheckableViews();

        if (index > checkBoxs.size()) {
            print("index:" + index + "> switchers.size():" + checkBoxs.size());
            return false;
        }

        return checkBoxs.get(index).isChecked();
    }

    /**
     * setRequestedOrientation
     * 
     * @param orientation
     *            :
     *            local.setRequestedOrientation(CafeTestCase.SCREEN_ORIENTATION_PORTRAIT
     *            );
     *            local.setRequestedOrientation(CafeTestCase.SCREEN_ORIENTATION_LANDSCAPE
     *            );
     */
    public void setRequestedOrientation(int orientation) {
        if (orientation == CafeTestCase.SCREEN_ORIENTATION_LANDSCAPE) {
            this.mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (orientation == CafeTestCase.SCREEN_ORIENTATION_PORTRAIT) {
            this.mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
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
    public void sendMultiTouchMotionEvent(int pointerNumber, int[] start, int[] end, int step, int downDelay,
            int moveDelay, int upDelay) {

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
        mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime, MotionEvent.ACTION_DOWN, pointerNumber,
                pointerIds, pointerPositions, 0, 0.1f, 0.1f, 0, 0, 0, 0));
        this.sleep(downDelay);

        for (int i = 0; i < step; i++) {
            for (int j = 0; j < pointerNumber; j++) {
                temp = j * 2;
                pointerPositions[j].x = (float) (start[temp] + delta[temp] * (i + 1));

                temp++;
                pointerPositions[j].y = (float) (start[temp] + delta[temp] * (i + 1));
            }

            myTime = SystemClock.uptimeMillis();
            mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime, MotionEvent.ACTION_MOVE, pointerNumber,
                    pointerIds, pointerPositions, 0, 0.1f, 0.1f, 0, 0, 0, 0));

            this.sleep(moveDelay);
        }

        this.sleep(upDelay);
        myTime = SystemClock.uptimeMillis();
        mInstrumentation.sendPointerSync(MotionEvent.obtain(myTime, myTime, MotionEvent.ACTION_UP, pointerNumber,
                pointerIds, pointerPositions, 0, 0.1f, 0.1f, 0, 0, 0, 0));
    }

    /**
     * config mock server
     * 
     * @param filePath
     * @param caseId
     * @return
     * @throws IOException
     * @throws UnknownHostException
     */
    public String configMockServer(String filePath, String caseId) throws UnknownHostException, IOException {
        print("startup mock server");
        Intent myIntent = new Intent(MockConstant.MOCK_ACTIVITY_NAME);
        mActivity.startService(myIntent);
        try {
            print("wait 10 seconds");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            print(e.getMessage());
        }
        print("startup mock server end!");

        MockHelper helper = new MockHelper();
        return helper.config(filePath, caseId);
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
     * Returns a DatePicker located in the current activity
     * 
     * @return the DatePicker contained in the current activity
     */
    public DatePicker getCurrentDatePicker() {
        ArrayList<View> viewList = getViews();
        DatePicker datePicker = null;
        for (View view : viewList) {
            if (view instanceof android.widget.DatePicker) {
                datePicker = (DatePicker) view;
                break;
            }
        }
        return datePicker;
    }

    /**
     * Returns a TimePicker located in the current activity
     * 
     * @return the TimePicker contained in the current activity
     */
    public TimePicker getCurrentTimePicker() {
        ArrayList<View> viewList = getViews();
        TimePicker timePicker = null;
        for (View view : viewList) {
            if (view instanceof android.widget.TimePicker) {
                timePicker = (TimePicker) view;
                break;
            }
        }
        return timePicker;
    }

    /**
     * click on screen, the point is on the right
     */
    public void clickOnScreenRight() {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        clickOnScreen(x / 4, y / 2);
    }

    /**
     * click on screen, the point is on the left
     */
    public void clickOnScreenLeft() {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        clickOnScreen(x - x / 4, y / 2);
    }

    /**
     * click on screen, the point is on the up
     */
    public void clickOnScreenUp() {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        clickOnScreen(x / 2, y / 4);
    }

    /**
     * click on screen, the point is on the down
     */
    public void clickOnScreenDown() {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        clickOnScreen(x / 2, y - y / 4);
    }

    /**
     * drag on screen to right
     */
    public void dragScreenToRight(int stepCount) {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        drag(x - x / 4, x / 4, y / 2, y / 2, stepCount);
    }

    /**
     * drag on screen to Left
     */
    public void dragScreenToLeft(int stepCount) {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        drag(x / 4, x - x / 4, y / 2, y / 2, stepCount);
    }

    /**
     * drag on screen to up
     */
    public void dragScreenToUp(int stepCount) {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
        drag(x / 2, x / 2, y - y / 4, y / 4, stepCount);
    }

    /**
     * drag on screen to Down
     */
    public void dragScreenToDown(int stepCount) {
        DisplayMetrics dm;
        dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int x = dm.widthPixels;
        int y = dm.heightPixels;
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
     * @param onlyvisiable
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
                    && !(Boolean) invoke(mScroller, "scroll", new Class[] { int.class }, new Object[] { getField(
                            mScroller, "DOWN") })) { // mScroller.scroll(mScroller.DOWN)
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

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.performClick();
            }
        });
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
        int size = views.size();
        int number = 0;

        for (int i = 0; i < size; i++) {
            String strid = "";
            int resid = views.get(i).getId();

            // we only concern the shown view
            if (false == views.get(i).isShown()) {
                continue;
            }

            try {
                strid = views.get(i).getResources().getResourceName(resid);
                print(strid + "  views.get(i).getResources().getResourceName(resid) is " + strid);
            } catch (Resources.NotFoundException e) {
                print("resid num " + resid + " dose not have id");
                continue;
            }

            if (searchMode == LocalLib.SEARCHMODE_INCLUDE_MATCHING && strid.contains(resId)) {
                print("include mode;  strid is " + strid);
                number++;
            } else if (searchMode == LocalLib.SEARCHMODE_COMPLETE_MATCHING && strid.split(":")[1].trim().equals(resId)) {
                print("complete mode; strid is " + strid);
                number++;
            }

            if (number - 1 == index) {
                return views.get(i);
            }
        }

        return null;
    }

    private boolean isResIdShow(String resId, boolean isVisiable) {
        boolean flag = false;
        ArrayList<View> viewArray = getViews();
        int size = viewArray.size();
        for (int i = 0; i < size; i++) {
            if ((true == isVisiable)
                    && ((viewArray.get(i).getVisibility() == View.GONE) || (viewArray.get(i).getVisibility() == View.INVISIBLE))) {
                continue;
            }
            // we only concern the shown view
            if (false == viewArray.get(i).isShown()) {
                continue;
            }
            String strid;
            int resid = viewArray.get(i).getId();
            try {
                strid = viewArray.get(i).getResources().getResourceName(resid);
                print(strid + "  viewArray.get(i).getResources().getResourceName(resid) is " + strid);
            } catch (Resources.NotFoundException e) {
                print("resid num " + resid + " dose not have id");
                continue;
            }
            if (strid.contains(resId)) {
                flag = true;
                break;
            }
        }
        print("return value is " + flag);
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
        ArrayList<TextView> textViews = (ArrayList<TextView>) invoke(mViewFetcher, "getCurrentViews", new Class[] {
                Class.class, View.class }, new Object[] { TextView.class, parent }); // mViewFetcher.getCurrentViews(TextView.class,
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

    private boolean isLineShow(ListView listView, int line) {
        return (listView.getLastVisiblePosition() >= line && listView.getFirstVisiblePosition() <= line);
    }

    /**
     * Click in list
     * 
     * @param line
     *            the line that should be clicked
     * @param index
     *            the index of the list. E.g. Index 1 if two lists are available
     * @param scroll
     *            whether you need scroll the list automatically or not
     * @return a {@code List} of the {@code TextView}s located in the list line
     */
    @SuppressWarnings("unchecked")
    public ArrayList<TextView> clickInList(int line, int index, boolean scroll) {
        boolean foundList = (Boolean) invoke(mWaiter, "waitForView", new Class[] { Class.class, int.class },
                new Object[] { ListView.class, index });//waiter.waitForView(ListView.class, index);
        if (!foundList) {
            Assert.assertTrue("No ListView with index " + index + " is available!", false);
        }

        final ListView listView = (ListView) invoke(mViewFetcher, "getView", new Class[] { Class.class,
                ArrayList.class, int.class }, new Object[] { ListView.class, null, index });//viewFetcher.getView(ListView.class, null, index);
        if (listView == null) {
            Assert.assertTrue("ListView is null!", false);
        }

        if (isLineShow(listView, line - 1)) {
            int visibleLine = line - listView.getFirstVisiblePosition();
            print("visibleLine:" + visibleLine);
            return (ArrayList<TextView>) invoke(mClicker, "clickInList", new Class[] { int.class, Boolean.class,
                    int.class }, new Object[] { index, false, 0 }); // mClicker.clickInList(index,
            // false, 0);
        }

        if (scroll
                && (Boolean) invoke(mScroller, "scroll", new Class[] { int.class }, new Object[] { getField(mScroller,
                        "DOWN") })) {//scroller.scroll(Scroller.DOWN)
            return (ArrayList<TextView>) invoke(mClicker, "clickInList", new Class[] { int.class, int.class,
                    Boolean.class }, new Object[] { line, index, scroll }); // mClicker.clickInList(line,
            // index, scroll);
        }

        return null;
    }

    /**
     * Take an activity snapshot named 'fileName+timestamp', and place it in
     * this apk's folder.
     * 
     * @param fileName
     *            file name
     */
    public void takeActivitySnapshot(String fileName) {
        Time localTime = new Time("Asia/Hong_Kong");
        localTime.setToNow();
        fileName += localTime.format("%Y-%m-%d-%H-%M-%S");

        String savePath = "/data/data/" + getCurrentActivity().getPackageName() + "/" + fileName + ".jpg";
        print("save snapshot, file is in " + savePath);

        View view = (View) invoke(mViewFetcher, "getActiveDecorView"); // mViewFetcher.getActiveDecorView();
        SnapshotHelper.takeViewSnapshot(view, savePath);
    }

    /**
     * Take an activity snapshot
     * 
     * @param path
     *            path
     * @param fileName
     *            file name
     */
    public void takeActivitySnapshot(String path, String fileName) {
        String savePath = path + fileName + ".jpg";
        print("Save snapshot, file is in " + savePath);
        View view = (View) invoke(mViewFetcher, "getActiveDecorView"); // mViewFetcher.getActiveDecorView();
        SnapshotHelper.takeViewSnapshot(view, savePath);
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
            String path = mContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA).sourceDir;
            DexFile dexfile = new DexFile(path);
            Enumeration<String> entries = dexfile.entries();
            while (entries.hasMoreElements()) {
                String name = (String) entries.nextElement();
                classes.add(name);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

}
