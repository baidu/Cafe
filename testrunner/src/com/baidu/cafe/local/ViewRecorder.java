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
        mOnClickListener = (OnClickListener) getListener(view, "mOnClickListener");
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (null != mOnClickListener) {
                    mOnClickListener.onClick(v);
                }
                generateCodeForClick(v);
                print("id:" + v.getId() + "\t click");
            }
        });

        mOnLongClickListener = (OnLongClickListener) getListener(view, "mOnLongClickListener");
        view.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (null != mOnLongClickListener) {
                    mOnLongClickListener.onLongClick(v);
                }
                print("id:" + v.getId() + "\t long_click");
                return false;
            }
        });

        mOnTouchListener = (OnTouchListener) getListener(view, "mOnTouchListener");
        view.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (null != mOnTouchListener) {
                    mOnTouchListener.onTouch(v, event);
                }
                print("id:" + v.getId() + "\t" + event.toString());
                return false;
            }
        });

        mOnKeyListener = (OnKeyListener) getListener(view, "mOnKeyListener");
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

    /**
     * Get listener from view. e.g. (OnClickListener) getListener(view,
     * "mOnClickListener"); means get click listener. Listener is a private
     * property of a view, that's why this function is created.
     * 
     * @param view
     *            target view
     * @param fieldName
     *            target listener. e.g. mOnClickListener, mOnLongClickListener,
     *            mOnTouchListener, mOnKeyListener
     * @return listener object; null means no listeners has been found
     */
    private Object getListener(View view, String fieldName) {
        int level = countLevelFromView(view);
        if (-1 == level) {
            return null;
        }
        try {
            return ReflectHelper.getObjectProperty(view, level, fieldName);
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
        String code = "//view.text=" + local.getViewText(view) + "\n" + "clickOnView(findViewById(new Integer("
                + view.getId() + ")));";
        print(code);
    }
}
