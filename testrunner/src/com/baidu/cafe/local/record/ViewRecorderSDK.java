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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ScrollView;
import android.widget.Spinner;

import com.baidu.cafe.utils.ReflectHelper;

/**
 * A single class transplaned from com.baidu.cafe.local.record.ViewRecorder
 * which is not depended on com.baidu.cafe.local.Locallib.
 * 
 * Usage:
 * 
 * {
 * 
 * super.onCreate();
 * 
 * ViewRecorderSDK vr = new ViewRecorderSDK(this);
 * 
 * vr.beginRecordCode();
 * 
 * vr.pollOutputLogQueue();
 * 
 * }
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2013-10-8
 * @version
 * @todo
 */
public class ViewRecorderSDK {
    private final static int                         MAX_SLEEP_TIME                = 20000;
    private final static int                         MIN_SLEEP_TIME                = 1000;
    private final static int                         MIN_STEP_COUNT                = 4;
    private final static boolean                     DEBUG_WEBVIEW                 = true;
    private final static SimpleDateFormat            mSimpleDateFormat             = new SimpleDateFormat(
                                                                                           "yyyy-MM-dd HH:mm:ss.SSS");

    private static boolean                           mBegin                        = false;

    /**
     * For judging whether a view is an old one.
     * 
     * Key is string of view id.
     * 
     * Value is position array of view.
     */
    private HashMap<String, int[]>                   mAllViewPosition              = new HashMap<String, int[]>();

    /**
     * For judging whether a view has been hooked.
     */
    private ArrayList<Integer>                       mAllListenerHashcodes         = new ArrayList<Integer>(
                                                                                           1024);

    private ArrayList<Integer>                       mAllAbsListViewHashcodes      = new ArrayList<Integer>();

    /**
     * For judging whether a EditText has been hooked.
     */
    private ArrayList<EditText>                      mAllEditTexts                 = new ArrayList<EditText>();

    /**
     * For merge a sequeue of MotionEvents to a drag.
     */
    private Queue<RecordMotionEvent>                 mMotionEventQueue             = new LinkedList<RecordMotionEvent>();

    /**
     * For judging events of the same view at the same time which should be
     * keeped by their priorities.
     */
    private Queue<OutputEvent>                       mOutputEventQueue             = new LinkedList<OutputEvent>();

    /**
     * For saving output log
     */
    private Queue<String>                            mOutputLogQueue               = new LinkedList<String>();

    /**
     * For mapping keycode to keyname
     */
    private HashMap<Integer, String>                 mKeyCodeMap                   = new HashMap<Integer, String>();

    /**
     * lock for OutputEventQueue
     * 
     * NOTICE: new String("") can not replaced by "", because the code
     * synchronizes on interned String. Constant Strings are interned and shared
     * across all other classes loaded by the JVM. Thus, this could is locking
     * on something that other code might also be locking. This could result in
     * very strange and hard to diagnose blocking and deadlock behavior.
     */
    private static String                            mSyncOutputEventQueue         = new String(
                                                                                           "mSyncOutputEventQueue");

    /**
     * lock for MotionEventQueue
     */
    private static String                            mSyncMotionEventQueue         = new String(
                                                                                           "mSyncMotionEventQueue");
    /**
     * lock for OutputLogQueue
     */
    private static String                            mSyncOutputLogQueue           = new String(
                                                                                           "mSyncOutputLogQueue");
    /**
     * lock for AllListenerHashcodes
     */
    private static String                            mSyncAllListenerHashcodes     = new String(
                                                                                           "mSyncAllListenerHashcodes");
    /**
     * Time when event was being generated.
     */
    private long                                     mTheCurrentEventOutputime     = System.currentTimeMillis();

    /**
     * event count for naming screenshot
     */
    private int                                      mEventCount                   = 0;

    /**
     * interval between events
     */
    private long                                     mLastEventTime                = System.currentTimeMillis();

    /**
     * assume that only one ScrollView is fling
     */
    private String                                   mFamilyStringBeforeScroll     = "";

    /**
     * to ignore drag event
     */
    private boolean                                  mIsLongClick                  = false;

    private boolean                                  mDragWithoutUp                = false;

    /**
     * to ignore drag event when "output a drag without up"
     */
    private boolean                                  mIsAbsListViewToTheEnd        = false;

    /**
     * Saving states for each listview
     */
    private HashMap<String, AbsListViewState>        mAbsListViewStates            = new HashMap<String, AbsListViewState>();

    /**
     * save edittext the lastest text
     */
    private HashMap<String, String>                  mEditTextLastText             = new HashMap<String, String>();

    private HashMap<OnClickListener, Integer>        mOnClickListenerInvokeCounter = new HashMap<OnClickListener, Integer>();
    private HashMap<OnTouchListener, Integer>        mOnTouchListenerInvokeCounter = new HashMap<OnTouchListener, Integer>();
    private HashMap<OnKeyListener, Integer>          mOnKeyListenerCounters        = new HashMap<OnKeyListener, Integer>();
    private HashMap<OnScrollListener, Integer>       mOnScrollListenerCounters     = new HashMap<OnScrollListener, Integer>();
    private HashMap<OnGroupClickListener, Integer>   mOnGroupClickListenerCounters = new HashMap<OnGroupClickListener, Integer>();
    private HashMap<OnChildClickListener, Integer>   mOnChildClickListenerCounters = new HashMap<OnChildClickListener, Integer>();
    /**
     * Saving old listener for invoking when needed
     */
    private HashMap<String, OnClickListener>         mOnClickListeners             = new HashMap<String, OnClickListener>();
    private HashMap<String, OnLongClickListener>     mOnLongClickListeners         = new HashMap<String, OnLongClickListener>();
    private HashMap<String, OnTouchListener>         mOnTouchListeners             = new HashMap<String, OnTouchListener>();
    private HashMap<String, OnKeyListener>           mOnKeyListeners               = new HashMap<String, OnKeyListener>();
    private HashMap<String, OnItemClickListener>     mOnItemClickListeners         = new HashMap<String, OnItemClickListener>();
    private HashMap<String, OnGroupClickListener>    mOnGroupClickListeners        = new HashMap<String, OnGroupClickListener>();
    private HashMap<String, OnChildClickListener>    mOnChildClickListeners        = new HashMap<String, OnChildClickListener>();
    private HashMap<String, OnScrollListener>        mOnScrollListeners            = new HashMap<String, OnScrollListener>();
    private HashMap<String, OnItemLongClickListener> mOnItemLongClickListeners     = new HashMap<String, OnItemLongClickListener>();
    private String                                   mPackageName                  = null;
    private int                                      mCurrentEditTextIndex         = 0;
    private String                                   mCurrentEditTextString        = "";
    private boolean                                  mHasTextChange                = false;
    private long                                     mTheLastTextChangedTime       = System.currentTimeMillis();
    private int                                      mCurrentScrollState           = 0;
    private Context                                  mContext                      = null;
    private ActivityManager                          mActivityManager              = null;

    public ViewRecorderSDK(Context context) {
        this.mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        init();
    }

    public class OutputEvent {
        final static int PRIORITY_DRAG              = 1;
        final static int PRIORITY_KEY               = 2;
        final static int PRIORITY_SCROLL            = 3;
        final static int PRIORITY_CLICK             = 4;

        final static int PRIORITY_WEBELEMENT_CLICK  = 10;
        final static int PRIORITY_WEBELEMENT_CHANGE = 10;

        /**
         * NOTICE: This field can not be null!
         */
        public View      view                       = null;
        public int       priority                   = 0;
        protected String code                       = "";
        protected String log                        = "";

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
            return String.format("[%s] %s", view, priority);
        }

        @Override
        public boolean equals(Object o) {
            if (null == o) {
                return false;
            }
            OutputEvent target = (OutputEvent) o;
            return this.view.equals(target.view) && this.priority == target.priority ? true : false;
        }

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
            if (getFamilyString(e1.view).length() > getFamilyString(e2.view).length()) {
                return 1;
            }
            return -1;
        }
    }

    private final static String CLASSNAME_DECORVIEW = "com.android.internal.policy.impl.PhoneWindow$DecorView";

    private String getFamilyString(View v) {
        View view = v;
        String familyString = "";
        while (view.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (null == parent) {
                printLog("null == parent at getFamilyString");
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

    private String rmTheLastChar(String str) {
        return str.length() == 0 ? str : str.substring(0, str.length() - 1);
    }

    private String getViewText(View view) {
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

    private void print(String tag, String message) {
        if ("RecorderCode".equals(tag)) {
            offerOutputLogQueue(message);
        }
        Log.i(tag, message);
    }

    private void printLog(String message) {
        print("ViewRecorder", message);
    }

    private void printCode(String message) {
        print("RecorderCode", message);
    }

    private void init() {
        setWindowManagerString();
        mPackageName = mContext.getPackageName();
        //((Activity)context).getWindowManager();
        initKeyTable();
    }

    /**
     * Add listeners on all views for generating cafe code automatically
     */
    public void beginRecordCode() {
        if (mBegin) {
            printLog("ViewRecorderSDK has already begin!");
            return;
        }
        mBegin = true;
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
        System.out.println("ViewRecorder is ready to work.");
        handleRecordMotionEventQueue();
        handleOutputEventQueue();

        mLastEventTime = System.currentTimeMillis();
        printLog("ViewRecorder is ready to work.");
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {
        }
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
        // Activity activity = local.getCurrentActivity();
        setOnTouchListenerOnDecorView();
    }

    private static Class<?> windowManager;
    static {
        try {
            String windowManagerClassName;
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                windowManagerClassName = "android.view.WindowManagerGlobal";
            } else {
                windowManagerClassName = "android.view.WindowManagerImpl";
            }
            windowManager = Class.forName(windowManagerClassName);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private String          windowManagerString;

    /**
     * Sets the window manager string.
     */
    private void setWindowManagerString() {

        if (android.os.Build.VERSION.SDK_INT >= 17) {
            windowManagerString = "sDefaultWindowManager";

        } else if (android.os.Build.VERSION.SDK_INT >= 13) {
            windowManagerString = "sWindowManager";

        } else {
            windowManagerString = "mWindowManager";
        }
    }

    private View[] getWindowDecorViews() {
        Field viewsField;
        Field instanceField;
        try {
            viewsField = windowManager.getDeclaredField("mViews");
            instanceField = windowManager.getDeclaredField(windowManagerString);
            viewsField.setAccessible(true);
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            return (View[]) viewsField.get(instance);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<View> getViews() {
        try {
            return getViews(null, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extracts all {@code View}s located in the currently active
     * {@code Activity}, recursively.
     * 
     * @param parent
     *            the {@code View} whose children should be returned, or
     *            {@code null} for all
     * @param onlySufficientlyVisible
     *            if only sufficiently visible views should be returned
     * @return all {@code View}s located in the currently active
     *         {@code Activity}, never {@code null}
     */

    private ArrayList<View> getViews(View parent, boolean onlySufficientlyVisible) {
        final ArrayList<View> views = new ArrayList<View>();
        final View parentToUse;

        if (parent == null) {
            return getAllViews(onlySufficientlyVisible);
        } else {
            parentToUse = parent;

            views.add(parentToUse);

            if (parentToUse instanceof ViewGroup) {
                addChildren(views, (ViewGroup) parentToUse, onlySufficientlyVisible);
            }
        }
        return views;
    }

    /**
     * Adds all children of {@code viewGroup} (recursively) into {@code views}.
     * 
     * @param views
     *            an {@code ArrayList} of {@code View}s
     * @param viewGroup
     *            the {@code ViewGroup} to extract children from
     * @param onlySufficientlyVisible
     *            if only sufficiently visible views should be returned
     */

    private void addChildren(ArrayList<View> views, ViewGroup viewGroup,
            boolean onlySufficientlyVisible) {
        if (viewGroup != null) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                final View child = viewGroup.getChildAt(i);

                if (onlySufficientlyVisible && isViewSufficientlyShown(child))
                    views.add(child);

                else if (!onlySufficientlyVisible)
                    views.add(child);

                if (child instanceof ViewGroup) {
                    addChildren(views, (ViewGroup) child, onlySufficientlyVisible);
                }
            }
        }
    }

    /**
     * Returns true if the view is sufficiently shown
     * 
     * @param view
     *            the view to check
     * @return true if the view is sufficiently shown
     */

    private final boolean isViewSufficientlyShown(View view) {
        final int[] xyView = new int[2];
        final int[] xyParent = new int[2];

        if (view == null)
            return false;

        final float viewHeight = view.getHeight();
        final View parent = getScrollOrListParent(view, 1);
        view.getLocationOnScreen(xyView);

        if (parent == null) {
            xyParent[1] = 0;
        } else {
            parent.getLocationOnScreen(xyParent);
        }

        if (xyView[1] + (viewHeight / 2.0f) > getScrollListWindowHeight(view))
            return false;

        else if (xyView[1] + (viewHeight / 2.0f) < xyParent[1])
            return false;

        return true;
    }

    /**
     * Returns the height of the scroll or list view parent
     * 
     * @param view
     *            the view who's parents height should be returned
     * @return the height of the scroll or list view parent
     */
    @SuppressWarnings("deprecation")
    private float getScrollListWindowHeight(View view) {
        final int[] xyParent = new int[2];
        View parent = getScrollOrListParent(view, 1);
        final float windowHeight;
        if (parent == null) {
            windowHeight = ((Activity) mContext).getWindowManager().getDefaultDisplay().getHeight();
        } else {
            parent.getLocationOnScreen(xyParent);
            windowHeight = xyParent[1] + parent.getHeight();
        }
        parent = null;
        return windowHeight;
    }

    /**
     * Returns the scroll or list parent view
     * 
     * @param view
     *            the view who's parent should be returned
     * @return the parent scroll view, list view or null
     */

    private View getScrollOrListParent(View view, int depth) {
        depth++;
        if (!(view instanceof android.widget.AbsListView)
                && !(view instanceof android.widget.ScrollView) && !(view instanceof WebView)) {
            try {
                return getScrollOrListParent((View) view.getParent(), depth);
            } catch (Exception e) {
                return null;
            }
        } else {
            return view;
        }
    }

    private final View[] getNonDecorViews(View[] views) {
        View[] decorViews = null;

        if (views != null) {
            decorViews = new View[views.length];

            int i = 0;
            View view;

            for (int j = 0; j < views.length; j++) {
                view = views[j];
                if (view != null
                        && !(view.getClass().getName()
                                .equals("com.android.internal.policy.impl.PhoneWindow$DecorView"))) {
                    decorViews[i] = view;
                    i++;
                }
            }
        }
        return decorViews;
    }

    private ArrayList<View> getAllViews(boolean onlySufficientlyVisible) {
        final View[] views = getWindowDecorViews();
        final ArrayList<View> allViews = new ArrayList<View>();
        final View[] nonDecorViews = getNonDecorViews(views);
        View view = null;

        if (nonDecorViews != null) {
            for (int i = 0; i < nonDecorViews.length; i++) {
                view = nonDecorViews[i];
                try {
                    addChildren(allViews, (ViewGroup) view, onlySufficientlyVisible);
                } catch (Exception ignored) {
                }
                if (view != null)
                    allViews.add(view);
            }
        }

        if (views != null && views.length > 0) {
            view = getRecentDecorView(views);
            try {
                addChildren(allViews, (ViewGroup) view, onlySufficientlyVisible);
            } catch (Exception ignored) {
            }

            if (view != null)
                allViews.add(view);
        }

        return allViews;
    }

    /**
     * Returns the most recent DecorView
     * 
     * @param views
     *            the views to check
     * @return the most recent DecorView
     */

    private final View getRecentDecorView(View[] views) {
        final View[] decorViews = new View[views.length];
        int i = 0;
        View view;

        for (int j = 0; j < views.length; j++) {
            view = views[j];
            if (view != null
                    && view.getClass().getName()
                            .equals("com.android.internal.policy.impl.PhoneWindow$DecorView")) {
                decorViews[i] = view;
                i++;
            }
        }
        return getRecentContainer(decorViews);
    }

    /**
     * Returns the most recent view container
     * 
     * @param views
     *            the views to check
     * @return the most recent view container
     */

    private final View getRecentContainer(View[] views) {
        View container = null;
        long drawingTime = 0;
        View view;

        for (int i = 0; i < views.length; i++) {
            view = views[i];
            if (view != null && view.isShown() && view.hasWindowFocus()
                    && view.getDrawingTime() > drawingTime) {
                container = view;
                drawingTime = view.getDrawingTime();
            }
        }
        return container;
    }

    /**
     * If there is no views to handle onTouch event, decorView will handle it
     * and invoke activity.onTouchEvent(event).If decorView does not handle a
     * touch event by return true, events follow-up will not be dispatched to
     * views including decorView.
     */
    private void setOnTouchListenerOnDecorView() {
        View[] views = getWindowDecorViews();
        if (views != null) {
            for (View view : views) {
                handleOnTouchListener(view);
            }
        } else {
            printLog("setOnTouchListenerOnDecorView NULL pointer.");
        }
        // View decorView = activity.getWindow().getDecorView();
    }

    private float getDisplayX() {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
        //mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    private float getDisplayY() {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    private boolean isInScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int leftX = location[0];
        int righX = location[0] + view.getWidth();
        int leftY = location[1];
        int righY = location[1] + view.getHeight();
        return righX < 0 || leftX > getDisplayX() || righY < 0 || leftY > getDisplayY() ? false
                : true;
    }

    private boolean isSize0(final View view) {
        return view.getHeight() == 0 || view.getWidth() == 0;
    }

    private <T extends View> ArrayList<T> removeInvisibleViews(ArrayList<T> viewList) {
        ArrayList<T> tmpViewList = new ArrayList<T>(viewList.size());
        for (T view : viewList) {
            if (view != null && view.isShown() && isInScreen(view) && !isSize0(view)) {
                tmpViewList.add(view);
            }
        }
        return tmpViewList;
    }

    private ArrayList<View> getTargetViews() {
        ArrayList<View> views = removeInvisibleViews(getViews(null, false));
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

        return targetViews;
    }

    private void saveView(View view) {
        if (null == view) {
            printLog("null == view ");
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

    private View getFocusView(ArrayList<View> views) {
        for (View view : views) {
            if (view.isFocused()) {
                return view;
            }
        }
        return null;
    }

    private View getCurrentFocusView() {
        ArrayList<View> views = getViews();
        return getFocusView(views);
    }

    private View getRecentDecorView() {
        View[] views = getWindowDecorViews();

        if (null == views || 0 == views.length) {
            printLog("0 == views.length at getRecentDecorView");
            return null;
        }

        View recentDecorview = getRecentDecorView(views);
        if (null == recentDecorview) {
            // print("null == rview; use views[0]: " + views[0]);
            recentDecorview = views[0];
        }
        return recentDecorview;
    }

    private void setDefaultFocusView() {
        // It's too slow..
        // if (local.getCurrentActivity().getCurrentFocus() != null) {
        // return;
        // }
        if (getCurrentFocusView() != null) {
            return;
        }
        View view = getRecentDecorView();
        if (null == view) {
            printLog("null == view of setDefaultFocusView");
            return;
        }
        // boolean hasFocus = local.requestFocus(view);
        // printLog(view + " hasFocus: " + hasFocus);
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
    private Object getListener(View view, Class<?> targetClass, String fieldName) {
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
     * find parent until parent is father or java.lang.Object(to the end)
     * 
     * @param view
     *            target view
     * @param father
     *            target father
     * @return positive means level from father; -1 means not found
     */
    private int countLevelFromViewToFather(View view, Class<?> father) {
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

    private Object getListener(View view, String listenerName) {
        return getListener(view, getClassByListenerName(listenerName), listenerName);
    }

    private void setListener(View view, String listenerName, Object value) {
        setListener(view, getClassByListenerName(listenerName), listenerName, value);
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
    private void setListener(View view, Class<?> targetClass, String fieldName, Object value) {
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
     * These try-catch can not be merged. We need try to hook listeners as many
     * as possible.
     */
    private void setHookListenerOnView(View view) {
        // for thread safe
        if (null == view) {
            return;
        }

        /*
         * if (view instanceof WebView && DEBUG_WEBVIEW) { new
         * WebElementRecorder(this).handleWebView((WebView) view); }
         */

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
            // view.isLongClickable()
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
            // handleOnClickListener can not replace handleOnTouchListener
            // because reason below.
            // There are some views which have click listener and touch listener
            // but only use touch listener.
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
            // printLog("firstVisibleItem:" + firstVisibleItem);
            // printLog("visibleItemCount:" + visibleItemCount);
            // printLog("totalItemCount:" + totalItemCount);
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
        scrollEvent.setCode(getPrefix(view) + "|scroll");
        scrollEvent.setLog("scroll " + view + " to " + absListViewState.firstVisibleItem);
        offerOutputEventQueue(scrollEvent);
    }

    private void hookOnScrollListener(final AbsListView absListView,
            final OnScrollListener onScrollListener) {
        printLog("hook onScrollListener [" + absListView + "] [" + onScrollListener.hashCode()
                + "]");

        // save old listener
        mOnScrollListeners.put(getViewID(absListView), onScrollListener);
        // mOnScrollListeners.put(String.valueOf(absListView.hashCode()),
        // onScrollListener);

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
                    printLog("onScrollListener == null ");
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
                    printLog("onScrollListener == null ");
                }
            }
        };

        absListView.post(new Runnable() {
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
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format(getPrefix(parent) + "|click");
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
        ClickEvent clickEvent = new ClickEvent(parent);
        String code = String.format(getPrefix(parent) + "|click");
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

    private void hookOnClickListener(final View view, final OnClickListener onClickListener) {
        // printLog(String.format("hookClickListener [%s(%s)]", view, local.getViewText(view)));

        // save old listener
        OnClickListener originListener = onClickListener;
        //should use originListener = kryo.copy(onClickListener);
        mOnClickListeners.put(getViewID(view), originListener);

        view.post(new Runnable() {

            @Override
            public void run() {
                // init counter
                mOnClickListenerInvokeCounter.put(onClickListener, 0);

                // set hook listener
                OnClickListener onClickListenerHooked = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean shouldInvokeOrigin = false;
                        int counter = mOnClickListenerInvokeCounter.get(onClickListener);
                        mOnClickListenerInvokeCounter.put(onClickListener, ++counter);
                        if (counter < 2) {
                            setOnClick(v);
                        } else {
                            printLog("recover onClickListener counter:" + counter);
                            setListener(view, "mOnClickListener", onClickListener);
                            shouldInvokeOrigin = true;
                        }
                        if (shouldInvokeOrigin) {
                            onClickListener.onClick(view);
                        }

                        // reset counter
                        mOnClickListenerInvokeCounter.put(onClickListener, 0);
                    }
                };

                OnClickListener originOnClickListener = mOnClickListeners.get(getViewID(view));
                if (onClickListenerHooked.equals(originOnClickListener)) {
                    printLog("#########onClickListenerHooked.equals(originOnClickListener):"
                            + onClickListenerHooked);
                } else {
                    setListener(view, "mOnClickListener", onClickListenerHooked);
                }
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
        if (isSize0(v)) {
            printLog(v + " is size 0 ");
            invokeOriginOnClickListener(v);
            return;
        }

        // set click event output
        ClickEvent clickEvent = new ClickEvent(v);
        clickEvent.setCode(getPrefix(v) + "|click");

        offerOutputEventQueue(clickEvent);
        invokeOriginOnClickListener(v);
    }

    private void invokeOriginOnClickListener(View v) {
        OnClickListener onClickListener = mOnClickListeners.get(getViewID(v));
        OnClickListener onClickListenerHooked = (OnClickListener) getListener(v, "mOnClickListener");

        if (onClickListener != null) {
            if (onClickListener.equals(onClickListenerHooked)) {
                printLog("onClickListener == onClickListenerHooked!!!");
                return;
            }
            onClickListener.onClick(v);
        } else {
            printLog("onClickListener == null");
        }
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

            String rString = mContext.getResources().getResourceName(view.getId());
            return rString.substring(rString.lastIndexOf("/") + 1, rString.length());
        } catch (Exception e) {
            // eat it because some view has no res id
        }
        return "";
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
                if ("".equals(s.toString()) || text.equals(lastText) || !editText.isShown()
                        || !editText.isFocused()) {
                    return;
                }
                printLog("onTextChanged: " + text + " getVisibility:" + editText + " "
                        + editText.getVisibility());
                mTheLastTextChangedTime = System.currentTimeMillis();
                mCurrentEditTextIndex = getCurrentViewIndex(editText);
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

    /**
     * get view index by its class at current activity
     * 
     * @param view
     * @return -1 means not found;otherwise is then index of view
     */
    private int getCurrentViewIndex(View view) {
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

    /**
     * Returns an {@code ArrayList} of {@code View}s of the specified
     * {@code Class} located in the current {@code Activity}.
     * 
     * @param classToFilterBy
     *            return all instances of this class, e.g. {@code Button.class}
     *            or {@code GridView.class}
     * @return an {@code ArrayList} of {@code View}s of the specified
     *         {@code Class} located in the current {@code Activity}
     */

    private <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy) {
        return getCurrentViews(classToFilterBy, null);
    }

    /**
     * Returns an {@code ArrayList} of {@code View}s of the specified
     * {@code Class} located under the specified {@code parent}.
     * 
     * @param classToFilterBy
     *            return all instances of this class, e.g. {@code Button.class}
     *            or {@code GridView.class}
     * @param parent
     *            the parent {@code View} for where to start the traversal
     * @return an {@code ArrayList} of {@code View}s of the specified
     *         {@code Class} located under the specified {@code parent}
     */

    private <T extends View> ArrayList<T> getCurrentViews(Class<T> classToFilterBy, View parent) {
        ArrayList<T> filteredViews = new ArrayList<T>();
        List<View> allViews = getViews(parent, true);
        for (View view : allViews) {
            if (view != null && classToFilterBy.isAssignableFrom(view.getClass())) {
                filteredViews.add(classToFilterBy.cast(view));
            }
        }
        allViews = null;
        return filteredViews;
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
            //	mOnClickListenerCounters.put(onTouchListener,0);
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

    private void hookOnTouchListener(View view, final OnTouchListener onTouchListener) {
        // printLog("hookOnTouchListener [" + view + "(" + local.getViewText(view) + ")]");

        // save old listener
        mOnTouchListeners.put(getViewID(view), onTouchListener);

        // init counter
        mOnTouchListenerInvokeCounter.put(onTouchListener, 0);

        // set hook listener
        OnTouchListener onTouchListenerHooked = new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean ret = false;
                boolean shouldInvokeOrigin = false;
                int counter = mOnTouchListenerInvokeCounter.get(onTouchListener);
                mOnTouchListenerInvokeCounter.put(onTouchListener, ++counter);
                if (counter < 2) {
                    OnTouchListener onTouchListenerHooked = (OnTouchListener) getListener(v,
                            "mOnTouchListener");
                    addEvent(v, event);
                    if (onTouchListener != null) {
                        if (onTouchListener.equals(onTouchListenerHooked)) {
                            printLog("onTouchListenerHooked == onTouchListener!!!");
                            return false;
                        }
                        ret = onTouchListener.onTouch(v, event);
                    } else {
                        printLog("onTouchListener == null");
                    }
                } else {
                    printLog("recover onTouchListener counter:" + counter);
                    setListener(v, "mOnTouchListener", onTouchListener);
                    shouldInvokeOrigin = true;
                }
                if (shouldInvokeOrigin) {
                    ret = onTouchListener.onTouch(v, event);
                }

                // reset counter
                mOnTouchListenerInvokeCounter.put(onTouchListener, 0);

                return ret;
            }
        };
        setListener(view, "mOnTouchListener", onTouchListenerHooked);
    }

    private void addEvent(View v, MotionEvent event) {
        // printLog(v + " " + event);
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
            printLog("hook AdapterView [" + adapterView + "] [" + onItemClickListener.hashCode()
                    + "]" + mAllListenerHashcodes.contains(onItemClickListener.hashCode()));
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
            printLog("save onItemClickListenerHooked " + onItemClickListenerHooked.hashCode());
            printLog("mAllListenerHashcodes.contains "
                    + mAllListenerHashcodes.contains(onItemClickListenerHooked.hashCode()));
            printLog("mAllListenerHashcodes.size():" + mAllListenerHashcodes.size());
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
        ClickEvent clickEvent = new ClickEvent(parent);
        clickEvent.setCode(getPrefix(parent) + "|click");
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
            // parent.performItemClick(view, position, id);
        }
    }

    private void handleOnItemLongClickListener(AdapterView<?> view) {
        // if (local.isSize0(view)) {
        // printLog(view + " is size 0 at handleOnItemLongClickListener");
        // return;
        // }

        OnItemLongClickListener onItemLongClickListener = (OnItemLongClickListener) getListener(
                view, "mOnItemLongClickListener");

        // has hooked listener
        if (onItemLongClickListener != null
                && mAllListenerHashcodes.contains(onItemLongClickListener.hashCode())) {
            return;
        }

        if (null != onItemLongClickListener) {
            printLog("hookOnItemLongClickListener [" + view + "(" + getViewText(view) + ")]");

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
        if (isSize0(view)) {
            printLog(view + " is size 0 ");
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
            printLog("hookOnLongClickListener [" + view + "(" + getViewText(view) + ")]");

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
        clickEvent.setCode(getPrefix(v) + "|longclick");

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

    private boolean offerOutputEventQueue(OutputEvent e) {
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

    /**
     * poll a output log from mOutputLogQueue
     * 
     * synchronized by mSyncOutputLogQueue
     * 
     * @return a line of output log
     */
    public String pollOutputLogQueue() {
        synchronized (mSyncOutputLogQueue) {
            return mOutputLogQueue.poll();
        }
    }

    private boolean offerOutputLogQueue(String line) {
        synchronized (mSyncOutputLogQueue) {
            return mOutputLogQueue.offer(line);
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
                    // printLog("event.proity > nextEvent.proity");
                    newEvents.add(event);
                } else if (event.priority < nextEvent.priority) {
                    // printLog("event.proity < nextEvent.proity");
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
        String familyString1 = getFamilyString(v1);
        String familyString2 = getFamilyString(v2);
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
                printCode(event.getCode());
            }
            printLog(event.getLog());
        } else {
            printCode(event.getCode());
            printLog(event.getLog());
            outputEditTextEvent();
        }
    }

    private String getPrefix(View view) {
        String viewId = "";
        try {
            viewId = getRString(view);
            viewId = viewId.equals("") ? getViewText(view) : viewId;
            // if view has no id and no text, viewId == ""
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("%s|%s|%s", getPrefix(), getViewString(view), viewId);
    }

    private String getPrefix() {
        String time = "";
        String activity = "";
        try {
            time = mSimpleDateFormat.format(new Date());
            // need permission GET_TASK
            //activity = mActivityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format("%s", time);
    }

    private boolean outputEditTextEvent() {
        if ("".equals(mCurrentEditTextString) || mCurrentEditTextIndex < 0 || !mHasTextChange) {
            return false;
        }

        String code = String.format("local.enterText(%s, \"%s\", false);", mCurrentEditTextIndex,
                mCurrentEditTextString);
        printCode(code);

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
                            if (MotionEvent.ACTION_UP == e.action
                                    || MotionEvent.ACTION_CANCEL == e.action) {
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
                                mFamilyStringBeforeScroll = getFamilyString(e.view);
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
                                // printLog("ignore a drag without up");
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
         * if (0 == duration) { printLog("ignore drag event of [" + up.view +
         * "] because 0 == duration"); printLog("x:" + up.x + " y:" + up.y);
         * return; }
         */

        dragEvent.setLog(String.format(
                "Drag [%s<%s>] from (%s,%s) to (%s, %s) by duration %s step %s", up.view,
                getFamilyString(up.view), down.x, down.y, up.x, up.y, duration, stepCount));
        dragEvent.setCode(getPrefix(up.view) + "|drag");

        if (up.view instanceof AbsListView || mIsLongClick
        /* || (up.view instanceof WebView && DEBUG_WEBVIEW) */) {
            printLog("ignore drag event of [" + up.view + "]");
            mIsLongClick = false;
            return;
        }

        // wait for other type event
        sleep(100);
        offerOutputEventQueue(dragEvent);
    }

    private float toPercentX(float x) {
        return x / getDisplayX();
    }

    private float toPercentY(float y) {
        return y / getDisplayY();
    }

    /**
     * This method will cost 100ms to judge whether scrollview stoped.
     * 
     * @param scrollView
     * @return true means scrolling is stop, otherwise return fasle
     */
    private boolean isScrollStoped(final ScrollView scrollView) {
        int x1 = scrollView.getScrollX();
        int y1 = scrollView.getScrollY();
        sleep(100);
        int x2 = scrollView.getScrollX();
        int y2 = scrollView.getScrollY();
        return x1 == x2 && y1 == y2 ? true : false;
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
                while (!isScrollStoped(scrollView)) {
                    // wait for scroll stoping
                }
                if ("".equals(mFamilyStringBeforeScroll)) {
                    printLog("mFamilyStringBeforeScroll is \"\"");
                    return;
                }
                int scrollX = scrollView.getScrollX();
                int scrollY = scrollView.getScrollY();
                String drag = String.format(
                        "local.recordReplay.scrollScrollViewTo(\"%s\", %s, %s);",
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
            hardKeyEvent.setCode(getPrefix() + "|key|" + mKeyCodeMap.get(keyCode));
            hardKeyEvent.setLog("view: " + view + " " + event);

            offerOutputEventQueue(hardKeyEvent);
        }
    }

    /**
     * for view.getId() == -1
     */
    private String getViewID(View view) {
        if (null == view) {
            printLog("null == view ");
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
        ArrayList<String> names = ReflectHelper.getFieldNameByType(keyEvent, null, int.class);
        try {
            for (String name : names) {
                if (name.startsWith("KEYCODE_")) {
                    Integer keyCode = (Integer) ReflectHelper.getField(keyEvent, null, name);
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
        return null == view.getParent() ? false : true;
    }
}
