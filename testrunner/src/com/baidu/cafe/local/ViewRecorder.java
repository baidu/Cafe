package com.baidu.cafe.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.TextView.OnEditorActionListener;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-8
 * @version
 * @todo 1.不同版本的Android，录制、回放是否可行。尤其是4.2 2.记录成index形式的 3.touch记录成百分比
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

    class RecordMotionEvent {
        public View        view;
        public MotionEvent motionEvent;

        public RecordMotionEvent(View view, MotionEvent motionEvent) {
            this.view = view;
            this.motionEvent = motionEvent;
        }

        @Override
        public String toString() {
            return "RecordMotionEvent(" + view + ", " + motionEvent + ")";
        }

    }

    public ViewRecorder(LocalLib local) {
        this.local = local;
    }

    private void print(String message) {
        if (Log.IS_DEBUG) {
            Log.i("ViewRecorder", message);
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
        OnClickListener onClickListener = (OnClickListener) local.getListener(view, "mOnClickListener");
        if (null != onClickListener) {
            print("hookClickListener [" + view + "(" + local.getViewText(view) + ")]");
            mOnClickListeners.put(getViewID(view), onClickListener);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    generateCodeForClick(v);
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
        OnLongClickListener onLongClickListener = (OnLongClickListener) local.getListener(view, "mOnLongClickListener");
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
        OnTouchListener onTouchListener = (OnTouchListener) local.getListener(view, "mOnTouchListener");
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
        if (!mMotionEventQueue.offer(new RecordMotionEvent(v, event))) {
            print("Add to mMotionEventQueue Failed! view:" + v + "\t" + event.toString() + "mMotionEventQueue.size="
                    + mMotionEventQueue.size());
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
                        //                        print("" + e);
                        if (e.motionEvent.getAction() == MotionEvent.ACTION_UP) {
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

        // drag from (fromX,fromY) to (toX, toY) by step count 
        float fromX = down.motionEvent.getRawX();
        float fromY = down.motionEvent.getRawY();
        float toX = up.motionEvent.getRawX();
        float toY = up.motionEvent.getRawY();
        print(String.format("Drag from (%s,%s) to (%s, %s) by step count %s", fromX, fromY, toX, toY, stepCount));
    }

    private void hookOnItemClickListener(AdapterView view) {
        OnItemClickListener onItemClickListener = (OnItemClickListener) local.getListener(view, "mOnItemClickListener");

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
                    print("parent: " + parent + "view: " + view + "\t position: " + position + " click ");
                    OnItemClickListener onItemClickListener = mOnItemClickListeners.get(getViewID(parent));
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

    private void generateCodeForClick(View view) {
        String code = "//view.text=" + local.getViewText(view) + "\n" + "clickOnView(findViewById(new Integer("
                + view.getId() + ")));";
        print(code);
    }
}
