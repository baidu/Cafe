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

package com.baidu.cafe.record;

import java.io.BufferedWriter;
import java.io.File;
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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-8
 * @version
 * @todo
 */
public class ViewRecorder {
    private HashMap<String, OnClickListener>         mOnClickListeners         = new HashMap<String, OnClickListener>();
    private HashMap<String, OnLongClickListener>     mOnLongClickListeners     = new HashMap<String, OnLongClickListener>();
    private HashMap<String, OnTouchListener>         mOnTouchListeners         = new HashMap<String, OnTouchListener>();
    private HashMap<String, OnKeyListener>           mOnKeyListeners           = new HashMap<String, OnKeyListener>();
    private HashMap<String, OnItemClickListener>     mOnItemClickListeners     = new HashMap<String, OnItemClickListener>();
    private HashMap<String, OnItemLongClickListener> mOnItemLongClickListeners = new HashMap<String, OnItemLongClickListener>();
    private HashMap<String, OnItemSelectedListener>  mOnItemSelectedListeners  = new HashMap<String, OnItemSelectedListener>();
    private ArrayList<String>                        mAllViews                 = new ArrayList<String>();
    private ArrayList<Integer>                       mAllListenerHashcodes     = new ArrayList<Integer>();
    private ArrayList<EditText>                      mAllEditTexts             = new ArrayList<EditText>();

    /**
     * For merge a sequeue of MotionEvents to a drag
     */
    private Queue<RecordMotionEvent>                 mMotionEventQueue         = new LinkedList<RecordMotionEvent>();

    /**
     * For judging events of the same view at the same time which should be
     * keeped by their priorities.
     */
    private Queue<OutputEvent>                       mOutputEventQueue         = new LinkedList<OutputEvent>();
    private LocalLib                                 local                     = null;
    private File                                     mRecord                   = null;

    public ViewRecorder(LocalLib local) {
        this.local = local;
        init();
        printLog("ViewRecorder is ready to work.");
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

    class SortByView implements Comparator<OutputEvent> {
        public int compare(OutputEvent e1, OutputEvent e2) {
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

    private void printCode(String message) {
        print("CodeRecorder", message);
    }

    private void init() {
        String path = "/data/data/" + local.getCurrentActivity().getPackageName() + "/cafe";
        File cafe = new File(path);
        if (!cafe.exists()) {
            cafe.mkdir();
            local.executeOnDevice("chmod 777 " + path, "/");
        }
        mRecord = new File(path + "/record");
        if (mRecord.exists()) {
            mRecord.delete();
        }
    }

    /**
     * add listeners on all views for generating cafe code automatically
     */
    public void beginRecordCode() {
        // keep hooking new views
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    ArrayList<View> newViews = getTargetViews(local.getCurrentViews());
                    //                    print("newViews=" + newViews.size());
                    for (View view : newViews) {
                        try {
                            setHookListenerOnView(view);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        handleRecordMotionEventQueue();
        handleOutputEventQueue();
    }

    private ArrayList<View> getTargetViews(ArrayList<View> views) {
        ArrayList<View> targetViews = new ArrayList<View>();
        for (View view : views) {
            // get new views
            String viewID = getViewID(view);
            if (!mAllViews.contains(viewID)) {
                targetViews.add(view);
                mAllViews.add(viewID);
            } else {
                // get views who have unhooked listeners
                if (hasUnhookedListener(view)) {
                    targetViews.add(view);
                }
            }

        }
        return targetViews;
    }

    private boolean hasUnhookedListener(View view) {
        String[] listenerNames = new String[] { "mOnItemClickListener", "mOnClickListener",
                "mOnTouchListener" };
        for (String listenerName : listenerNames) {
            Object listener = local.getListener(view, listenerName);
            if (listener != null && !mAllListenerHashcodes.contains(listener.hashCode())) {
                //                print("has unhooked " + listenerName + ": " + view);
                return true;
            }
        }
        return false;
    }

    private boolean hasHookedListener(View view, String listenerName) {
        Object listener = local.getListener(view, listenerName);
        if (listener != null && mAllListenerHashcodes.contains(listener.hashCode())) {
            return true;
        }
        return false;
    }

    private void setHookListenerOnView(View view) {
        if (view instanceof AdapterView) {
            printLog("AdapterView [" + view + "]");
            hookOnItemClickListener((AdapterView) view);
            //            adapterView.setOnItemLongClickListener(listener);
            //            adapterView.setOnItemSelectedListener(listener);
            // MenuItem.OnMenuItemClickListener
        }

        if (view instanceof EditText) {
            hookEditText((EditText) view);
            return;
        }

        if (!hookOnClickListener(view)) {
            // If view has ClickListener, do not add a TouchListener.
            hookOnTouchListener(view);
        }

        /*
                mOnLongClickListener = (OnLongClickListener) local.getListener(view, "mOnLongClickListener");
                if (null != mOnLongClickListener) {
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            print("id:" + v.getId() + "\t long_click");
                            mOnLongClickListener.onLongClick(v);
                            return false;
                        }
                    });
                }

        */
    }

    private void hookEditText(EditText editText) {
        if (mAllEditTexts.contains(editText)) {
            return;
        }

        // all TextWatcher works at the same time
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                printLog("text:" + s);
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

    private boolean hookOnClickListener(View view) {
        if (hasHookedListener(view, "mOnClickListener")) {
            return true;
        }
        OnClickListener onClickListener = (OnClickListener) local.getListener(view,
                "mOnClickListener");
        if (null != onClickListener) {
            printLog(String.format("hookClickListener [%s(%s), %s]", view, local.getViewText(view)));

            // save old listener
            mOnClickListeners.put(getViewID(view), onClickListener);

            // set hook listener
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClickEvent clickEvent = new ClickEvent(v);
                    clickEvent.setCode(String.format("local.clickOn(%s, %s);",
                            getViewClassString(v), local.getCurrentViewIndex(v)));
                    clickEvent.setLog(String.format("Click On View [%s(%s)] ", v,
                            local.getViewText(v)));
                    mOutputEventQueue.offer(clickEvent);

                    OnClickListener onClickListener = mOnClickListeners.get(getViewID(v));
                    if (onClickListener != null) {
                        onClickListener.onClick(v);
                    } else {
                        printLog("onClickListener == null");
                    }
                }
            });

            // save hashcode of hooked listener
            OnClickListener onClickListenerHooked = (OnClickListener) local.getListener(view,
                    "mOnClickListener");
            if (onClickListenerHooked != null) {
                mAllListenerHashcodes.add(onClickListenerHooked.hashCode());
            }
            return true;
        }
        return false;
    }

    private void hookOnTouchListener(View view) {
        if (hasHookedListener(view, "mOnTouchListener")) {
            return;
        }
        OnTouchListener onTouchListener = (OnTouchListener) local.getListener(view,
                "mOnTouchListener");
        //        print("hookOnTouchListener [" + view + "(" + local.getViewText(view) + ")]"
        //                + (view instanceof ViewGroup ? "ViewGroup" : "View"));
        if (null != onTouchListener) {

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
        OnTouchListener onTouchListenerHooked = (OnTouchListener) local.getListener(view,
                "mOnTouchListener");
        if (onTouchListenerHooked != null) {
            mAllListenerHashcodes.add(onTouchListenerHooked.hashCode());
        }
    }

    private void addEvent(View v, MotionEvent event) {
        if (!mMotionEventQueue.offer(new RecordMotionEvent(v, event.getAction(), event.getRawX(),
                event.getRawY()))) {
            printLog("Add to mMotionEventQueue Failed! view:" + v + "\t" + event.toString()
                    + "mMotionEventQueue.size=" + mMotionEventQueue.size());
        }
    }

    private void hookOnItemClickListener(AdapterView view) {
        if (hasHookedListener(view, "mOnItemClickListener")) {
            return;
        }
        OnItemClickListener onItemClickListener = (OnItemClickListener) local.getListener(view,
                "mOnItemClickListener");

        if (null != onItemClickListener) {
            printLog("hook AdapterView [" + view + "]");

            // save old listener
            mOnItemClickListeners.put(getViewID(view), onItemClickListener);

            // set hook listener
            view.setOnItemClickListener(new OnItemClickListener() {

                /**
                 * Callback method to be invoked when an item in this
                 * AdapterView has been clicked.
                 * <p>
                 * Implementers can call getItemAtPosition(position) if they
                 * need to access the data associated with the selected item.
                 * 
                 * @param parent
                 *            The AdapterView where the click happened.
                 * @param view
                 *            The view within the AdapterView that was clicked
                 *            (this will be a view provided by the adapter)
                 * @param position
                 *            The position of the view in the adapter.
                 * @param id
                 *            The row id of the item that was clicked.
                 */
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ClickEvent clickEvent = new ClickEvent(parent);
                    clickEvent.setCode(String.format("local.clickInList(%s, %s, false);", position,
                            local.getCurrentViewIndex(parent)));
                    clickEvent.setLog("parent: " + parent + " view: " + view + " position: "
                            + position + " click ");
                    mOutputEventQueue.offer(clickEvent);

                    OnItemClickListener onItemClickListener = mOnItemClickListeners
                            .get(getViewID(parent));
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(parent, view, position, id);
                    } else {
                        printLog("onItemClickListener == null");
                    }
                }
            });

            // save hashcode of hooked listener
            OnItemClickListener onItemClickListenerHooked = (OnItemClickListener) local
                    .getListener(view, "mOnItemClickListener");
            if (onItemClickListenerHooked != null) {
                mAllListenerHashcodes.add(onItemClickListenerHooked.hashCode());
            }
        } else {
            printLog("onItemClickListener == null at [" + view + "]");
        }
    }

    private void hookOnLongClickListener(View view) {
        OnLongClickListener onLongClickListener = (OnLongClickListener) local.getListener(view,
                "mOnLongClickListener");
        if (null != onLongClickListener) {
            printLog("hookOnLongClickListener [" + view + "(" + local.getViewText(view) + ")]");
            mOnLongClickListeners.put(getViewID(view), onLongClickListener);
            view.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {

                    return false;
                }
            });
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception exception) {
            exception.printStackTrace();
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
                        sleep(200);

                        // get all event
                        while ((e = mOutputEventQueue.poll()) != null) {
                            events.add(e);
                        }

                        // sort by view.hashCode()
                        Collections.sort(events, new SortByView());

                        outputEvents(events);

                        events.clear();
                    } else {
                        sleep(50);
                    }
                }
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
                    if (event.view.equals(nextEvent.view)) {
                        i += 2;
                        if (event.proity > nextEvent.proity) {
                            //                            printLog("event.proity > nextEvent.proity");
                            outputAnEvent(event);
                        } else if (event.proity < nextEvent.proity) {
                            //                            printLog("event.proity < nextEvent.proity");
                            outputAnEvent(nextEvent);
                        } else {
                            //                            printLog("event.proity == nextEvent.proity");
                        }
                    } else {
                        i++;
                        outputAnEvent(event);
                    }
                }
            }
        }).start();
    }

    private void outputAnEvent(OutputEvent event) {
        printLog("[CODE] " + event.getCode());
        printLog(event.getLog());
    }

    private int getIndexByView(ArrayList<OutputEvent> events, View view) {
        boolean isSelf = true;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).view.equals(view)) {
                if (!isSelf) {
                    return i;
                }
                isSelf = false;
            }
        }
        return -1;
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
                        //                        print("" + e);
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
                            if (recordMotionEvent.view.equals(targetView)) {
                                aTouch.add(recordMotionEvent);
                            }
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
     * Merge events from ACTION_DOWN to ACTION_UP.
     * 
     * @param events
     */
    private void mergeMotionEvents(ArrayList<RecordMotionEvent> events) {
        RecordMotionEvent down = events.get(0);
        RecordMotionEvent up = events.get(events.size() - 1);
        int stepCount = events.size() - 2;
        DragEvent dragEvent = new DragEvent(up.view);

        String code = "local.drag(local.toScreenX(%s), local.toScreenX(%s), local.toScreenY(%s), local.toScreenY(%s), %s);";
        dragEvent.setCode(String.format(code, local.toPercentX(down.x), local.toPercentX(up.x),
                local.toPercentY(down.y), local.toPercentY(up.y), stepCount));

        dragEvent.setLog(String.format("Drag [%s] from (%s,%s) to (%s, %s) by step count %s",
                down.view, down.x, down.y, up.x, up.y, stepCount));

        mOutputEventQueue.offer(dragEvent);
    }

    private void hookOnItemSelectedListener(AdapterView view) {

    }

    /**
     * for KeyEvent
     * 
     * @param editText
     */
    private void hookOnKeyListener(EditText editText) {

        OnKeyListener onKeyListener = (OnKeyListener) local.getListener(editText, "mOnKeyListener");
        if (null != onKeyListener) {
            printLog("hookOnKeyListener [" + editText + "]");
            mOnKeyListeners.put(getViewID(editText), onKeyListener);
            editText.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    OnKeyListener onKeyListener = mOnKeyListeners.get(getViewID(v));
                    printLog(event + " on " + v);
                    if (null != onKeyListener) {
                        onKeyListener.onKey(v, keyCode, event);
                    } else {
                        printLog("onKeyListener == null");
                    }
                    return false;
                }
            });
        } else {
            editText.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    printLog(event + " new on " + v);
                    return false;
                }
            });
        }
    }

    private String getViewID(View view) {
        String viewString = view.toString();
        return viewString.substring(viewString.indexOf("@"));
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

    private String getViewClassString(View view) {
        return view.getClass().toString().split(" ")[1] + ".class";
    }
}
