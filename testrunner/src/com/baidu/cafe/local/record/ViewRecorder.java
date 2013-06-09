/*
 * Copyright (C) 2012 Baidu.com Inc
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

package com.baidu.cafe.local.record;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.local.DESEncryption;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.Log;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-8
 * @version
 * @todo
 */
public class ViewRecorder {
    public final static boolean                      DEBUG                     = false;

    private final static int                         MAX_SLEEP_TIME            = 20000;
    private final static int                         MIN_SLEEP_TIME            = 1000;
    private final static int                         MIN_STEP_COUNT            = 4;
    private final static boolean                     DEBUG_WEBVIEW             = true;
    private final static String                      REPLAY_CLASS_NAME         = "CafeReplay";
    private final static String                      REPLAY_FILE_NAME          = REPLAY_CLASS_NAME
                                                                                       + ".java";
    /**
     * For judging whether a view is an old one.
     * 
     * Key is string of view id.
     * 
     * Value is position array of view.
     */
    private HashMap<String, int[]>                   mAllViewPosition          = new HashMap<String, int[]>();

    /**
     * For judging whether a view has been hooked.
     */
    private ArrayList<Integer>                       mAllListenerHashcodes     = new ArrayList<Integer>();

    /**
     * For judging whether a EditText has been hooked.
     */
    private ArrayList<EditText>                      mAllEditTexts             = new ArrayList<EditText>();

    /**
     * For merge a sequeue of MotionEvents to a drag.
     */
    private Queue<RecordMotionEvent>                 mMotionEventQueue         = new LinkedList<RecordMotionEvent>();

    /**
     * For judging events of the same view at the same time which should be
     * keeped by their priorities.
     */
    private Queue<OutputEvent>                       mOutputEventQueue         = new LinkedList<OutputEvent>();

    /**
     * For mapping keycode to keyname
     */
    private HashMap<Integer, String>                 mKeyCodeMap               = new HashMap<Integer, String>();

    /**
     * For judging whether UI is static.
     */
    private ArrayList<View>                          mLastViews                = new ArrayList<View>();

    /**
     * lock for OutputEventQueue
     * 
     * NOTICE: new String("") can not replaced by "", because the code
     * synchronizes on interned String. Constant Strings are interned and shared
     * across all other classes loaded by the JVM. Thus, this could is locking
     * on something that other code might also be locking. This could result in
     * very strange and hard to diagnose blocking and deadlock behavior.
     */
    private static String                            mSyncOutputEventQueue     = new String(
                                                                                       "mSyncOutputEventQueue");

    /**
     * lock for MotionEventQueue
     */
    private static String                            mSyncMotionEventQueue     = new String(
                                                                                       "mSyncMotionEventQueue");
    /**
     * Time when event was being generated.
     */
    private long                                     mTheCurrentEventOutputime = System.currentTimeMillis();

    /**
     * event count for naming screenshot
     */
    private int                                      mEventCount               = 0;

    /**
     * interval between events
     */
    private long                                     mLastEventTime            = System.currentTimeMillis();

    /**
     * assume that only one ScrollView is fling
     */
    private String                                   mFamilyStringBeforeScroll = "";

    /**
     * to ignore drag event
     */
    private boolean                                  mIsLongClick              = false;

    private boolean                                  mDragWithoutUp            = false;

    /**
     * to ignore drag event when "output a drag without up"
     */
    private boolean                                  mIsAbsListViewToTheEnd    = false;

    /**
     * Saving states for each listview
     */
    private HashMap<String, AbsListViewState>        mAbsListViewStates        = new HashMap<String, AbsListViewState>();

    /**
     * save edittext the lastest text
     */
    private HashMap<String, String>                  mEditTextLastText         = new HashMap<String, String>();

    /**
     * Saving old listener for invoking when needed
     */
    private HashMap<String, OnClickListener>         mOnClickListeners         = new HashMap<String, OnClickListener>();
    private HashMap<String, OnLongClickListener>     mOnLongClickListeners     = new HashMap<String, OnLongClickListener>();
    private HashMap<String, OnTouchListener>         mOnTouchListeners         = new HashMap<String, OnTouchListener>();
    private HashMap<String, OnKeyListener>           mOnKeyListeners           = new HashMap<String, OnKeyListener>();
    private HashMap<String, OnItemClickListener>     mOnItemClickListeners     = new HashMap<String, OnItemClickListener>();
    private HashMap<String, OnGroupClickListener>    mOnGroupClickListeners    = new HashMap<String, OnGroupClickListener>();
    private HashMap<String, OnChildClickListener>    mOnChildClickListeners    = new HashMap<String, OnChildClickListener>();
    private HashMap<String, OnScrollListener>        mOnScrollListeners        = new HashMap<String, OnScrollListener>();
    private HashMap<String, OnItemLongClickListener> mOnItemLongClickListeners = new HashMap<String, OnItemLongClickListener>();
    private HashMap<String, OnItemSelectedListener>  mOnItemSelectedListeners  = new HashMap<String, OnItemSelectedListener>();
    private LocalLib                                 local                     = null;
    private File                                     mRecord                   = null;
    private String                                   mPackageName              = null;
    private String                                   mPath                     = null;
    private int                                      mCurrentEditTextIndex     = 0;
    private String                                   mCurrentEditTextString    = "";
    private boolean                                  mHasTextChange            = false;
    private long                                     mTheLastTextChangedTime   = System.currentTimeMillis();
    private int                                      mCurrentScrollState       = 0;

    public ViewRecorder(LocalLib local) {
        this.local = local;
        init();
    }

    class RecordMotionEvent {
        public View  view;
        public float x;
        public float y;
        public int   action;
        public long  time;

        public RecordMotionEvent(View view, int action, float x, float y, long time) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.action = action;
            this.time = time;
        }

        @Override
        public String toString() {
            return String
                    .format("RecordMotionEvent(%s, action=%s, x=%s, y=%s)", view, action, x, y);
        }
    }

    class AbsListViewState {
        public int firstVisibleItem     = 0;
        public int visibleItemCount     = 0;
        public int totalItemCount       = 0;
        public int lastFirstVisibleItem = 0;
    }

    class ClickEvent extends OutputEvent {
        public ClickEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_CLICK;
        }
    }

    class DragEvent extends OutputEvent {
        public DragEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_DRAG;
        }
    }

    class HardKeyEvent extends OutputEvent {
        public HardKeyEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_KEY;
        }
    }

    class ScrollEvent extends OutputEvent {
        public ScrollEvent(View view) {
            this.view = view;
            this.priority = PRIORITY_SCROLL;
        }
    }

    /**
     * sort by view.hashCode()
     */
    class SortByView implements Comparator<OutputEvent> {
        @Override
        public int compare(OutputEvent e1, OutputEvent e2) {
            if (null == e1 || null == e1.view) {
                return -1;
            }
            if (null == e2 || null == e2.view) {
                return 1;
            }
            if (e1.view.hashCode() > e2.view.hashCode()) {
                return 1;
            }
            return -1;
        }
    }

    /**
     * sort by proity
     */
    class SortByPriority implements Comparator<OutputEvent> {
        @Override
        public int compare(OutputEvent e1, OutputEvent e2) {
            if (null == e1 || null == e1.view) {
                return -1;
            }
            if (null == e2 || null == e2.view) {
                return 1;
            }
            if (e1.priority > e2.priority) {
                return 1;
            }
            return -1;
        }
    }

    /**
     * sort by view.familyString.length()
     */
    class SortByFamilyString implements Comparator<OutputEvent> {
        @Override
        public int compare(OutputEvent e1, OutputEvent e2) {
            if (null == e1 || null == e1.view) {
                return -1;
            }
            if (null == e2 || null == e2.view) {
                return 1;
            }
            // longer means younger
            if (local.getFamilyString(e1.view).length() > local.getFamilyString(e2.view).length()) {
                return 1;
            }
            return -1;
        }
    }

    private void print(String tag, String message) {
        if (DEBUG) {
            Log.i(tag, message);
        } else {
            Log.i(tag, DESEncryption.encryptStr(message));
        }
    }

    private void printLog(String message) {
        print("ViewRecorder", message);
    }

    private void printLayout(View view) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        // local.getRIdNameByValue(packageName, value)
        String msg = String.format("[][%s][%s][%s][%s,%s,%s,%s]", local.getFamilyString(view),
                view, local.getViewText(view), xy[0], xy[1], view.getWidth(), view.getHeight());
        print("ViewLayout", msg);
    }

    private void printCode(String message) {
        print("RecorderCode", message);

        String[] lines = message.split("\n");
        String importLine = "";
        String codeLine = "";
        for (String line : lines) {
            if (line.startsWith("import ")) {
                importLine = line;
            } else {
                codeLine += line + "\n";
            }
        }
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        ArrayList<String> linesBeforNextImport = new ArrayList<String>();
        try {
            String line = null;
            reader = new BufferedReader(new FileReader(mRecord));
            while ((line = reader.readLine()) != null) {
                linesBeforNextImport.add(line);
                // add import line
                if (!importLine.equals("") && line.contains("next import")
                        && !linesBeforNextImport.contains(importLine)) {
                    // sb.append(importLine + "\n");
                    // sb.append("// next import" + "\n");
                } else if (line.contains("next line")) {// add code line
                    sb.append(formatCode(codeLine));
                    sb.append(formatCode("// next line"));
                } else {
                    sb.append(line + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                writeToFile(sb.toString());
            } else {
                printLog(String.format("read [%s] failed", mRecord.getPath()));
            }
        }
    }

    private void writeToFile(String line) {
        if (null == line) {
            return;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(mRecord));
            writer.write(line);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * For indent code line
     */
    private String formatCode(String code) {
        String[] lines = code.split("\n");
        String formatString = "";
        String prefix = "        ";
        for (String line : lines) {
            formatString += prefix + line + "\n";
        }
        return formatString;
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        DESEncryption.setKey("com.baidu.cafe.local");
        mPackageName = local.getCurrentActivity().getPackageName();
        initKeyTable();

        // init cafe dir
        mPath = "/data/data/" + mPackageName + "/cafe";
        File cafe = new File(mPath);
        if (!cafe.exists()) {
            cafe.mkdir();
            LocalLib.executeOnDevice("chmod 777 " + mPath, "/", 200);
        }

        // init template
        mRecord = new File(mPath + "/" + REPLAY_FILE_NAME);
        if (mRecord.exists()) {
            mRecord.delete();
        }
        String code = String.format(template, CafeTestCase.mActivityClass.getName(), mPackageName);
        writeToFile(code);
        LocalLib.executeOnDevice("chmod 777 " + mPath + "/" + REPLAY_FILE_NAME, "/", 200);
    }

    final String template = "package com.example.demo.test;\n" + "\n"
                                  + "import android.view.KeyEvent;\n"
                                  + "import com.baidu.cafe.CafeTestCase;\n" + "// next import\n"
                                  + "\n" + "public class "
                                  + REPLAY_CLASS_NAME
                                  + " extends CafeTestCase {\n"
                                  + "    private static Class<?>     launcherActivityClass;\n"
                                  + "    static {\n"
                                  + "        try {\n"
                                  + "            launcherActivityClass = Class.forName(\"%s\");\n"
                                  + "        } catch (ClassNotFoundException e) {\n"
                                  + "        }\n"
                                  + "    }\n"
                                  + "\n"
                                  + "    public "
                                  + REPLAY_CLASS_NAME
                                  + "() {\n"
                                  + "        super(\"%s\", launcherActivityClass);\n"
                                  + "    }\n"
                                  + "\n"
                                  + "    @Override\n"
                                  + "    protected void setUp() throws Exception{\n"
                                  + "        super.setUp();\n"
                                  + "    }\n"
                                  + "\n"
                                  + "    @Override\n"
                                  + "    protected void tearDown() throws Exception{\n"
                                  + "        super.tearDown();\n"
                                  + "    }\n"
                                  + "\n"
                                  + "    public void testRecorded() {\n"
                                  + "        // next line\n"
                                  + "        local.sleep(3000);\n" + "    }\n" + "\n" + "}\n";

    /**
     * Add listeners on all views for generating cafe code automatically
     */
    public void beginRecordCode() {
        monitorCurrentActivity();

        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    sleep(50);
                    ArrayList<View> newViews = getTargetViews();
                    if (newViews.size() == 0) {
                        continue;
                    }
                    setDefaultFocusView();
                    for (View view : newViews) {
                        try {
                            setHookListenerOnView(view);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, "keep hooking new views").start();

        handleRecordMotionEventQueue();
        handleOutputEventQueue();

        mLastEventTime = System.currentTimeMillis();
        local.sleep(2000);
        printLog("ViewRecorder is ready to work.");
    }

    private void monitorCurrentActivity() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    updateCurrentActivity();
                    sleep(1000);
                }
            }
        }, "monitorCurrentActivity").start();
    }

    /**
     * @return new activity class
     */
    private void updateCurrentActivity() {
        Activity activity = local.getCurrentActivity();
        setOnTouchListenerOnDecorView(activity);
    }

    /**
     * If there is no views to handle onTouch event, decorView will handle it
     * and invoke activity.onTouchEvent(event).If decorView does not handle a
     * touch event by return true, events follow-up will not be dispatched to
     * views including decorView.
     */
    private void setOnTouchListenerOnDecorView(final Activity activity) {
        View[] views = LocalLib.getWindowDecorViews();
        for (View view : views) {
            handleOnTouchListener(view);
            /*
            OnTouchListener onTouchListener = (OnTouchListener) getListener(view,
                    "mOnTouchListener");
            if (null == onTouchListener) {
                view.setOnTouchListener(new OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        printLog("onTouch:" + event);
                        addEvent(v, event);
                        // return activity.onTouchEvent(event);
                        return false;
                    }
                });
            }
            */
        }
        // View decorView = activity.getWindow().getDecorView();
    }

    private ArrayList<View> getTargetViews() {
        ArrayList<View> views = local
                .removeInvisibleViews(local.getCurrentViews()/*onlySufficientlyVisible == true*/);
        // ArrayList<View> views = local.getViews();
        ArrayList<View> targetViews = new ArrayList<View>();

        for (View view : views) {
            // for thread safe
            if (null == view) {
                continue;
            }
            boolean isOld = mAllViewPosition.containsKey(getViewID(view));
            // refresh view layout
            if (hasChange(view)) {
                saveView(view);
            }

            if (!isOld) {
                // save new view
                saveView(view);
                targetViews.add(view);
                handleOnKeyListener(view);
            } else {
                // get view who have unhooked listeners
                if (hasUnhookedListener(view)) {
                    targetViews.add(view);
                }
            }
        }

        flushWhenStatic(local.removeInvisibleViews(views));

        return targetViews;
    }

    private void flushWhenStatic(final ArrayList<View> views) {
        if (mLastViews.size() == views.size() && !hasChangedView(views)) {
            return;
        }

        // It's too slow to be at main thread.
        new Thread(new Runnable() {

            @Override
            public void run() {
                flushViewLayout(views);
            }
        }, "flushViewLayout").start();
        mLastViews.clear();
        mLastViews.addAll(views);
    }

    private boolean hasChangedView(ArrayList<View> views) {
        for (View view : views) {
            if (hasChange(view)) {
                return true;
            }
        }
        return false;
    }

    /**
     * For mtc client
     */
    private void flushViewLayout(ArrayList<View> views) {
        print("ViewLayout", String.format("[]ViewLayout refreshed."));
        for (View view : views) {
            printLayout(view);
        }
    }

    private void saveView(View view) {
        if (null == view) {
            printLog("null == view " + Log.getThreadInfo());
            return;
        }
        String viewID = getViewID(view);
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        mAllViewPosition.put(viewID, xy);
    }

    private boolean hasChange(View view) {
        // new view
        String viewID = getViewID(view);
        int[] oldXy = mAllViewPosition.get(viewID);
        if (null == oldXy) {
            return true;
        }

        // location change
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        return xy[0] != oldXy[0] || xy[1] != oldXy[1] ? true : false;
    }

    private View getCurrentFocusView() {
        ArrayList<View> views = local.getViews();/*onlySufficientlyVisible == false*/
        return local.getFocusView(views);
    }

    private void setDefaultFocusView() {
        // It's too slow..
        // if (local.getCurrentActivity().getCurrentFocus() != null) {
        //     return;
        // }
        if (getCurrentFocusView() != null) {
            return;
        }
        View view = local.getRecentDecorView();
        if (null == view) {
            printLog("null == view of setDefaultFocusView");
            return;
        }
        boolean hasFocus = local.requestFocus(view);
        //        printLog(view + " hasFocus: " + hasFocus);
        String viewID = getViewID(view);
        if (!mAllViewPosition.containsKey(viewID)) {
            saveView(view);
            handleOnKeyListener(view);
        }
    }

    private boolean hasUnhookedListener(View view) {
        String[] listenerNames = new String[] { "mOnItemClickListener", "mOnClickListener",
                "mOnTouchListener", "mOnKeyListener", "mOnScrollListener" };
        for (String listenerName : listenerNames) {
            Object listener = getListener(view, listenerName);
            if (listener != null && !mAllListenerHashcodes.contains(listener.hashCode())) {
                // print("has unhooked " + listenerName + ": " + view);
                return true;
            }
        }
        return false;
    }

    private Class<?> getClassByListenerName(String listenerName) {
        Class<?> viewClass = null;
        if ("mOnItemClickListener".equals(listenerName)
                || "mOnItemLongClickListener".equals(listenerName)) {
            viewClass = AdapterView.class;
        } else if ("mOnScrollListener".equals(listenerName)) {
            viewClass = AbsListView.class;
        } else if ("mOnChildClickListener".equals(listenerName)
                || "mOnGroupClickListener".equals(listenerName)) {
            viewClass = ExpandableListView.class;
        } else {
            viewClass = View.class;
        }
        return viewClass;
    }

    private Object getListener(View view, String listenerName) {
        return local.getListener(view, getClassByListenerName(listenerName), listenerName);
    }

    public LocalLib getLocalLib() {
        return local;
    }

    private void setListener(View view, String listenerName, Object value) {
        local.setListener(view, getClassByListenerName(listenerName), listenerName, value);
    }

    /**
     * These try-catch can not be merged. We need try to hook listeners as many
     * as possible.
     */
    private void setHookListenerOnView(View view) {
        // for thread safe
        if (null == view) {
            return;
        }

        if (view instanceof WebView && DEBUG_WEBVIEW) {
            new WebElementRecorder(this).handleWebView((WebView) view);
        }

        // handle list
        if (view instanceof AdapterView) {
            if (view instanceof ExpandableListView) {
                handleExpandableListView((ExpandableListView) view);
            } else if (!(view instanceof Spinner)) {
                handleOnItemClickListener((AdapterView<?>) view);
            }
            if (view instanceof AbsListView) {
                handleOnScrollListener((AbsListView) view);
            }
            //            view.isLongClickable()
            handleOnItemLongClickListener((AdapterView<?>) view);
            // adapterView.setOnItemSelectedListener(listener);
            // MenuItem.OnMenuItemClickListener
        }

        if (view.isLongClickable()) {
            handleOnLongClickListener(view);
        }

        if (view instanceof EditText) {
            hookEditText((EditText) view);
        } else {
            // handleOnClickListener can not replace handleOnTouchListener because reason below.
            // There are some views which have click listener and touch listener but only use touch listener.
            handleOnClickListener(view);
        }

        handleOnTouchListener(view);
    }

    private void handleOnScrollListener(AbsListView absListView) {
        OnScrollListener onScrollListener = (OnScrollListener) getListener(absListView,
                "mOnScrollListener");
        // has hooked listener
        if (onScrollListener != null && mAllListenerHashcodes.contains(onScrollListener.hashCode())) {
            return;
        }

        mAbsListViewStates.put(getViewID(absListView), new AbsListViewState());
        if (null != onScrollListener) {
            hookOnScrollListener(absListView, onScrollListener);
        } else {
            printLog("set onScrollListener [" + absListView + "]");
            OnScrollListener onScrollListenerHooked = new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    setOnScrollStateChanged(view, scrollState);
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                        int totalItemCount) {
                    setOnScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            };
            setListener(absListView, "mOnScrollListener", onScrollListenerHooked);
        }

        // save hashcode of hooked listener
        OnScrollListener onScrollListenerHooked = (OnScrollListener) getListener(absListView,
                "mOnScrollListener");

        if (onScrollListenerHooked != null) {
            mAllListenerHashcodes.add(onScrollListenerHooked.hashCode());
        }
    }

    private void setOnScrollStateChanged(AbsListView view, int scrollState) {
        AbsListViewState absListViewState = mAbsListViewStates.get(getViewID(view));
        if (null == absListViewState) {
            printLog("null == absListViewState !!!");
            return;
        }
        mCurrentScrollState = scrollState;

        if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
            printLog("getLastVisiblePosition:" + view.getLastVisiblePosition());
            printLog("totalItemCount:" + absListViewState.totalItemCount);
            if (view.getLastVisiblePosition() + 1 == absListViewState.totalItemCount) {
                mIsAbsListViewToTheEnd = true;
            }
            outputAScroll(view);
        }
        if (OnScrollListener.SCROLL_STATE_TOUCH_SCROLL == scrollState) {
            absListViewState.lastFirstVisibleItem = view.getFirstVisiblePosition();
        }
    }

    private void setOnScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        AbsListViewState absListViewState = mAbsListViewStates.get(getViewID(view));
        if (null == absListViewState) {
            printLog("null == absListViewState !!!");
            return;
        }
        absListViewState.firstVisibleItem = firstVisibleItem;
        absListViewState.visibleItemCount = visibleItemCount;
        absListViewState.totalItemCount = totalItemCount;

        if (firstVisibleItem + visibleItemCount == totalItemCount && firstVisibleItem != 0) {
            //printLog("firstVisibleItem:" + firstVisibleItem);
            //printLog("visibleItemCount:" + visibleItemCount);
            //printLog("totalItemCount:" + totalItemCount);
            outputAScroll(view);
        }
    }

    private void outputAScroll(AbsListView view) {
        AbsListViewState absListViewState = mAbsListViewStates.get(getViewID(view));
        if (null == absListViewState || absListViewState.totalItemCount == 0
                || absListViewState.visibleItemCount == 0
                || absListViewState.lastFirstVisibleItem == absListViewState.firstVisibleItem) {
            return;
        }
        printLog("mLastFirstVisibleItem:" + absListViewState.lastFirstVisibleItem);
        printLog("mFirstVisibleItem:" + absListViewState.firstVisibleItem);
        printLog("getFirstVisiblePosition:" + view.getFirstVisiblePosition());
        absListViewState.lastFirstVisibleItem = absListViewState.firstVisibleItem;
        ScrollEvent scrollEvent = new ScrollEvent(view);
        String familyString = local.getFamilyString(view);
        String scroll = String.format("local.scrollListToLineWithFamilyString(%s, \"%s\");",
                absListViewState.firstVisibleItem, familyString);
        scrollEvent.setCode(scroll);
        scrollEvent.setLog("scroll " + view + " to " + absListViewState.firstVisibleItem);
        offerOutputEventQueue(scrollEvent);
    }

    private void hookOnScrollListener(final AbsListView absListView,
            final OnScrollListener onScrollListener) {
        printLog("hook onScrollListener [" + absListView + "]");

        // save old listener
        mOnScrollListeners.put(getViewID(absListView), onScrollListener);
        //        mOnScrollListeners.put(String.valueOf(absListView.hashCode()), onScrollListener);

        // set hook listener
        final OnScrollListener onScrollListenernew = new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                setOnScrollStateChanged(view, scrollState);
                OnScrollListener onScrollListener = mOnScrollListeners.get(getViewID(view));
                OnScrollListener onScrollListenerHooked = (OnScrollListener) getListener(view,
                        "mOnScrollListener");
                if (onScrollListener != null) {
                    // TODO It's a bug. It can not be fix by below.
                    if (onScrollListener.equals(onScrollListenerHooked)) {
                        printLog("onScrollListenerHooked == onScrollListener!!!");
                        return;
                    }
                    onScrollListener.onScrollStateChanged(view, scrollState);
                } else {
                    printLog("onScrollListener == null " + Log.getThreadInfo());
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                    int totalItemCount) {
                setOnScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                OnScrollListener onScrollListener = mOnScrollListeners.get(String.valueOf(view
                        .hashCode()));
                OnScrollListener onScrollListenerHooked = (OnScrollListener) getListener(view,
                        "mOnScrollListener");
                if (onScrollListener != null) {
                    // TODO It's a bug. It can not be fix by below.
                    if (onScrollListener.equals(onScrollListenerHooked)) {
                        printLog("onScrollListenerHooked == onScrollListener!!!");
                        return;
                    }
                    onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
                            totalItemCount);
                } else {
                    printLog("onScrollListener == null " + Log.getThreadInfo());
                }
            }
        };

        local.runOnMainSync(new Runnable() {

            @Override
            public void run() {
                absListView.setOnScrollListener(onScrollListenernew);
            }
        });
    }

    private void handleExpandableListView(ExpandableListView expandableListView) {
        handleOnGroupClickListener(expandableListView);
        handleOnChildClickListener(expandableListView);
    }

    private void handleOnGroupClickListener(final ExpandableListView expandableListView) {
        OnGroupClickListener onGroupClickListener = (OnGroupClickListener) getListener(
                expandableListView, "mOnGroupClickListener");

        // has hooked listener
        if (onGroupClickListener != null
                && mAllListenerHashcodes.contains(onGroupClickListener.hashCode())) {
            return;
        }

        if (null != onGroupClickListener) {
            hookOnGroupClickListener(expandableListView, onGroupClickListener);
        } else {
            printLog("set onGroupClickListener [" + expandableListView + "]");
            OnGroupClickListener onGroupClickListenerHooked = new OnGroupClickListener() {

                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                        long id) {
                    setOnGroupClick(parent, groupPosition);
                    return false;
                }
            };
            setListener(expandableListView, "mOnGroupClickListener", onGroupClickListenerHooked);
        }

        // save hashcode of hooked listener
        OnGroupClickListener onGroupClickListenerHooked = (OnGroupClickListener) getListener(
                expandableListView, "mOnGroupClickListener");
        if (onGroupClickListenerHooked != null) {
            mAllListenerHashcodes.add(onGroupClickListenerHooked.hashCode());
        }
    }

    private void setOnGroupClick(ExpandableListView parent, int groupPosition) {
        int flatListPosition = parent.getFlatListPosition(ExpandableListView
                .getPackedPositionForGroup(groupPosition));
        String familyString = local.getFamilyString(parent);
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format("local.clickOnExpandableListView(\"%s\", %s);", familyString,
                flatListPosition);
        clickEvent.setCode(code);
        clickEvent.setLog(String.format("click on group[%s]", groupPosition));

        offerOutputEventQueue(clickEvent);
    }

    private void hookOnGroupClickListener(final ExpandableListView expandableListView,
            OnGroupClickListener onGroupClickListener) {
        printLog("hook onGroupCollapseListener [" + expandableListView + "]");

        // save old listener
        mOnGroupClickListeners.put(getViewID(expandableListView), onGroupClickListener);

        // set hook listener
        expandableListView.setOnGroupClickListener(new OnGroupClickListener() {

            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                    long id) {
                setOnGroupClick(parent, groupPosition);
                OnGroupClickListener onGroupClickListener = mOnGroupClickListeners
                        .get(getViewID(expandableListView));
                if (onGroupClickListener != null) {
                    onGroupClickListener.onGroupClick(parent, v, groupPosition, id);
                } else {
                    printLog("onGroupClickListener == null");
                }
                return false;
            }
        });
    }

    private void handleOnChildClickListener(final ExpandableListView expandableListView) {
        OnChildClickListener onChildClickListener = (OnChildClickListener) getListener(
                expandableListView, "mOnChildClickListener");

        // has hooked listener
        if (onChildClickListener != null
                && mAllListenerHashcodes.contains(onChildClickListener.hashCode())) {
            return;
        }

        if (null != onChildClickListener) {
            hookOnChildClickListener(expandableListView, onChildClickListener);
        } else {
            printLog("set onChildClickListener [" + expandableListView + "]");
            OnChildClickListener onChildClickListenerHooked = new OnChildClickListener() {

                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                        int childPosition, long id) {
                    setOnChildClick(expandableListView, groupPosition, childPosition);
                    return false;
                }
            };
            setListener(expandableListView, "mOnChildClickListener", onChildClickListenerHooked);
        }

        // save hashcode of hooked listener
        OnChildClickListener onChildClickListenerHooked = (OnChildClickListener) getListener(
                expandableListView, "mOnChildClickListener");
        if (onChildClickListenerHooked != null) {
            mAllListenerHashcodes.add(onChildClickListenerHooked.hashCode());
        }
    }

    private void setOnChildClick(ExpandableListView parent, int groupPosition, int childPosition) {
        int flatListPosition = parent.getFlatListPosition(ExpandableListView
                .getPackedPositionForChild(groupPosition, childPosition));
        String familyString = local.getFamilyString(parent);
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format("local.clickOnExpandableListView(\"%s\", %s);", familyString,
                flatListPosition);
        clickEvent.setCode(code);
        clickEvent.setLog(String.format("click on group[%s] child[%s]", groupPosition,
                childPosition));

        offerOutputEventQueue(clickEvent);
    }

    private void hookOnChildClickListener(final ExpandableListView expandableListView,
            OnChildClickListener onChildClickListener) {
        printLog("hook onChildClickListener [" + expandableListView + "]");

        // save old listener
        mOnChildClickListeners.put(getViewID(expandableListView), onChildClickListener);

        // set hook listener
        expandableListView.setOnChildClickListener(new OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                    int childPosition, long id) {
                setOnChildClick(expandableListView, groupPosition, childPosition);
                OnChildClickListener onChildClickListener = mOnChildClickListeners
                        .get(getViewID(expandableListView));
                if (onChildClickListener != null) {
                    onChildClickListener.onChildClick(parent, v, groupPosition, childPosition, id);
                } else {
                    printLog("onChildClickListener == null");
                }
                return false;
            }
        });
    }

    private boolean handleOnClickListener(View view) {
        OnClickListener onClickListener = (OnClickListener) getListener(view, "mOnClickListener");

        // has hooked listener
        if (onClickListener != null && mAllListenerHashcodes.contains(onClickListener.hashCode())) {
            return true;
        }

        if (onClickListener != null) {
            try {
                hookOnClickListener(view, onClickListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else {
            // only care of views which has OnClickListener
        }

        return false;
    }

    private void hookOnClickListener(final View view, OnClickListener onClickListener) {
        // printLog(String.format("hookClickListener [%s(%s)]", view, local.getViewText(view)));

        // save old listener
        mOnClickListeners.put(getViewID(view), onClickListener);

        local.runOnMainSync(new Runnable() {

            @Override
            public void run() {
                // set hook listener
                OnClickListener onClickListenerHooked = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setOnClick(v);
                    }
                };
                setListener(view, "mOnClickListener", onClickListenerHooked);
            }
        });

        // save hashcode of hooked listener
        OnClickListener onClickListenerHooked = (OnClickListener) getListener(view,
                "mOnClickListener");
        if (onClickListenerHooked != null) {
            mAllListenerHashcodes.add(onClickListenerHooked.hashCode());
        }
    }

    private void setOnClick(View v) {
        if (local.isSize0(v)) {
            printLog(v + " is size 0 " + Log.getThreadInfo());
            invokeOriginOnClickListener(v);
            return;
        }

        // set click event output
        ClickEvent clickEvent = new ClickEvent(v);
        String viewClass = getViewString(v);
        String familyString = local.getFamilyString(v);
        String r = getRString(v);
        String rString = r.equals("") ? "" : "[" + r + "]";
        String comments = String.format("[%s]%s[%s] ", v, rString, local.getViewText(v));
        String click = "";
        if ("".equals(rString)) {
            click = String.format("local.clickOn(\"%s\", \"%s\", false);//%s%s", viewClass,
                    familyString, "Click On ", getFirstLine(comments));
        } else {
            String rStringSuffix = getRStringSuffix(v);
            int index = local.getResIdIndex(v);
            click = String.format("local.clickOn(\"id/%s\", \"%s\", false);//%s%s", rStringSuffix,
                    index, "Click On ", getFirstLine(comments));
        }
        clickEvent.setCode(click);

        // clickEvent.setLog();
        offerOutputEventQueue(clickEvent);
        invokeOriginOnClickListener(v);
    }

    private void invokeOriginOnClickListener(View v) {
        OnClickListener onClickListener = mOnClickListeners.get(getViewID(v));
        OnClickListener onClickListenerHooked = (OnClickListener) getListener(v, "mOnClickListener");
        if (onClickListener != null) {
            // TODO It's a bug. It can not be fixed by below.
            if (onClickListener.equals(onClickListenerHooked)) {
                printLog("onClickListener == onClickListenerHooked!!!");
                return;
            }
            onClickListener.onClick(v);
        } else {
            printLog("onClickListener == null");
        }
    }

    private String getFirstLine(String str) {
        String[] lines = str.split("\r\n");
        if (lines.length > 1) {
            return lines[0];
        }
        lines = str.split("\n");
        if (lines.length > 1) {
            return lines[0];
        }
        return str;
    }

    private String getRString(View view) {
        String rStringSuffix = getRStringSuffix(view);
        return "".equals(rStringSuffix) ? "" : "R.id." + rStringSuffix;
    }

    private String getRStringSuffix(View view) {
        int id = view.getId();
        if (-1 == id) {
            return "";
        }

        try {
            String rString = local.getCurrentActivity().getResources()
                    .getResourceName(view.getId());
            return rString.substring(rString.lastIndexOf("/") + 1, rString.length());
        } catch (Exception e) {
            // eat it because some view has no res id
        }
        return "";
    }

    private long getSleepTime() {
        long ret = System.currentTimeMillis() - mLastEventTime;

        new Thread(new Runnable() {

            @Override
            public void run() {
                local.sleep(300);
                mLastEventTime = System.currentTimeMillis();
            }
        }, "update mLastEventTime lately").start();

        ret = ret < MIN_SLEEP_TIME ? MIN_SLEEP_TIME : ret;
        ret = ret > MAX_SLEEP_TIME ? MAX_SLEEP_TIME : ret;
        return ret;
    }

    private void hookEditText(final EditText editText) {
        if (mAllEditTexts.contains(editText)) {
            return;
        }

        // save origin text
        mEditTextLastText.put(getViewID(editText), editText.getText().toString());

        // all TextWatchers work at the same time
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\r\n", "\\n").replace("\n", "\\n");
                String lastText = mEditTextLastText.get(getViewID(editText));
                if ("".equals(s.toString()) || text.equals(lastText) || !editText.isShown()) {
                    return;
                }
                printLog("onTextChanged: " + local.getFamilyString(editText) + " getVisibility:"
                        + editText + " " + editText.getVisibility());
                mTheLastTextChangedTime = System.currentTimeMillis();
                mCurrentEditTextIndex = local.getCurrentViewIndex(editText);
                mEditTextLastText.put(getViewID(editText), text);
                mCurrentEditTextString = text;
                mHasTextChange = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        printLog("hookEditText [" + editText + "]");
        mAllEditTexts.add(editText);
    }

    private void handleOnTouchListener(View view) {
        OnTouchListener onTouchListener = (OnTouchListener) getListener(view, "mOnTouchListener");

        // has hooked listener
        if (onTouchListener != null && mAllListenerHashcodes.contains(onTouchListener.hashCode())) {
            return;
        }

        if (null != onTouchListener) {
            hookOnTouchListener(view, onTouchListener);
        } else {
            // printLog("setOnTouchListener [" + view + "]");
            OnTouchListener onTouchListenerHooked = new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    addEvent(v, event);
                    return false;
                }
            };
            setListener(view, "mOnTouchListener", onTouchListenerHooked);
        }

        // save hashcode of hooked listener
        OnTouchListener onTouchListenerHooked = (OnTouchListener) getListener(view,
                "mOnTouchListener");
        if (onTouchListenerHooked != null) {
            mAllListenerHashcodes.add(onTouchListenerHooked.hashCode());
        }
    }

    private void hookOnTouchListener(View view, OnTouchListener onTouchListener) {
        // printLog("hookOnTouchListener [" + view + "(" + local.getViewText(view) + ")]");

        // save old listener
        mOnTouchListeners.put(getViewID(view), onTouchListener);

        // set hook listener
        OnTouchListener onTouchListenerHooked = new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                OnTouchListener onTouchListener = mOnTouchListeners.get(getViewID(v));
                OnTouchListener onTouchListenerHooked = (OnTouchListener) getListener(v,
                        "mOnTouchListener");
                addEvent(v, event);
                // TODO It's a bug. It can not be fix by below.
                if (onTouchListener != null) {
                    if (onTouchListener.equals(onTouchListenerHooked)) {
                        printLog("onTouchListenerHooked == onTouchListener!!!");
                        return false;
                    }
                    return onTouchListener.onTouch(v, event);
                } else {
                    printLog("onTouchListener == null");
                }
                return false;
            }
        };
        setListener(view, "mOnTouchListener", onTouchListenerHooked);
    }

    private void addEvent(View v, MotionEvent event) {
        //        printLog(v + " " + event);
        if (!offerMotionEventQueue(new RecordMotionEvent(v, event.getAction(), event.getRawX(),
                event.getRawY(), SystemClock.currentThreadTimeMillis()))) {
            printLog("Add to mMotionEventQueue Failed! view:" + v + "\t" + event.toString()
                    + "mMotionEventQueue.size=" + mMotionEventQueue.size());
        }
    }

    private void handleOnItemClickListener(AdapterView<?> adapterView) {
        OnItemClickListener onItemClickListener = (OnItemClickListener) getListener(adapterView,
                "mOnItemClickListener");

        // has hooked listener
        if (onItemClickListener != null
                && mAllListenerHashcodes.contains(onItemClickListener.hashCode())) {
            return;
        }

        if (null != onItemClickListener) {
            printLog("hook AdapterView [" + adapterView + "]");
            // save old listener
            mOnItemClickListeners.put(getViewID(adapterView), onItemClickListener);
        } else {
            printLog("set onItemClickListener at [" + adapterView + "]");
        }

        OnItemClickListener onItemClickListenerHooked = new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setOnItemClick(parent, view, position, id);
            }
        };
        setListener(adapterView, "mOnItemClickListener", onItemClickListenerHooked);

        // save hashcode of hooked listener
        onItemClickListenerHooked = (OnItemClickListener) getListener(adapterView,
                "mOnItemClickListener");
        if (onItemClickListenerHooked != null) {
            mAllListenerHashcodes.add(onItemClickListenerHooked.hashCode());
        }
    }

    /**
     * @param parent
     * @param view
     * @param position
     *            it can not be used for mutiple columns listview
     * @param id
     */
    private void setOnItemClick(AdapterView<?> parent, View view, int position, long id) {
        //use center of item 
        //        int[] center = LocalLib.getViewCenter(view);
        //        DragEvent dragEvent = new DragEvent(view);
        //        dragEvent.setCode(getDragCode(center[0], center[0], center[1], center[1], MIN_STEP_COUNT));
        //        dragEvent.setLog("genernated by setOnItemClick");
        //        offerOutputEventQueue(dragEvent);

        ClickEvent clickEvent = new ClickEvent(parent);

        String r = getRString(parent);
        String rString = r.equals("") ? "" : "[" + r + "]";
        String click = "";
        if ("".equals(rString)) {
            String familyString = local.getFamilyString(parent);
            click = String.format("local.clickInList(%s, \"%s\");", position, familyString);
        } else {
            String rStringSuffix = getRStringSuffix(parent);
            int index = local.getResIdIndex(parent);
            click = String.format("local.clickInList(%s, \"id/%s\", \"%s\");", position,
                    rStringSuffix, index);
        }

        clickEvent.setCode(click);
        clickEvent.setLog("parent: " + parent + " view: " + view + " position: " + position
                + " click");
        offerOutputEventQueue(clickEvent);

        OnItemClickListener onItemClickListener = mOnItemClickListeners.get(getViewID(parent));
        OnItemClickListener onItemClickListenerHooked = (OnItemClickListener) getListener(parent,
                "mOnItemClickListener");
        if (onItemClickListener != null) {
            // TODO It's a bug. It can not be fix by below.
            if (onItemClickListener.equals(onItemClickListenerHooked)) {
                printLog("onItemClickListener == onItemClickListenerHooked!!!");
                return;
            }
            onItemClickListener.onItemClick(parent, view, position, id);
        } else {
            printLog("onItemClickListener == null");
            //parent.performItemClick(view, position, id);
        }
    }

    private void handleOnItemLongClickListener(AdapterView<?> view) {
        //        if (local.isSize0(view)) {
        //            printLog(view + " is size 0 at handleOnItemLongClickListener");
        //            return;
        //        }

        OnItemLongClickListener onItemLongClickListener = (OnItemLongClickListener) getListener(
                view, "mOnItemLongClickListener");

        // has hooked listener
        if (onItemLongClickListener != null
                && mAllListenerHashcodes.contains(onItemLongClickListener.hashCode())) {
            return;
        }

        if (null != onItemLongClickListener) {
            printLog("hookOnItemLongClickListener [" + view + "(" + local.getViewText(view) + ")]");

            // save old listener
            mOnItemLongClickListeners.put(getViewID(view), onItemLongClickListener);

            // set hook listener
            view.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    setOnLongClick(view);
                    OnItemLongClickListener onItemLongClickListener = mOnItemLongClickListeners
                            .get(getViewID(parent));
                    if (onItemLongClickListener != null) {
                        return onItemLongClickListener.onItemLongClick(parent, view, position, id);
                    } else {
                        printLog("onItemLongClickListener == null");
                    }
                    return false;
                }
            });

            // save hashcode of hooked listener
            OnItemLongClickListener onItemLongClickListenerHooked = (OnItemLongClickListener) getListener(
                    view, "mOnItemLongClickListener");
            if (onItemLongClickListenerHooked != null) {
                mAllListenerHashcodes.add(onItemLongClickListenerHooked.hashCode());
            }
        } else {
            printLog("setOnItemLongClickListener at " + view);
            view.setOnItemLongClickListener(new OnItemLongClickListener() {

                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    setOnLongClick(view);
                    return false;
                }
            });
        }
    }

    private void handleOnLongClickListener(View view) {
        if (local.isSize0(view)) {
            printLog(view + " is size 0 " + Log.getThreadInfo());
            invokeOriginOnLongClickListener(view);
            return;
        }

        OnLongClickListener onLongClickListener = (OnLongClickListener) getListener(view,
                "mOnLongClickListener");

        // has hooked listener
        if (onLongClickListener != null
                && mAllListenerHashcodes.contains(onLongClickListener.hashCode())) {
            return;
        }

        if (null != onLongClickListener) {
            printLog("hookOnLongClickListener [" + view + "(" + local.getViewText(view) + ")]");

            // save old listener
            mOnLongClickListeners.put(getViewID(view), onLongClickListener);

            // set hook listener
            view.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    setOnLongClick(v);
                    invokeOriginOnLongClickListener(v);
                    return false;
                }
            });

            // save hashcode of hooked listener
            OnLongClickListener onLongClickListenerHooked = (OnLongClickListener) getListener(view,
                    "mOnLongClickListener");
            if (onLongClickListenerHooked != null) {
                mAllListenerHashcodes.add(onLongClickListenerHooked.hashCode());
            }
        } else {
            printLog("setOnLongClickListener at " + view);
            view.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    setOnLongClick(v);
                    return false;
                }
            });
        }
    }

    private void invokeOriginOnLongClickListener(View v) {
        OnLongClickListener onLongClickListener = mOnLongClickListeners.get(getViewID(v));
        if (onLongClickListener != null) {
            onLongClickListener.onLongClick(v);
        } else {
            printLog("onLongClickListener == null");
        }
    }

    private void setOnLongClick(View v) {
        ClickEvent clickEvent = new ClickEvent(v);
        String viewClass = getViewString(v);
        String familyString = local.getFamilyString(v);
        String r = getRString(v);
        String rString = r.equals("") ? "" : "[" + r + "]";
        String comments = String.format("[%s]%s[%s] ", v, rString, local.getViewText(v));
        String click = "";

        if ("".equals(rString)) {
            click = String.format("local.clickOn(\"%s\", \"%s\", true);//%s%s", viewClass,
                    familyString, "Long Click On ", getFirstLine(comments));
        } else {
            String rStringSuffix = getRStringSuffix(v);
            int index = local.getResIdIndex(v);
            click = String.format("local.clickOn(\"id/%s\", \"%s\", true);//%s%s", rStringSuffix,
                    index, "Long Click On ", getFirstLine(comments));
        }

        clickEvent.setCode(click);

        // clickEvent.setLog();
        offerOutputEventQueue(clickEvent);
        mIsLongClick = true;
    }

    private void handleOutputEventQueue() {
        // merge event in 50ms by their priorities
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    ArrayList<OutputEvent> events = new ArrayList<OutputEvent>();
                    while (true) {
                        OutputEvent e = pollOutputEventQueue();
                        if (e != null) {
                            events.add(e);
                            if (e.view instanceof WebView || mDragWithoutUp) {
                                sleep(1000);
                            } else {
                                sleep(400);
                            }
                            // get all event
                            while ((e = pollOutputEventQueue()) != null) {
                                events.add(e);
                            }

                            Collections.sort(events, new SortByPriority());
                            events = removeDuplicatePriority(events);
                            Collections.sort(events, new SortByView());
                            outputEvents(events);
                            events.clear();
                            mDragWithoutUp = false;
                        } else {
                            sleep(50);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "handleOutputEventQueue").start();
    }

    private ArrayList<OutputEvent> removeDuplicatePriority(ArrayList<OutputEvent> events) {
        if (events.size() < 2) {
            return events;
        }

        ArrayList<OutputEvent> newEvents = new ArrayList<OutputEvent>();
        newEvents.add(events.get(0));
        for (int i = 1; i < events.size(); i++) {
            OutputEvent left = events.get(i - 1);
            OutputEvent current = events.get(i);
            if (current.priority != left.priority) {
                newEvents.add(current);
            }
        }
        return newEvents;
    }

    private OutputEvent pollOutputEventQueue() {
        synchronized (mSyncOutputEventQueue) {
            return mOutputEventQueue.poll();
        }
    }

    public boolean offerOutputEventQueue(OutputEvent e) {
        synchronized (mSyncOutputEventQueue) {
            mTheCurrentEventOutputime = System.currentTimeMillis();
            return mOutputEventQueue.offer(e);
        }
    }

    private RecordMotionEvent pollMotionEventQueue() {
        synchronized (mSyncMotionEventQueue) {
            return mMotionEventQueue.poll();
        }
    }

    private boolean offerMotionEventQueue(RecordMotionEvent e) {
        synchronized (mSyncMotionEventQueue) {
            return mMotionEventQueue.offer(e);
        }
    }

    private void outputEvents(ArrayList<OutputEvent> events) {
        for (OutputEvent outputEvent : filterByRelationship(filterByProity(events))) {
            outputAnEvent(outputEvent);
        }
    }

    /**
     * get the youngest event from family and ignore parent events
     */
    private ArrayList<OutputEvent> filterByRelationship(ArrayList<OutputEvent> events) {
        ArrayList<OutputEvent> newEvents = new ArrayList<OutputEvent>();

        // init eventsFlag
        int[] eventsFlag = new int[events.size()];
        for (int i = 0; i < eventsFlag.length; i++) {
            eventsFlag[i] = 0;
        }

        for (int i = 0; i < events.size(); i++) {
            if (1 == eventsFlag[i]) {
                continue;
            }
            OutputEvent outputEvent = events.get(i);
            ArrayList<OutputEvent> eventFamily = getEventsByRelationship(events, outputEvent);
            // get the longest family string
            Collections.sort(eventFamily, new SortByFamilyString());
            newEvents.add(eventFamily.get(0));

            // mark event which have been handled
            for (int j = 0; j < events.size(); j++) {
                eventsFlag[j] = eventFamily.contains(events.get(j)) ? 1 : 0;
            }
        }

        return newEvents;
    }

    private ArrayList<OutputEvent> getEventsByRelationship(ArrayList<OutputEvent> events,
            OutputEvent targetOutputEvent) {
        ArrayList<OutputEvent> newEvents = new ArrayList<OutputEvent>();
        for (OutputEvent outputEvent : events) {
            if (getRelationship(targetOutputEvent.view, outputEvent.view) != 0) {
                newEvents.add(outputEvent);
            }
        }
        return newEvents;
    }

    /**
     * ignore low proity event
     */
    private ArrayList<OutputEvent> filterByProity(ArrayList<OutputEvent> events) {
        ArrayList<OutputEvent> newEvents = new ArrayList<OutputEvent>();
        int maxIndex = events.size() - 1;

        for (int i = 0; i <= maxIndex;) {
            OutputEvent event = events.get(i);
            if (i == maxIndex) {
                newEvents.add(event);
                break;
            }

            // NOTICE: Assume that one action just generates two outputevents.
            OutputEvent nextEvent = events.get(i + 1);
            if (getRelationship(event.view, nextEvent.view) != 0) {
                i += 2;
                // printLog("" + event.proity + " " + nextEvent.proity);
                if (event.priority > nextEvent.priority) {
                    //printLog("event.proity > nextEvent.proity");
                    newEvents.add(event);
                } else if (event.priority < nextEvent.priority) {
                    //printLog("event.proity < nextEvent.proity");
                    newEvents.add(nextEvent);
                } else {
                    printLog("event.proity == nextEvent.proity");
                    newEvents.add(event);
                    newEvents.add(nextEvent);
                }
            } else {
                i = nextEvent.priority == event.priority ? i + 2 : i + 1;
                newEvents.add(event);
            }
        }

        return newEvents;
    }

    private int getRelationship(View v1, View v2) {
        String familyString1 = local.getFamilyString(v1);
        String familyString2 = local.getFamilyString(v2);
        if (familyString1.contains(familyString2)) {
            return -1;// -1 means v1 is a child of v2
        } else if (familyString2.contains(familyString1)) {
            return 1;// 1 means v1 is a parent of v2
        } else {
            return 0;// 0 means v1 has no relationship with v2
        }
    }

    private void outputAnEvent(OutputEvent event) {
        if (mTheCurrentEventOutputime >= mTheLastTextChangedTime) {
            if (outputEditTextEvent()) {
                printCode(event.getCode());
            } else {
                printCode(getSleepCode() + "\n" + event.getCode());
            }
            printLog(event.getLog());
        } else {
            printCode(getSleepCode() + "\n" + event.getCode());
            printLog(event.getLog());
            outputEditTextEvent();
        }
    }

    private boolean outputEditTextEvent() {
        if ("".equals(mCurrentEditTextString) || mCurrentEditTextIndex < 0 || !mHasTextChange) {
            return false;
        }

        String code = String.format("local.enterText(%s, \"%s\", false);", mCurrentEditTextIndex,
                mCurrentEditTextString);
        printCode(getSleepCode() + "\n" + code);

        // restore var
        mCurrentEditTextString = "";
        mCurrentEditTextIndex = -1;
        mHasTextChange = false;
        return true;
    }

    private final static int TIMEOUT_NEXT_EVENT = 100;

    /**
     * check mMotionEventQueue and merge MotionEvent to drag
     */
    private void handleRecordMotionEventQueue() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                ArrayList<RecordMotionEvent> events = new ArrayList<RecordMotionEvent>();
                while (true) {
                    // find MotionEvent with ACTION_UP
                    RecordMotionEvent e = null;
                    boolean isUp = false;
                    boolean isDown = false;
                    long timeout = 0;
                    while (true) {
                        if ((e = pollMotionEventQueue()) != null) {
                            events.add(e);
                            if (MotionEvent.ACTION_UP == e.action) {
                                isUp = true;
                                isDown = false;
                                break;
                            }
                            if (MotionEvent.ACTION_MOVE == e.action) {
                                isDown = false;
                            }
                            if (MotionEvent.ACTION_DOWN == e.action) {
                                isDown = true;
                                timeout = System.currentTimeMillis() + TIMEOUT_NEXT_EVENT;
                            }
                            if (e.view instanceof ScrollView
                                    && "".equals(mFamilyStringBeforeScroll)) {
                                mFamilyStringBeforeScroll = local.getFamilyString(e.view);
                            }
                        }

                        if (isDown
                                && System.currentTimeMillis() > timeout
                                && mCurrentScrollState != OnScrollListener.SCROLL_STATE_FLING
                                && mCurrentScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
                                && !mIsAbsListViewToTheEnd) {
                            // events.get(0) is ACTION_DOWN
                            if (!isParentScrollable(events.get(0).view)) {
                                printLog("output a drag without up at " + events.get(0).view);
                                mDragWithoutUp = true;
                                mergeMotionEvents(events);
                                events.clear();
                                isDown = false;
                            } else {
                                //printLog("ignore a drag without up");
                            }
                        }
                        sleep(10);
                    }

                    if (isUp) {
                        // remove other views
                        // View targetView = events.get(events.size() - 1).view;
                        ArrayList<RecordMotionEvent> aTouch = new ArrayList<RecordMotionEvent>();
                        for (RecordMotionEvent recordMotionEvent : events) {
                            // if (recordMotionEvent.view.equals(targetView)) {
                            aTouch.add(recordMotionEvent);
                            // }
                        }
                        mDragWithoutUp = false;
                        mergeMotionEvents(aTouch);
                        events.clear();
                    }
                    sleep(50);
                }
            }
        }, "handleRecordMotionEventQueue").start();
    }

    /**
     * Merge touch events from ACTION_DOWN to ACTION_UP.
     */
    private void mergeMotionEvents(ArrayList<RecordMotionEvent> events) {
        RecordMotionEvent down = events.get(0);
        RecordMotionEvent up = events.get(events.size() - 1);
        DragEvent dragEvent = new DragEvent(up.view);

        if (up.view instanceof ScrollView) {
            outputAfterScrollStop((ScrollView) up.view, dragEvent);
            return;
        }

        int stepCount = events.size() - 2;
        stepCount = stepCount > MIN_STEP_COUNT ? stepCount : MIN_STEP_COUNT;
        long duration = up.time - down.time;
        /*
        if (0 == duration) {
            printLog("ignore drag event of [" + up.view + "] because 0 == duration");
            printLog("x:" + up.x + " y:" + up.y);
            return;
        }*/

        dragEvent.setLog(String.format(
                "Drag [%s<%s>] from (%s,%s) to (%s, %s) by duration %s step %s", up.view,
                local.getFamilyString(up.view), down.x, down.y, up.x, up.y, duration, stepCount));
        dragEvent.setCode(getDragCode(down.x, up.x, down.y, up.y, stepCount));

        if (up.view instanceof AbsListView || mIsLongClick
        /*|| (up.view instanceof WebView && DEBUG_WEBVIEW)*/) {
            printLog("ignore drag event of [" + up.view + "]");
            mIsLongClick = false;
            return;
        }

        // wait for other type event
        sleep(100);
        offerOutputEventQueue(dragEvent);
    }

    private String getDragCode(float downX, float upX, float downY, float upY, int stepCount) {
        return String.format("local.dragPercent(%sf, %sf, %sf, %sf, %s);", local.toPercentX(downX),
                local.toPercentX(upX), local.toPercentY(downY), local.toPercentY(upY), stepCount);
    }

    /**
     * Start a thread to wait for scroll stoping, and return immediately.
     * 
     * @param scrollView
     * @param dragEvent
     */
    private void outputAfterScrollStop(final ScrollView scrollView, final DragEvent dragEvent) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (!local.isScrollStoped(scrollView)) {
                    // wait for scroll stoping
                }
                if ("".equals(mFamilyStringBeforeScroll)) {
                    printLog("mFamilyStringBeforeScroll is \"\"");
                    return;
                }
                int scrollX = scrollView.getScrollX();
                int scrollY = scrollView.getScrollY();
                String drag = String.format(
                        "local.scrollScrollViewToWithFamilyString(\"%s\", %s, %s);",
                        mFamilyStringBeforeScroll, scrollX, scrollY);
                mFamilyStringBeforeScroll = "";
                dragEvent.setLog(String.format("Scroll [%s] to (%s, %s)", scrollView, scrollX,
                        scrollY));
                dragEvent.setCode(drag);
                outputAnEvent(dragEvent);
            }
        }, "outputAfterScrollStop").start();
    }

    private void handleOnKeyListener(View view) {
        // for thread safe
        if (null == view) {
            return;
        }

        OnKeyListener onKeyListener = (OnKeyListener) getListener(view, "mOnKeyListener");

        // has hooked listener
        if (onKeyListener != null && mAllListenerHashcodes.contains(onKeyListener.hashCode())) {
            return;
        }

        if (null != onKeyListener) {
            hookOnKeyListener(view, onKeyListener);
        } else {
            // printLog("setOnKeyListener [" + view + "]");
            view.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    setOnKey(v, keyCode, event);
                    return false;
                }
            });
        }

        // save hashcode of hooked listener
        OnKeyListener onKeyListenerHooked = (OnKeyListener) getListener(view, "mOnKeyListener");
        if (onKeyListenerHooked != null) {
            mAllListenerHashcodes.add(onKeyListenerHooked.hashCode());
        }
    }

    private void hookOnKeyListener(View view, OnKeyListener onKeyListener) {
        printLog("hookOnKeyListener [" + view + "]");
        mOnKeyListeners.put(getViewID(view), onKeyListener);
        view.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                setOnKey(v, keyCode, event);
                OnKeyListener onKeyListener = mOnKeyListeners.get(getViewID(v));
                if (null != onKeyListener) {
                    onKeyListener.onKey(v, keyCode, event);
                } else {
                    printLog("onKeyListener == null");
                }
                return false;
            }
        });
    }

    private void setOnKey(View view, int keyCode, KeyEvent event) {
        // ignore KeyEvent.ACTION_DOWN
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (view instanceof EditText && keyCode != KeyEvent.KEYCODE_MENU
                    && keyCode != KeyEvent.KEYCODE_BACK) {
                return;
            }
            HardKeyEvent hardKeyEvent = new HardKeyEvent(view);
            String sendKey = String.format("local.sendKey(KeyEvent.%s);", mKeyCodeMap.get(keyCode));
            hardKeyEvent.setCode(sendKey);
            hardKeyEvent.setLog("view: " + view + " " + event);

            offerOutputEventQueue(hardKeyEvent);
        }
    }

    private String getSleepCode() {
        String screenShotCode = String.format("local.screenShotNamedCaseName(\"%s\");",
                mEventCount++);
        return String.format("local.sleep(%s);\n%s", getSleepTime(), screenShotCode);
    }

    /**
     * for view.getId() == -1
     */
    private String getViewID(View view) {
        if (null == view) {
            printLog("null == view " + Log.getThreadInfo());
            return "";
        }

        try {
            String viewString = view.toString();
            if (viewString.indexOf('@') != -1) {
                return viewString.substring(viewString.indexOf("@"));
            } else if (viewString.indexOf('{') != -1) {
                // after android 4.2
                int leftBracket = viewString.indexOf('{');
                int firstSpace = viewString.indexOf(' ');
                return viewString.substring(leftBracket + 1, firstSpace);
            } else {
                return viewString + view.getId();
            }
        } catch (Exception e) {
            // TODO: handle exception
            return String.valueOf(view.getId());
        }
    }

    private String getViewString(View view) {
        return view.getClass().toString().split(" ")[1];
    }

    private void initKeyTable() {
        KeyEvent keyEvent = new KeyEvent(0, 0);
        ArrayList<String> names = LocalLib.getPropertyNameByType(keyEvent, 0, int.class);
        try {
            for (String name : names) {
                if (name.startsWith("KEYCODE_")) {
                    Integer keyCode = (Integer) LocalLib.getObjectProperty(keyEvent, 0, name);
                    mKeyCodeMap.put(keyCode, name);
                }
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

    private boolean isParentScrollable(View view) {
        while (view.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent instanceof ScrollView || parent instanceof AbsListView) {
                return true;
            }
            view = parent;
        }
        return null == view.getParent() ? true : false;
    }
}
