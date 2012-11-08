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
            setAutoGenerateCodeListenerOnView(allViews.get(i));
        }
    }

    private void setAutoGenerateCodeListenerOnView(View view) {
        mOnClickListener = (OnClickListener) local.getListener(view, "mOnClickListener");
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (null != mOnClickListener) {
                    mOnClickListener.onClick(v);
                }
                generateCodeForClick(v);
                print("id:" + v.getId() + "\t click");
            }
        });

        mOnLongClickListener = (OnLongClickListener) local.getListener(view, "mOnLongClickListener");
        view.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (null != mOnLongClickListener) {
                    mOnLongClickListener.onLongClick(v);
                }
                print("id:" + v.getId() + "\t long_click");
                return false;
            }
        });

        mOnTouchListener = (OnTouchListener) local.getListener(view, "mOnTouchListener");
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (null != mOnTouchListener) {
                    mOnTouchListener.onTouch(v, event);
                }
                print("id:" + v.getId() + "\t" + event.toString());
                return false;
            }
        });

        mOnKeyListener = (OnKeyListener) local.getListener(view, "mOnKeyListener");
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

    private void generateCodeForClick(View view) {
        String code = "//view.text=" + local.getViewText(view) + "\n" + "clickOnView(findViewById(new Integer("
                + view.getId() + ")));";
        print(code);
    }
}
