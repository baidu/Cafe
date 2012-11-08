package com.baidu.cafe.local;

import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-11-8
 * @version
 * @todo
 */
public class ViewRecorder {
    private final static boolean DEBUG = true;

    private OnClickListener      mOnClickListener;
    private OnLongClickListener  mOnLongClickListener;
    private OnTouchListener      mOnTouchListener;
    private OnKeyListener        mOnKeyListener;
    private LocalLib             local = null;

    public ViewRecorder(LocalLib local) {
        this.local = local;
    }

    private void print(String message) {
        if (DEBUG) {
            Log.i("ViewRecorder", message);
        }
    }

    /**
     * add listeners on all views for generating robotium code automatically
     */
    public void beginRecordCode() {
        ArrayList<View> allViews = local.getViews();
        int viewNumber = allViews.size();
        print("viewNumber=" + viewNumber);
        for (int i = 0; i < viewNumber; i++) {
            try {
                setAutoGenerateCodeListenerOnView(allViews.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setAutoGenerateCodeListenerOnView(View view) {
        if (view instanceof AdapterView) {
            print("ignore AdapterView [" + view + "]");
            return;
        }

        mOnClickListener = (OnClickListener) local.getListener(view, "mOnClickListener");
        if (null != mOnClickListener) {
            print("hook [" + view + "]");
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    generateCodeForClick(v);
                    print("id:" + v.getId() + "\t click");
                    mOnClickListener.onClick(v);
                }
            });
        }

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

        mOnTouchListener = (OnTouchListener) local.getListener(view, "mOnTouchListener");
        if (null != mOnTouchListener) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    print("id:" + v.getId() + "\t" + event.toString());
                    mOnTouchListener.onTouch(v, event);
                    return false;
                }
            });
        }

        mOnKeyListener = (OnKeyListener) local.getListener(view, "mOnKeyListener");
        if (null != mOnKeyListener) {
            view.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    print("id:" + v.getId() + "\t" + event.toString() + "\t" + keyCode);
                    mOnKeyListener.onKey(v, keyCode, event);
                    return false;
                }
            });
        }

    }

    private void generateCodeForClick(View view) {
        String code = "//view.text=" + local.getViewText(view) + "\n" + "clickOnView(findViewById(new Integer("
                + view.getId() + ")));";
        print(code);
    }
}
