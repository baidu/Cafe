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

package com.baidu.cafe.remote;

import java.util.ArrayList;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * An ui operation lib for testing demand across processes.
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-8-6
 * @version
 * @todo
 */
public class UILib {

    /**
     * If Arms is signed by platform signature, UILib should use
     * USE_INSTRUMENTATION. USE_INSTRUMENTATION has better performance than
     * USE_MONKEY.
     */
    private final static int     USE_INSTRUMENTATION   = 0;
    private final static int     USE_MONKEY            = 1;
    private final static int     EVENT_SENDER          = USE_MONKEY;
    private final static int     STEP_COUNT            = 100;
    private final static int     SCROLL_TIME_OUT       = 120000;
    private final static int     DOWN                  = 0;
    private final static int     UP                    = 1;

    private ViewPropertyProvider mViewPropertyProvider = null;
    private Instrumentation      mInstrumentation      = null;

    public UILib(ViewPropertyProvider viewPropertyProvider) {
        mViewPropertyProvider = viewPropertyProvider;
        mInstrumentation = new Instrumentation();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * simulate key pad using key code
     * 
     * @param keyCode
     *            the code to be simulated
     * @param longPress
     *            true if need to press long, else false
     */
    private void keypad(int keyCode, boolean longPress) {
        switch (EVENT_SENDER) {
        case USE_INSTRUMENTATION:
            if (longPress) {
                mInstrumentation.sendKeySync(KeyEvent.changeFlags(new KeyEvent(
                        KeyEvent.ACTION_DOWN, keyCode), KeyEvent.FLAG_LONG_PRESS));
            } else {
                mInstrumentation.sendKeyDownUpSync(keyCode);
            }
            break;
        case USE_MONKEY:
            new MonkeyNetwork().key(MonkeyNetwork.DOWN, keyCode);
            if (longPress) {
                SystemClock.sleep((int) (ViewConfiguration.getLongPressTimeout() * 1.5f));
            }
            new MonkeyNetwork().key(MonkeyNetwork.UP, keyCode);
            //            new MonkeyNetwork().done();
            break;
        }
    }

    /**
     * check whether the view exist or even has more
     * 
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param targetNumber
     *            the amount of views you wanna to dump
     * @return true if the number of the view not less than {targetNumber}, else
     *         false
     */
    public boolean checkView(String searchKey, String searchValue, int searchMode, int targetNumber) {
        String[] getKeys = { "mID" };
        ArrayList<String[]> getValues = mViewPropertyProvider.getViewsProperties(searchKey,
                searchValue, searchMode, targetNumber, getKeys, true, true);
        return targetNumber <= getValues.size();
    }

    /**
     * enter a text by instrumentation
     * 
     * @param text
     *            the text you wanna to enter
     */
    public void enterText(String text) {

        switch (EVENT_SENDER) {
        case USE_INSTRUMENTATION:
            mInstrumentation.sendStringSync(text);
            break;
        case USE_MONKEY:
            new MonkeyNetwork().type(text);
            break;
        }
    }

    /**
     * press a key
     * 
     * @param keyCode
     *            the key code u wanna to press
     */
    public void pressKey(int keyCode) {
        keypad(keyCode, false);
    }

    /**
     * press a key long time
     * 
     * @param keyCode
     *            the key code u wanna to press
     */
    public void longPressKey(int keyCode) {
        keypad(keyCode, true);
    }

    /**
     * click one dot of a screen
     * 
     * @param x
     *            the x value of a coordinate
     * @param y
     *            the y value of a coordinate
     */
    public void clickScreen(int x, int y) {
        touch(x, x, y, y, 0, 0);
    }

    /**
     * click one dot of a screen long time
     * 
     * @param x
     *            the x value of a coordinate
     * @param y
     *            the y value of a coordinate
     * @param longClickTime
     *            the time length u wanna click
     */
    public void clickLongScreen(int x, int y, int longClickTime) {
        if (longClickTime < 0) {
            Log.print("param error: longClickTime < 0");
            return;
        }

        touch(x, x, y, y, 0, longClickTime);
    }

    /**
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param index
     *            the amount of views you wanna to dump
     * @param timeout
     *            the time length for clicking
     * @param xOffset
     *            offset from center X
     * @param yOffset
     *            offset from center Y
     * @param longClickTime
     *            touch time length
     * @param scrollViewId
     *            the given scroll view's id
     * @param scrollViewIndex
     *            the given scroll view's index level
     * @return
     */
    public boolean clickView(String searchKey, String searchValue, int searchMode, int index,
            int timeout, int xOffset, int yOffset, int longClickTime, String scrollViewId,
            int scrollViewIndex) {
        if (index < 0 || timeout < 0 || null == searchKey || searchKey.isEmpty()
                || null == searchValue || longClickTime < 0 || scrollViewIndex < 0) {
            Log.print("clickView's param error");
            return false;
        }

        if (scrollViewId != null) {
            timeout = SCROLL_TIME_OUT;
        }

        ArrayList<String[]> getValues;
        final long endTime = System.currentTimeMillis() + timeout;

        while (true) {
            final boolean timedOut = System.currentTimeMillis() > endTime;
            if (timedOut) {
                Log.print("click View timeout");
                return false;
            }

            getValues = mViewPropertyProvider.getViewsProperties(searchKey, searchValue,
                    searchMode, index + 1, new String[] { "coordinate" }, true, true);
            Log.print("getValues.size():" + getValues.size());

            if (getValues.size() > index) {
                final String[] coordinates = getValues.get(index)[0].split("\\,");
                if (scrollViewId != null
                        && !isClickInList(coordinates, scrollViewId, scrollViewIndex)) {
                    Log.print("Scroll half height of scroll view, because target view is not completely visible.");
                    scrollList(DOWN, (float) 0.5, scrollViewId, scrollViewIndex);
                    getValues = mViewPropertyProvider.getViewsProperties(searchKey, searchValue,
                            searchMode, index + 1, new String[] { "coordinate" }, true, true);
                }
                break;
            }

            if (scrollViewId != null && !scrollList(DOWN, 1, scrollViewId, scrollViewIndex)) {
                if (index >= getValues.size()) {
                    Log.print("Found Failed:" + searchKey + " = " + searchValue);
                    return false;
                }
                break;
            }

            getValues.clear();
        }

        final String[] coordinates = getValues.get(index)[0].split("\\,");
        int[] centerXY = getCenterXY(coordinates);
        int centerX = centerXY[0] + xOffset;
        int centerY = centerXY[1] + yOffset;
        Log.print("centerX + xOffset = " + centerX);
        Log.print("centerY + yOffset = " + centerY);

        touch(centerX, centerX, centerY, centerY, 0, longClickTime);

        return true;
    }

    /**
     * check whether the given scroll view's center coordinate is in the given
     * coordinates
     * 
     * @param coordinates
     *            all of coordinates to be calculated
     * @param scrollViewId
     *            the given scroll view's id
     * @param scrollViewIndex
     *            the given scroll view's index level
     * @return true if the the given scroll view's center coordinate is in the
     *         given coordinates
     */
    private boolean isClickInList(String[] coordinates, String scrollViewId, int scrollViewIndex) {
        int[] clickXY = getCenterXY(coordinates);
        String[] xywh = getScrollProperty(scrollViewId, scrollViewIndex, "coordinate", false)
                .split("\\,");
        int scrollViewX = Integer.valueOf(xywh[0]);
        int scrollViewY = Integer.valueOf(xywh[1]);
        int scrollViewWidth = Integer.valueOf(xywh[2]);
        int scrollViewHeight = Integer.valueOf(xywh[3]);

        if (scrollViewX < clickXY[0] && clickXY[0] < scrollViewX + scrollViewWidth
                && scrollViewY < clickXY[1] && clickXY[1] < scrollViewY + scrollViewHeight) {
            return true;
        }

        return false;
    }

    /**
     * get center coordinate of the given coordinates
     * 
     * @param coordinates
     *            all of coordinates to be calculated
     * @return the center coordinate
     */
    private int[] getCenterXY(String[] coordinates) {
        int[] centerXY = new int[2];
        int x = Integer.valueOf(coordinates[0]);
        int y = Integer.valueOf(coordinates[1]);
        int width = Integer.valueOf(coordinates[2]);
        int height = Integer.valueOf(coordinates[3]);
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        centerXY[0] = centerX;
        centerXY[1] = centerY;

        return centerXY;
    }

    /**
     * drag from X to Y
     * 
     * @param fromX
     *            the x value of the original dot
     * @param toX
     *            the x value of the destination dot
     * @param fromY
     *            the y value of the original value
     * @param toY
     *            the y value of the destination dot
     * @param stepCount
     *            the step to touch from X to Y
     * @return
     */
    public void drag(float fromX, float toX, float fromY, float toY, int stepCount) {
        Log.print("scroll from (" + (int) fromX + "," + (int) fromY + ") to (" + (int) toX + ","
                + (int) toY + ")");
        touch(fromX, toX, fromY, toY, stepCount, 0);
    }

    /**
     * touch screen including two ways: instrumentation or native event
     * 
     * @param fromX
     *            the x value of the original dot
     * @param toX
     *            the x value of the destination dot
     * @param fromY
     *            the y value of the original value
     * @param toY
     *            the y value of the destination dot
     * @param stepCount
     *            the step to touch from X to Y
     * @param longClickTime
     *            touch time length
     */
    private void touch(float fromX, float toX, float fromY, float toY, int stepCount,
            int longClickTime) {
        switch (EVENT_SENDER) {
        case USE_INSTRUMENTATION:
            touchUseInstrumentation(fromX, toX, fromY, toY, stepCount, longClickTime);
            break;
        case USE_MONKEY:
            touchUseMonkey(fromX, toX, fromY, toY, stepCount, longClickTime);
            break;
        }
    }

    /**
     * touch screen from X to Y continually, maybe many steps according to your
     * params
     * 
     * @param fromX
     *            the x value of the original dot
     * @param toX
     *            the x value of the destination dot
     * @param fromY
     *            the y value of the original value
     * @param toY
     *            the y value of the destination dot
     * @param stepCount
     *            the step to touch from X to Y
     * @param longClickTime
     *            touch time length
     */
    private void touchUseInstrumentation(float fromX, float toX, float fromY, float toY,
            int stepCount, int longClickTime) {
        if (stepCount > 0 && longClickTime > 0) {
            Log.print("touchUseInstrumentation's param error: stepCount > 0 && longClickTime > 0");
        }

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        float x = fromX;
        float y = fromY;
        float xStep = 0;
        float yStep = 0;

        if (stepCount > 0) {
            xStep = (toX - fromX) / stepCount;
            yStep = (toY - fromY) / stepCount;
        }

        mInstrumentation.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0));

        for (int i = 0; i < stepCount; ++i) {
            y += yStep;
            x += xStep;
            eventTime = SystemClock.uptimeMillis();
            mInstrumentation.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_MOVE, x, y, 0));
        }

        if (longClickTime > 0) {
            mInstrumentation.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                    MotionEvent.ACTION_MOVE, x, y, 0));
            SystemClock.sleep(longClickTime);
        }

        eventTime = SystemClock.uptimeMillis();
        mInstrumentation.sendPointerSync(MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_UP, x, y, 0));
    }

    /**
     * touch screen using native event
     * 
     * @param fromX
     *            the x value of the original dot
     * @param toX
     *            the x value of the destination dot
     * @param fromY
     *            the y value of the original value
     * @param toY
     *            the y value of the destination dot
     * @param stepCount
     *            the step to touch from X to Y
     * @param longClickTime
     *            touch time length
     */
    private void touchUseMonkey(float fromX, float toX, float fromY, float toY, int stepCount,
            int longClickTime) {
        if (stepCount > 0 && longClickTime > 0) {
            Log.print("touchUseInstrumentation's param error: stepCount > 0 && longClickTime > 0");
        }

        int x = (int) fromX;
        int y = (int) fromY;
        float xStep = 0;
        float yStep = 0;

        if (stepCount > 0) {
            xStep = (toX - fromX) / stepCount;
            yStep = (toY - fromY) / stepCount;
        }

        new MonkeyNetwork().touch(MonkeyNetwork.DOWN, x, y);

        for (int i = 0; i < stepCount; ++i) {
            y += yStep;
            x += xStep;
            new MonkeyNetwork().touch(MonkeyNetwork.MOVE, x, y);
        }

        if (longClickTime > 0) {
            new MonkeyNetwork().touch(MonkeyNetwork.MOVE, x, y);
            SystemClock.sleep(longClickTime);
        }

        new MonkeyNetwork().touch(MonkeyNetwork.UP, x, y);
        //        new MonkeyNetwork().done();
    }

    /**
     * @param direction
     *            scroll direction
     * @param scrollDistance
     *            scroll distance that how many height of scrollView
     * @param scrollViewId
     *            id of scrollView
     * @param scrollViewIndex
     *            index of the same id
     * @return true if more scrolling can be done
     */
    public boolean scrollList(int direction, float scrollDistance, String scrollViewId,
            int scrollViewIndex) {
        String[] xywh = getScrollProperty(scrollViewId, scrollViewIndex, "coordinate", false)
                .split("\\,");
        int x = Integer.valueOf(xywh[0]);
        int y = Integer.valueOf(xywh[1]);
        int width = Integer.valueOf(xywh[2]);
        int height = Integer.valueOf(xywh[3]);
        int centerX = x + width / 2;

        int scrollAmount = getScrollAmount(scrollViewId, scrollViewIndex, false);
        if (direction == DOWN) {
            drag(centerX, centerX, (y + height - 1) * scrollDistance, y + 1, STEP_COUNT);
        } else if (direction == UP) {
            drag(centerX, centerX, y + 1, (y + height - 1) * scrollDistance, STEP_COUNT);
        }

        return getScrollAmount(scrollViewId, scrollViewIndex, true) == scrollAmount ? false : true;
    }

    /**
     * get the amount of the scroll
     * 
     * @param scrollViewId
     *            the given scroll view's id
     * @param scrollViewIndex
     *            the given scroll view's index level
     * @param getNew
     *            true if the view is already dumped, else false
     * @return the amount of the scroll
     */
    private int getScrollAmount(String scrollViewId, int scrollViewIndex, boolean getNew) {
        int scrollAmount = 0;
        String mFirstPosition = getScrollProperty(scrollViewId, scrollViewIndex, "mFirstPosition",
                getNew);
        String mScrollY = getScrollProperty(scrollViewId, scrollViewIndex, "mScrollY", getNew);

        if (null == mFirstPosition) {
            // scrollview
            scrollAmount = Integer.valueOf(mScrollY);
        } else {
            // listview gridview
            scrollAmount = Integer.valueOf(mFirstPosition);
        }
        return scrollAmount;
    }

    /**
     * get scroll view's properties including its x and y value, and its height
     * and with
     * 
     * @param scrollViewId
     *            the given scroll view's id
     * @param scrollViewIndex
     *            the given scroll view's index level
     * @param getKey
     *            what the keys u wanna to get its properties
     * @param getNew
     *            true if the view is already dumped, else false
     * @return the given scroll view's properties
     */
    private String getScrollProperty(String scrollViewId, int scrollViewIndex, String getKey,
            boolean getNew) {
        return mViewPropertyProvider.getViewsProperties("mID", scrollViewId,
                ViewPropertyProvider.SEARCHMODE_COMPLETE_MATCHING, scrollViewIndex + 1,
                new String[] { getKey }, getNew, true).get(scrollViewIndex)[0];
    }

}
