package com.baidu.cafe.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

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
    private Queue<RecordMotionEvent>                 mMotionEventQueue         = new LinkedList<RecordMotionEvent>();
    private LocalLib                                 local                     = null;
    private File                                     mRecord                   = null;

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

    public ViewRecorder(LocalLib local) {
        this.local = local;
        init();
    }

    private void print(String message) {
        if (Log.IS_DEBUG) {
            Log.i("ViewRecorder", message);
        }
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
                    ArrayList<View> newViews = getNewViews(local.getCurrentViews());
                    //                    print("newViews=" + newViews.size());
                    for (View view : newViews) {
                        try {
                            setAutoGenerateCodeListenerOnView(view);
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
    }

    private ArrayList<View> getNewViews(ArrayList<View> views) {
        ArrayList<View> newViews = new ArrayList<View>();
        for (View view : views) {
            String viewID = getViewID(view);
            if (!mAllViews.contains(viewID)) {
                newViews.add(view);
                mAllViews.add(viewID);
            }
        }
        return newViews;
    }

    private void setAutoGenerateCodeListenerOnView(View view) {
        if (view instanceof AdapterView) {
            print("AdapterView [" + view + "]");
            hookOnItemClickListener((AdapterView) view);
            //            adapterView.setOnItemLongClickListener(listener);
            //            adapterView.setOnItemSelectedListener(listener);
            // MenuItem.OnMenuItemClickListener
        }

        if (view instanceof EditText) {
            print("EditText [" + view + "]");
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
        print("hookEditText [" + editText + "]");
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                print("text:" + s);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    /**
     * for KeyEvent
     * 
     * @param editText
     */
    private void hookOnKeyListener(EditText editText) {

        OnKeyListener onKeyListener = (OnKeyListener) local.getListener(editText, "mOnKeyListener");
        if (null != onKeyListener) {
            print("hookOnKeyListener [" + editText + "]");
            mOnKeyListeners.put(getViewID(editText), onKeyListener);
            editText.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    OnKeyListener onKeyListener = mOnKeyListeners.get(getViewID(v));
                    print(event + " on " + v);
                    if (null != onKeyListener) {
                        onKeyListener.onKey(v, keyCode, event);
                    } else {
                        print("onKeyListener == null");
                    }
                    return false;
                }
            });
        } else {
            editText.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    print(event + " new on " + v);
                    return false;
                }
            });
        }
    }

    private boolean hookOnClickListener(View view) {
        OnClickListener onClickListener = (OnClickListener) local.getListener(view,
                "mOnClickListener");
        if (null != onClickListener) {
            print("hookClickListener [" + view + "(" + local.getViewText(view) + ")]");
            mOnClickListeners.put(getViewID(view), onClickListener);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String code = "//view.text=" + local.getViewText(v) + "\n"
                            + "clickOnView(findViewById(new Integer(" + v.getId() + ")));";
                    print(code);
                    writeToFile(String.format("local.clickOn(viewClass, index);", v.getClass(),
                            local.getCurrentViewIndex(v)));

                    OnClickListener onClickListener = mOnClickListeners.get(getViewID(v));
                    if (onClickListener != null) {
                        onClickListener.onClick(v);
                    } else {
                        print("onClickListener == null");
                    }
                }
            });
            return true;
        }
        return false;
    }

    private void hookOnLongClickListener(View view) {
        OnLongClickListener onLongClickListener = (OnLongClickListener) local.getListener(view,
                "mOnLongClickListener");
        if (null != onLongClickListener) {
            print("hookOnLongClickListener [" + view + "(" + local.getViewText(view) + ")]");
            mOnLongClickListeners.put(getViewID(view), onLongClickListener);
            view.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {

                    return false;
                }
            });
        }
    }

    private void hookOnTouchListener(View view) {
        OnTouchListener onTouchListener = (OnTouchListener) local.getListener(view,
                "mOnTouchListener");
        //        print("hookOnTouchListener [" + view + "(" + local.getViewText(view) + ")]"
        //                + (view instanceof ViewGroup ? "ViewGroup" : "View"));
        if (null != onTouchListener) {
            mOnTouchListeners.put(getViewID(view), onTouchListener);
            view.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    OnTouchListener onTouchListener = mOnTouchListeners.get(getViewID(v));
                    addEvent(v, event);

                    if (onTouchListener != null) {
                        onTouchListener.onTouch(v, event);
                    } else {
                        print("onTouchListener == null");
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
    }

    private void addEvent(View v, MotionEvent event) {
        if (!mMotionEventQueue.offer(new RecordMotionEvent(v, event.getAction(), event.getRawX(),
                event.getRawY()))) {
            print("Add to mMotionEventQueue Failed! view:" + v + "\t" + event.toString()
                    + "mMotionEventQueue.size=" + mMotionEventQueue.size());
        }
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
                        print("" + e);
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
                            print("events:" + recordMotionEvent);
                            if (recordMotionEvent.view.equals(targetView)) {
                                aTouch.add(recordMotionEvent);
                            }
                        }

                        mergeMotionEvents(aTouch);
                        events.clear();
                    }

                    try {
                        Thread.sleep(50);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
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
        print("down:" + down);
        print("up:" + up);
        // drag from (fromX,fromY) to (toX, toY) by step count 
        float fromX = down.x;
        float fromY = down.y;
        float toX = up.x;
        float toY = up.y;
        print(String.format("Drag from (%s,%s) to (%s, %s) by step count %s", fromX, fromY, toX,
                toY, stepCount));
        writeToFile(String.format("local.drag(%s, %s, %s, %s, %s);", fromX, toX, fromY, toY,
                stepCount));
    }

    private void hookOnItemClickListener(AdapterView view) {
        OnItemClickListener onItemClickListener = (OnItemClickListener) local.getListener(view,
                "mOnItemClickListener");

        if (null != onItemClickListener) {
            print("hook AdapterView [" + view + "]");
            mOnItemClickListeners.put(getViewID(view), onItemClickListener);
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
                    print("parent: " + parent + "view: " + view + "\t position: " + position
                            + " click ");
                    writeToFile(String.format("local.clickInList(%s, %s, false);", position,
                            local.getCurrentViewIndex(parent)));
                    OnItemClickListener onItemClickListener = mOnItemClickListeners
                            .get(getViewID(parent));
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(parent, view, position, id);
                    } else {
                        print("onItemClickListener == null");
                    }
                }
            });
        } else {
            print("onItemClickListener == null at [" + view + "]");
        }
    }

    private void hookOnItemSelectedListener(AdapterView view) {

    }

    private String getViewID(View view) {
        String viewString = view.toString();
        return viewString.substring(viewString.indexOf("@"));
    }

    private void writeToFile(String line) {
        if (null == line) {
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mRecord));
            writer.write(line);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }
}
