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

package com.baidu.cafe.local;

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

import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.Log;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
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

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-8
 * @version
 * @todo
 */
public class ViewRecorder {
    private final static String                      REPLAY_CLASS_NAME         = "CafeReplay";
    private final static String                      REPLAY_FILE_NAME          = REPLAY_CLASS_NAME
                                                                                       + ".java";
    private final static int                         MAX_SLEEP_TIME            = 20000;
    private final static int                         MIN_SLEEP_TIME            = 1000;

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
    private String                                   mCurrentActivityString    = null;
    private Activity                                 mCurrentActivity          = null;
    private String                                   mPath                     = null;
    private String                                   mCurrentEditTextString    = null;
    private int                                      mCurrentEditTextIndex     = 0;
    private int                                      mFirstVisibleItem         = 0;
    private int                                      mVisibleItemCount         = 0;
    private int                                      mTotalItemCount           = 0;
    private int                                      mLastFirstVisibleItem     = 0;

    /**
     * interval between events
     */
    private long                                     mLastEventTime            = System.currentTimeMillis();

    public ViewRecorder(LocalLib local) {
        this.local = local;
        init();
    }

    class RecordMotionEvent {
        public View  view;
        public float x;
        public float y;
        public int   action;

        public RecordMotionEvent(View view, int action, float x, float y) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.action = action;
        }

        @Override
        public String toString() {
            return String
                    .format("RecordMotionEvent(%s, action=%s, x=%s, y=%s)", view, action, x, y);
        }

    }

    class OutputEvent {
        final static int PRIORITY_ACTIVITY = 0;
        final static int PRIORITY_DRAG     = 1;
        final static int PRIORITY_KEY      = 2;
        final static int PRIORITY_SCROLL   = 3;
        final static int PRIORITY_CLICK    = 4;

        /**
         * NOTICE: This field can not be null!
         */
        public View      view              = null;
        public int       proity            = 0;
        protected String code              = "";
        protected String log               = "";

        public String getCode() {
            return code;
        }

        public String getLog() {
            return log;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setLog(String log) {
            this.log = log;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", view, proity);
        }

    }

    class ClickEvent extends OutputEvent {
        public ClickEvent(View view) {
            this.view = view;
            this.proity = PRIORITY_CLICK;
        }
    }

    class DragEvent extends OutputEvent {
        public DragEvent(View view) {
            this.view = view;
            this.proity = PRIORITY_DRAG;
        }
    }

    class ActivityEvent extends OutputEvent {
        public ActivityEvent(View view) {
            this.view = view;
            this.proity = PRIORITY_ACTIVITY;
        }
    }

    class HardKeyEvent extends OutputEvent {
        public HardKeyEvent(View view) {
            this.view = view;
            this.proity = PRIORITY_KEY;
        }
    }

    class ScrollEvent extends OutputEvent {
        public ScrollEvent(View view) {
            this.view = view;
            this.proity = PRIORITY_SCROLL;
        }
    }

    /**
     * sort by view.hashCode()
     */
    class SortByView implements Comparator<OutputEvent> {
        public int compare(OutputEvent e1, OutputEvent e2) {
            if (null == e1 || null == e1.view) {
                return 0;
            }
            if (null == e2 || null == e2.view) {
                return 1;
            }
            if (e1.view.hashCode() > e2.view.hashCode()) {
                return 1;
            }
            return 0;
        }
    }

    private void print(String tag, String message) {
        if (Log.IS_DEBUG) {
            Log.i(tag, message);
        }
    }

    private void printLog(String message) {
        print("ViewRecorder", message);
    }

    private void printLayout(View view) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        String msg = String.format("[%s][%s][%s][%s][%s,%s,%s,%s]", mCurrentActivityString,
                getRString(view), view, local.getViewText(view), xy[0], xy[1], view.getWidth(),
                view.getHeight());
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
                LocalLib.executeOnDevice("chmod 777 " + mPath + "/" + REPLAY_FILE_NAME, "/");
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
        mPackageName = local.getCurrentActivity().getPackageName();
        String launcherActivity = local.getCurrentActivity().getClass().getName();
        initKeyTable();

        // init cafe dir
        mPath = "/data/data/" + mPackageName + "/cafe";
        File cafe = new File(mPath);
        if (!cafe.exists()) {
            cafe.mkdir();
            LocalLib.executeOnDevice("chmod 777 " + mPath, "/");
        }

        // init template
        mRecord = new File(mPath + "/" + REPLAY_FILE_NAME);
        if (mRecord.exists()) {
            mRecord.delete();
        }
        String code = String.format(template, launcherActivity, mPackageName);
        writeToFile(code);
    }

    String template = "package com.example.demo.test;\n" + "\n"
                            + "import com.baidu.cafe.CafeTestCase;\n"
                            + "import android.view.KeyEvent;\n" + "// next import\n" + "\n"
                            + "public class "
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
                            + "    protected void setUp() throws Exception {\n"
                            + "        super.setUp();\n"
                            + "    }\n"
                            + "\n"
                            + "    @Override\n"
                            + "    protected void tearDown() throws Exception {\n"
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

        // keep hooking new views
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    setDefaultFocusView();
                    ArrayList<View> newViews = getTargetViews();
                    for (View view : newViews) {
                        try {
                            setHookListenerOnView(view);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    sleep(50);
                }
            }
        }).start();

        handleRecordMotionEventQueue();
        handleOutputEventQueue();

        local.sleep(3000);
        printLog("ViewRecorder is ready to work.");
        mLastEventTime = System.currentTimeMillis();
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
        }).start();
    }

    private void outputAnActivityEvent(Class<? extends Activity> activityClass) {
        String activity = activityClass.getName();
        ActivityEvent activityEvent = new ActivityEvent(null);
        //        activityEvent.setCode(String.format("assertTrue(local.waitForActivityWithSleep(\"%s\"));",
        //                activitySimpleName));
        activityEvent.setLog(String.format("Wait for Activity(%s)", activity));
        outputAnEvent(activityEvent);
    }

    /**
     * @return new activity class
     */
    private Class<? extends Activity> updateCurrentActivity() {
        Activity activity = local.getCurrentActivity();
        Class<? extends Activity> activityClass = activity.getClass();
        String activityName = activityClass.getName();
        if (!activityName.equals(mCurrentActivityString)) {
            outputAnActivityEvent(activityClass);
            mCurrentActivityString = activityName;
            mCurrentActivity = activity;
        }
        return activityClass;
    }

    private ArrayList<View> getTargetViews() {
        ArrayList<View> views = local
                .removeInvisibleViews(local.getCurrentViews()/*onlySufficientlyVisible == true*/);
        ArrayList<View> targetViews = new ArrayList<View>();
        boolean hasChange = false;

        for (View view : views) {
            boolean isOld = mAllViewPosition.containsKey(getViewID(view));
            // refresh view layout
            if (hasChange(view)) {
                hasChange = true;
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

        if (hasChange) {
            flushWhenStatic(views);
        }
        mLastViews.clear();
        mLastViews.addAll(views);

        return targetViews;
    }

    private void flushWhenStatic(final ArrayList<View> views) {
        // It's a lightly way to judge whether two arrays equals.
        if (mLastViews.size() != views.size()) {
            return;
        }

        // It's too slow to be at main thread.
        new Thread(new Runnable() {

            @Override
            public void run() {
                flushViewLayout(views);
            }
        }).start();
    }

    /**
     * For mtc client
     */
    private void flushViewLayout(ArrayList<View> views) {
        updateCurrentActivity();
        print("ViewLayout", String.format("[%s]ViewLayout refreshed.", mCurrentActivity));
        for (View view : views) {
            printLayout(view);
        }
    }

    private void saveView(View view) {
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
        //        if (local.getCurrentActivity().getCurrentFocus() != null) {
        //            return;
        //        }
        if (getCurrentFocusView() != null) {
            return;
        }
        View view = local.getRecentDecorView();
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

    private Object getListener(View view, String listenerName) {
        Class<?> viewClass = null;
        if ("mOnItemClickListener".equals(listenerName)) {
            viewClass = AdapterView.class;
        } else if ("mOnScrollListener".equals(listenerName)) {
            viewClass = AbsListView.class;
        } else if ("mOnChildClickListener".equals(listenerName)
                || "mOnGroupClickListener".equals(listenerName)) {
            viewClass = ExpandableListView.class;
        } else {
            viewClass = View.class;
        }

        return local.getListener(view, viewClass, listenerName);
    }

    private void setHookListenerOnView(View view) {
        if (view instanceof AdapterView) {
            if (view instanceof ExpandableListView) {
                handleExpandableListView((ExpandableListView) view);
            } else {
                handleOnItemClickListener((AdapterView<?>) view);
            }

            if (view instanceof AbsListView) {
                handleOnScrollListener((AbsListView) view);
            }
            // adapterView.setOnItemLongClickListener(listener);
            // adapterView.setOnItemSelectedListener(listener);
            // MenuItem.OnMenuItemClickListener
        }

        handleOnLongClickListener(view);

        if (view instanceof EditText) {
            hookEditText((EditText) view);
            return;
        }

        if (!handleOnClickListener(view)) {
            // If view has ClickListener, do not add a TouchListener.
            handleOnTouchListener(view);
        }
    }

    private void handleOnScrollListener(AbsListView absListView) {
        OnScrollListener onScrollListener = (OnScrollListener) getListener(absListView,
                "mOnScrollListener");
        // has hooked listener
        if (onScrollListener != null && mAllListenerHashcodes.contains(onScrollListener.hashCode())) {
            return;
        }

        if (null != onScrollListener) {
            hookOnScrollListener(absListView, onScrollListener);
        } else {
            printLog("set onScrollListener [" + absListView + "]");
            absListView.setOnScrollListener(new OnScrollListener() {

                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    setOnScrollStateChanged(view, scrollState);
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                        int totalItemCount) {
                    setOnScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
            });
        }

        // save hashcode of hooked listener
        OnScrollListener onScrollListenerHooked = (OnScrollListener) getListener(absListView,
                "mOnScrollListener");

        if (onScrollListenerHooked != null) {
            mAllListenerHashcodes.add(onScrollListenerHooked.hashCode());
        }
    }

    private void setOnScrollStateChanged(AbsListView view, int scrollState) {
        if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
            outputAScroll(view);
        }
        if (OnScrollListener.SCROLL_STATE_TOUCH_SCROLL == scrollState) {
            mLastFirstVisibleItem = view.getFirstVisiblePosition();
        }
    }

    private void outputAScroll(AbsListView view) {
        if (mTotalItemCount == 0 || mVisibleItemCount == 0
                || mLastFirstVisibleItem == mFirstVisibleItem) {
            return;
        }

        ScrollEvent scrollEvent = new ScrollEvent(view);
        int viewIndex = local.getCurrentViewIndex(view);
        if (-1 == viewIndex) {
            return;
        }
        printLog("mFirstVisibleItem:" + mFirstVisibleItem);
        String code = String
                .format("local.scrollListToLine(%s, %s);", viewIndex, mFirstVisibleItem);
        scrollEvent.setCode(code);

        mOutputEventQueue.offer(scrollEvent);
    }

    private void setOnScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
        mTotalItemCount = totalItemCount;

        if (firstVisibleItem + visibleItemCount == totalItemCount) {
            outputAScroll(view);
        }
    }

    private void hookOnScrollListener(AbsListView absListView,
            final OnScrollListener onScrollListener) {
        printLog("hook onScrollListener [" + absListView + "]");

        // save old listener
        //        mOnScrollListeners.put(getViewID(absListView), onScrollListener);
        mOnScrollListeners.put(String.valueOf(absListView.hashCode()), onScrollListener);

        // set hook listener
        OnScrollListener onScrollListenernew = new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                setOnScrollStateChanged(view, scrollState);
                OnScrollListener onScrollListener = mOnScrollListeners.get(getViewID(view));
                OnScrollListener onScrollListenerHooked = (OnScrollListener) getListener(view,
                        "mOnScrollListener");
                if (onScrollListener != null) {
                    if (onScrollListener.equals(onScrollListenerHooked)) {
                        printLog("onScrollListenerHooked == onScrollListener");
                        return;
                    }
                    onScrollListener.onScrollStateChanged(view, scrollState);
                } else {
                    printLog("onScrollListener == null");
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
                    if (onScrollListener.equals(onScrollListenerHooked)) {
                        printLog("onScrollListenerHooked == onScrollListener");
                        return;
                    }
                    onScrollListener.onScroll(view, firstVisibleItem, visibleItemCount,
                            totalItemCount);
                } else {
                    printLog("onScrollListener == null");
                }
            }
        };
        absListView.setOnScrollListener(onScrollListenernew);
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
            expandableListView.setOnGroupClickListener(new OnGroupClickListener() {

                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition,
                        long id) {
                    setOnGroupClick(parent, groupPosition);
                    return false;
                }
            });
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
        int viewIndex = local.getCurrentViewIndex(parent);
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format("local.clickOnExpandableListView(%s, %s);", viewIndex,
                flatListPosition);
        String sleep = String.format("local.sleep(%s);", getSleepTime());
        clickEvent.setCode(sleep + "\n" + code);
        clickEvent.setLog(String.format("click on group[%s]", groupPosition));

        mOutputEventQueue.offer(clickEvent);
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
            expandableListView.setOnChildClickListener(new OnChildClickListener() {

                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                        int childPosition, long id) {
                    setOnChildClick(expandableListView, groupPosition, childPosition);
                    return false;
                }
            });
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
        int viewIndex = local.getCurrentViewIndex(parent);
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format("local.clickOnExpandableListView(%s, %s);", viewIndex,
                flatListPosition);
        String sleep = String.format("local.sleep(%s);", getSleepTime());
        clickEvent.setCode(sleep + "\n" + code);
        clickEvent.setLog(String.format("click on group[%s] child[%s]", groupPosition,
                childPosition));

        mOutputEventQueue.offer(clickEvent);
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
            hookOnClickListener(view, onClickListener);
            return true;
        } else {
            // printLog("onClickListener == null " + view + local.getViewText(view));
        }

        return false;
    }

    private void hookOnClickListener(View view, OnClickListener onClickListener) {
        printLog(String.format("hookClickListener [%s(%s)]", view, local.getViewText(view)));

        // save old listener
        mOnClickListeners.put(getViewID(view), onClickListener);

        // set hook listener
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setOnClick(v);
            }
        });

        // save hashcode of hooked listener
        OnClickListener onClickListenerHooked = (OnClickListener) getListener(view,
                "mOnClickListener");
        if (onClickListenerHooked != null) {
            mAllListenerHashcodes.add(onClickListenerHooked.hashCode());
        }
    }

    private long getSleepTime() {
        long ret = System.currentTimeMillis() - mLastEventTime;
        mLastEventTime = System.currentTimeMillis();
        ret = ret < MIN_SLEEP_TIME ? MIN_SLEEP_TIME : ret;
        ret = ret > MAX_SLEEP_TIME ? MAX_SLEEP_TIME : ret;
        return ret;
    }

    private void setOnClick(View v) {
        // set click event output
        ClickEvent clickEvent = new ClickEvent(v);
        String viewClass = getViewString(v);
        //        int viewIndex = local.getCurrentViewIndex(v);
        String familyString = local.getFamilyString(v);
        String r = getRString(v);
        String rString = r.equals("") ? "" : "[" + r + "]";
        String comments = String.format("[%s]%s[%s] ", v, rString, local.getViewText(v));
        String importLine = String.format("import %s;", getViewString(v));
        //        String wait = String.format("assertTrue(local.waitForView(\"%s\", %s, %s, false));//%s%s",
        //                viewClass, viewIndex, WAIT_TIMEOUT, "Wait for ", comments);
        String click = String.format("local.clickOn(\"%s\", \"%s\");//%s%s", viewClass,
                familyString, "Click On ", comments);

        String sleep = String.format("local.sleep(%s);", getSleepTime());
        clickEvent.setCode(importLine + "\n" + sleep + "\n" + click);

        // clickEvent.setLog();
        mOutputEventQueue.offer(clickEvent);

        OnClickListener onClickListener = mOnClickListeners.get(getViewID(v));
        if (onClickListener != null) {
            onClickListener.onClick(v);
        } else {
            printLog("onClickListener == null");
        }
    }

    private String getRString(View view) {
        int id = view.getId();
        if (-1 == id) {
            return "";
        }

        try {
            String rString = mCurrentActivity.getResources().getResourceName(view.getId());
            return "R.id." + rString.substring(rString.lastIndexOf("/") + 1, rString.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void hookEditText(final EditText editText) {
        if (mAllEditTexts.contains(editText)) {
            return;
        }

        editText.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus || null == mCurrentEditTextString) {
                    return;
                }

                // flush EditText event
                String code = String.format("local.enterText(%s, \"%s\", false);",
                        mCurrentEditTextIndex, mCurrentEditTextString);
                String sleep = String.format("local.sleep(%s);", getSleepTime());
                printCode(sleep + "\n" + code);
                printLog("text:" + mCurrentEditTextString);

                // restore var
                mCurrentEditTextString = null;
                mCurrentEditTextIndex = 0;
            }
        });

        // all TextWatcher works at the same time
        final int viewIndex = local.getCurrentViewIndex(editText);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ("".equals(s.toString())) {
                    return;
                }
                mCurrentEditTextIndex = viewIndex;
                mCurrentEditTextString = s.toString().replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\r\n", "\\n").replace("\n", "\\n");
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
            view.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    addEvent(v, event);
                    return false;
                }
            });
        }

        // save hashcode of hooked listener
        OnTouchListener onTouchListenerHooked = (OnTouchListener) getListener(view,
                "mOnTouchListener");
        if (onTouchListenerHooked != null) {
            mAllListenerHashcodes.add(onTouchListenerHooked.hashCode());
        }
    }

    private void hookOnTouchListener(View view, OnTouchListener onTouchListener) {
        //        print("hookOnTouchListener [" + view + "(" + local.getViewText(view) + ")]"
        //                + (view instanceof ViewGroup ? "ViewGroup" : "View"));

        // save old listener
        mOnTouchListeners.put(getViewID(view), onTouchListener);

        // set hook listener
        view.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                OnTouchListener onTouchListener = mOnTouchListeners.get(getViewID(v));
                addEvent(v, event);
                if (onTouchListener != null) {
                    onTouchListener.onTouch(v, event);
                } else {
                    printLog("onTouchListener == null");
                }
                return false;
            }
        });
    }

    private void addEvent(View v, MotionEvent event) {
        //        printLog(v + " " + event);
        if (!mMotionEventQueue.offer(new RecordMotionEvent(v, event.getAction(), event.getRawX(),
                event.getRawY()))) {
            printLog("Add to mMotionEventQueue Failed! view:" + v + "\t" + event.toString()
                    + "mMotionEventQueue.size=" + mMotionEventQueue.size());
        }
    }

    private void handleOnItemClickListener(AdapterView<?> view) {
        OnItemClickListener onItemClickListener = (OnItemClickListener) getListener(view,
                "mOnItemClickListener");

        // has hooked listener
        if (onItemClickListener != null
                && mAllListenerHashcodes.contains(onItemClickListener.hashCode())) {
            return;
        }

        if (null != onItemClickListener) {
            printLog("hook AdapterView [" + view + "]");

            // save old listener
            mOnItemClickListeners.put(getViewID(view), onItemClickListener);
        } else {
            printLog("set onItemClickListener at [" + view + "]");
        }

        // set hook listener
        view.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setOnItemClick(parent, view, position, id);
            }
        });

        // save hashcode of hooked listener
        OnItemClickListener onItemClickListenerHooked = (OnItemClickListener) getListener(view,
                "mOnItemClickListener");
        if (onItemClickListenerHooked != null) {
            mAllListenerHashcodes.add(onItemClickListenerHooked.hashCode());
        }
    }

    private void setOnItemClick(AdapterView<?> parent, View view, int position, long id) {
        ClickEvent clickEvent = new ClickEvent(parent);
        int childIndex = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).equals(view)) {
                childIndex = i;
            }
        }
        String familyString = local.getFamilyString(parent);
        String click = String.format("local.clickInListWithFamilyString(%s, \"%s\");", position
                - parent.getFirstVisiblePosition() + 1, familyString);
        printLog("position:" + position);
        printLog("getFirstVisiblePosition:" + parent.getFirstVisiblePosition());
        printLog("childIndex:" + childIndex);
        String sleep = String.format("local.sleep(%s);", getSleepTime());
        clickEvent.setCode(sleep + "\n" + click);
        clickEvent.setLog("parent: " + parent + " view: " + view + " position: " + position
                + " click");
        mOutputEventQueue.offer(clickEvent);

        OnItemClickListener onItemClickListener = mOnItemClickListeners.get(getViewID(parent));
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(parent, view, position, id);
        } else {
            printLog("onItemClickListener == null");
            //parent.performItemClick(view, position, id);
        }
    }

    private void handleOnLongClickListener(View view) {
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
                    printLog("local.clickLongOnView(view): " + v);
                    OnLongClickListener onLongClickListener = mOnLongClickListeners
                            .get(getViewID(v));
                    if (onLongClickListener != null) {
                        onLongClickListener.onLongClick(v);
                    } else {
                        printLog("onLongClickListener == null");
                    }
                    return false;
                }
            });

            // save hashcode of hooked listener
            OnLongClickListener onLongClickListenerHooked = (OnLongClickListener) local
                    .getListener(view, View.class, "mOnLongClickListener");
            if (onLongClickListenerHooked != null) {
                mAllListenerHashcodes.add(onLongClickListenerHooked.hashCode());
            }
        }
    }

    private void handleOutputEventQueue() {
        // merge event in 50ms by their priorities
        new Thread(new Runnable() {

            @Override
            public void run() {
                ArrayList<OutputEvent> events = new ArrayList<OutputEvent>();
                while (true) {
                    OutputEvent e = mOutputEventQueue.poll();
                    if (e != null) {
                        events.add(e);
                        sleep(400);

                        // get all event
                        while ((e = mOutputEventQueue.poll()) != null) {
                            events.add(e);
                        }

                        Collections.sort(events, new SortByView());
                        outputEvents(events);
                        events.clear();
                    } else {
                        sleep(50);
                    }
                }
            }
        }).start();
    }

    private void outputEvents(ArrayList<OutputEvent> events) {
        int maxIndex = events.size() - 1;
        for (int i = 0; i <= maxIndex;) {
            OutputEvent event = events.get(i);
            if (i == maxIndex) {
                outputAnEvent(event);
                break;
            }

            // NOTICE: Assume that one action just generates two outputevents.
            OutputEvent nextEvent = events.get(i + 1);
            if (hasRelationship(event.view, nextEvent.view)) {
                i += 2;
                if (event.proity > nextEvent.proity) {
                    //printLog("event.proity > nextEvent.proity");
                    outputAnEvent(event);
                } else if (event.proity < nextEvent.proity) {
                    //printLog("event.proity < nextEvent.proity");
                    outputAnEvent(nextEvent);
                } else {
                    printLog("event.proity == nextEvent.proity");
                    outputAnEvent(event);
                    outputAnEvent(nextEvent);
                }
            } else {
                i = nextEvent.proity == event.proity ? i + 2 : i + 1;
                outputAnEvent(event);
            }
        }
    }

    private boolean hasRelationship(View v1, View v2) {
        String familyString1 = local.getFamilyString(v1);
        String familyString2 = local.getFamilyString(v2);
        return familyString1.contains(familyString2) || familyString2.contains(familyString1) ? true
                : false;
    }

    private void outputAnEvent(OutputEvent event) {
        if (mCurrentEditTextString != null) {
            // flush EditText event
            String code = String.format("local.enterText(%s, \"%s\", false);",
                    mCurrentEditTextIndex, mCurrentEditTextString);
            printCode(code);
            printLog("text:" + mCurrentEditTextString);

            // restore var
            mCurrentEditTextString = null;
            mCurrentEditTextIndex = 0;
        }

        printCode(event.getCode());
        printLog(event.getLog());
    }

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
                    while ((e = mMotionEventQueue.poll()) != null) {
                        events.add(e);
                        if (MotionEvent.ACTION_UP == e.action) {
                            isUp = true;
                            break;
                        }
                    }

                    if (isUp) {
                        // remove other views
                        View targetView = events.get(events.size() - 1).view;
                        ArrayList<RecordMotionEvent> aTouch = new ArrayList<RecordMotionEvent>();
                        for (RecordMotionEvent recordMotionEvent : events) {
                            //                            if (recordMotionEvent.view.equals(targetView)) {
                            aTouch.add(recordMotionEvent);
                            //                            }
                        }

                        mergeMotionEvents(aTouch);
                        events.clear();
                    }
                    sleep(50);
                }
            }
        }).start();
    }

    /**
     * Merge touch events from ACTION_DOWN to ACTION_UP.
     */
    private void mergeMotionEvents(ArrayList<RecordMotionEvent> events) {
        RecordMotionEvent down = events.get(0);
        RecordMotionEvent up = events.get(events.size() - 1);
        int stepCount = events.size() - 2;
        stepCount = stepCount >= 0 ? stepCount : 0;
        DragEvent dragEvent = new DragEvent(up.view);
        String drag = "";

        if (up.view instanceof ScrollView) {
            outputAfterScrollStop((ScrollView) up.view, dragEvent);
            return;
        } else if (up.view instanceof AbsListView) {
            printLog("ignore drag event of [" + up.view + "]");
            return;
        } else {
            String dragString = "local.drag(local.toScreenX(%sf), local.toScreenX(%sf), local.toScreenY(%sf), local.toScreenY(%sf), %s);";
            drag = String.format(dragString, local.toPercentX(down.x), local.toPercentX(up.x),
                    local.toPercentY(down.y), local.toPercentY(up.y), stepCount);
            dragEvent.setLog(String.format("Drag [%s] from (%s,%s) to (%s, %s) by step count %s",
                    up.view, down.x, down.y, up.x, up.y, stepCount));
        }

        String sleep = String.format("local.sleep(%s);", getSleepTime());
        dragEvent.setCode(sleep + "\n" + drag);

        // wait for other type event
        sleep(100);

        mOutputEventQueue.offer(dragEvent);
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
                while (!local.isScrollStoped(scrollView))
                    ;
                int scrollX = scrollView.getScrollX();
                int scrollY = scrollView.getScrollY();
                int viewIndex = local.getCurrentViewIndex(scrollView);
                String drag = String.format("local.scrollScrollViewTo(%s, %s, %s);", viewIndex,
                        scrollX, scrollY);
                dragEvent.setLog(String.format("Scroll [%s] to (%s, %s)", scrollView, scrollX,
                        scrollY));
                String sleep = String.format("local.sleep(%s);", getSleepTime());
                dragEvent.setCode(sleep + "\n" + drag);
                outputAnEvent(dragEvent);
            }
        }).start();
    }

    private void hookOnItemSelectedListener(AdapterView view) {
    }

    private void handleOnKeyListener(View view) {
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
            String sleep = String.format("local.sleep(%s);", getSleepTime());
            hardKeyEvent.setCode(sleep + "\n" + sendKey);
            hardKeyEvent.setLog("view: " + view + " " + event);

            mOutputEventQueue.offer(hardKeyEvent);
        }
    }

    private String getViewID(View view) {
        String viewString = view.toString();
        return viewString.substring(viewString.indexOf("@"));
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

}
