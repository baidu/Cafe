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

package com.baidu.cafe.local.traveler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * define view's action
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-11-28
 * @version
 * @todo
 */
public class ViewHelper {
    public final static String             ACTION_CLICK              = "click";
    public final static String             ACTION_TEXT               = "text";
    public final static String             ACTION_LONG_CLICK         = "longclick";
    public final static String             ACTION_SLIDE              = "slide";
    public final static String             ACTION_LISTVIEW           = "listview";

    public static ArrayList<String>        mBlackList                = new ArrayList<String>();

    private static int                     mGloablStep               = 0;
    private static boolean                 mIsLogged                 = false;
    private static ArrayList<View>         mList                     = new ArrayList<View>();
    private static ArrayList<AbstractList> mAbstractLists            = new ArrayList<AbstractList>();

    private final static int               SLEEP_TIME                = 3000;
    private final static int               INTERNET_SLEEP_TIME       = 5000;
    private final static int               TIMEOUT_WAIT_FOR_LOADING  = 1000 * 5;
    private final static int               INTERVAL_WAIT_FOR_LOADING = 500;
    private final static int               TYPE_PASSWORD             = InputType.TYPE_CLASS_TEXT
                                                                             | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private final static int               TYPE_VISIBLE_PASSWORD     = InputType.TYPE_CLASS_TEXT
                                                                             | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

    private final static int               RET_PASS                  = 0;
    private final static int               RET_IN_WHITE_LIST         = 1;
    private final static int               RET_IN_BLACK_LIST         = 2;
    private final static int               RET_NOT_SUITABLE          = 3;
    private final static int               RET_NO_ACTION             = 4;
    private final static int               RET_NO_LISTENER           = 5;
    private final static int               RET_NOT_IN_SCREEN         = 6;
    private final static int               RET_IN_LIST               = 7;

    public static class AbstractList {
        public View list;
        public int  indexAtActivity = 0;
        public int  cursor          = 0;

        public AbstractList(View list, int indexAtActivity, int cursor) {
            this.list = list;
            this.indexAtActivity = indexAtActivity;
            this.cursor = cursor;
        }
    }

    public static void sleep() {
        APPTraveler.local.sleep(getSleepTime());
    }

    private static int getSleepTime() {
        //        if (mUseInternet && mGloablStep++ <= 5) {
        //            Logger.println("INTERNET_SLEEP_TIME: " + INTERNET_SLEEP_TIME);
        //            return INTERNET_SLEEP_TIME;
        //        }
        return SLEEP_TIME;
    }

    public static <T extends View> ArrayList<T> getAllOperatableViews(Class<T> classToFilterBy) {
        ArrayList<View> operatableViews = getAllOperatableViews();
        ArrayList<T> views = new ArrayList<T>();

        for (View view : operatableViews) {
            if (classToFilterBy.isAssignableFrom(view.getClass())) {
                views.add(classToFilterBy.cast(view));
            }
        }
        operatableViews = null;
        return views;
    }

    public static ArrayList<View> sortByDistanceYFromCenter(ArrayList<View> views) {
        final int centerY = APPTraveler.mDisplayY / 2;
        Comparator<View> comparator = new Comparator<View>() {

            @Override
            public int compare(View lhs, View rhs) {
                int distanceLeft = Math.abs(APPTraveler.local.getViewCenter(lhs)[1] - centerY);
                int distanceRight = Math.abs(APPTraveler.local.getViewCenter(rhs)[1] - centerY);
                return distanceLeft - distanceRight;
            }

        };
        Collections.sort(views, comparator);
        return views;
    }

    public static ArrayList<View> getAllOperatableViews() {
        ArrayList<View> views = APPTraveler.local.getCurrentViews();
        //        ArrayList<View> views = CafeCaller.getLocal().getViews();//not onlySufficientlyVisible
        ArrayList<View> operatableViews = new ArrayList<View>();
        if (views.size() == 0) {
            Logger.println("views.size() == 0");
        }

        for (View view : views) {
            if (!view.isShown() || APPTraveler.local.isSize0(view)) {
                continue;
            }

            if (view instanceof ViewGroup) {
                operatableViews.addAll(getListItems(view));
            } else {
                if (isAvailable(view, true)) {
                    operatableViews.add(view);
                }
            }
        }
        if (operatableViews.size() == 0) {
            Logger.println("operatableViews.size() ==0");
        }

        return sortByDistanceYFromCenter(operatableViews);
    }

    public static boolean handlePasswordEditText() {
        if (mIsLogged) {
            return false;
        }

        // fill user name & password
        EditText password = getPasswordEditText();
        EditText userName = null;
        if (null == password) {
            return false;
        } else {
            userName = getUserNameEditText(password);
        }
        if (null == userName) {
            return false;
        }

        Logger.println(String.format("FIND username [%s] & password [%s]", userName, password));
        Logger.println("username:" + APPTraveler.mUsername);
        Logger.println("password:" + APPTraveler.mPassword);
        enterText(userName, APPTraveler.mUsername);
        enterText(password, APPTraveler.mPassword);
        if (clickLoginButton(password)) {
            return mIsLogged = true;
        }

        return false;
    }

    private static boolean clickLoginButton(EditText password) {
        //        ArrayList<View> viewsBelow = getViewsBelow(password);
        View loginButton = getLoginButton(APPTraveler.local.getCurrentViews());
        if (null == loginButton) {
            Logger.println("null == loginButton");
            return false;
            //            int[] xy = new int[2];
            //            password.getLocationOnScreen(xy);
            //            loginButton = getNearestView(xy[0], xy[1], viewsBelow);
        }

        int[] xy = new int[2];
        loginButton.getLocationOnScreen(xy);
        APPTraveler.screenShot(xy);
        APPTraveler.local.clickViewWithoutAssert(loginButton);
        return true;
    }

    private static View getLoginButton(ArrayList<View> buttons) {
        String loginString1 = APPTraveler.local.getTestRString("login1");
        String loginString2 = APPTraveler.local.getTestRString("login2");

        for (View button : buttons) {
            if (button instanceof Button) {
                String text = ((Button) button).getText().toString();
                if (text.contains(loginString1) && text.contains(loginString2)) {
                    Logger.println("loginButton: " + text);
                    return button;
                }
            }
        }
        return null;
    }

    private static ArrayList<View> getViewsBelow(View anchor) {
        int[] xyAnchor = new int[2];
        anchor.getLocationOnScreen(xyAnchor);
        ArrayList<View> viewsBelow = new ArrayList<View>();
        for (View view : APPTraveler.local.getCurrentViews()) {
            int[] xy = new int[2];
            view.getLocationOnScreen(xy);
            if (xy[1] > xyAnchor[1]) {
                viewsBelow.add(view);
            }
        }
        return viewsBelow;
    }

    private static EditText getPasswordEditText() {
        ArrayList<EditText> editTexts = APPTraveler.local.getCurrentViews(EditText.class);
        for (EditText editText : editTexts) {
            int type = editText.getInputType();
            Logger.println("" + editText + "InputType:" + type);
            if (type == TYPE_PASSWORD || type == TYPE_VISIBLE_PASSWORD) {
                return editText;
            }
        }
        return null;
    }

    private static EditText getUserNameEditText(EditText password) {
        int[] passwordXY = new int[2];
        password.getLocationOnScreen(passwordXY);

        // get edittexts above password edittext
        ArrayList<EditText> editTexts = APPTraveler.local.getCurrentViews(EditText.class);
        ArrayList<View> upEditTexts = new ArrayList<View>();
        for (EditText editText : editTexts) {
            int[] xy = new int[2];
            editText.getLocationOnScreen(xy);
            if (xy[1] < passwordXY[1]) {
                upEditTexts.add(editText);
            }
        }

        return (EditText) getNearestView(passwordXY[0], passwordXY[1], upEditTexts);
    }

    private static View getNearestView(int x, int y, ArrayList<View> candidate) {
        double minDistance = Double.MAX_VALUE;
        View nearestView = null;
        for (View view : candidate) {
            int[] xy = new int[2];
            view.getLocationOnScreen(xy);
            double distance = Util.getDistance(x, y, xy[0], xy[1]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestView = view;
            }
        }
        return nearestView;
    }

    private static ArrayList<View> getListItems(View view) {
        ArrayList<View> items = new ArrayList<View>();

        //        if (mList.contains(view)) {
        //            Logger.println(view + " is old");
        //            return items;
        //        }

        if (view instanceof ListView) {
            items.add(view);

        } else if (view instanceof ScrollView) {
            items = getVisibleViews(getScrollViewListItems((ViewGroup) view));
        } else if (view instanceof GridView) {
            // TODO
        }

        //        if (items.size() != 0) {
        //            Logger.println("add " + view + " to list");
        //            mList.add(view);
        //        }
        return items;
    }

    private static ArrayList<View> getListViewListItems(ListView listView) {
        ArrayList<View> items = new ArrayList<View>();
        Logger.println("getListViewListItems: " + listView);
        for (int i = listView.getFirstVisiblePosition(); i <= listView.getLastVisiblePosition(); i++) {
            Logger.println("listitem: " + listView.getChildAt(i).toString());
            items.add(listView.getChildAt(i));
        }
        return items;
    }

    private static boolean isSuitableTextView(TextView textView) {
        if (isInList(textView)) {
            return false;
        }
        return true;
    }

    private static boolean isInList(View view) {
        View parent = view;
        while ((parent = getParent(parent)) != null) {
            if (isList(parent)) {
                return true;
            }
        }
        return false;
    }

    public static String getViewText(View view) {
        return view instanceof TextView ? ((TextView) view).getText().toString() : "";
    }

    private static boolean hasListener(View view) {
        if (APPTraveler.local.getListener(view, View.class, "mOnClickListener") != null) {
            return true;
        }
        if (APPTraveler.local.getListener(view, View.class, "mOnLongClickListener") != null) {
            return true;
        }
        if (APPTraveler.local.getListener(view, View.class, "mOnTouchListener") != null) {
            return true;
        }
        if (APPTraveler.local.getListener(view, View.class, "mOnKeyListener") != null) {
            return true;
        }
        return false;
    }

    private static ArrayList<View> getVisibleViews(ArrayList<View> views) {
        ArrayList<View> visibleViews = new ArrayList<View>();
        for (View view : views) {
            if (APPTraveler.local.isInScreen(view)) {
                visibleViews.add(view);
            }
        }
        return visibleViews;
    }

    private static ArrayList<View> getScrollViewListItems(ViewGroup viewGroup) {
        ArrayList<View> children = new ArrayList<View>();
        if (hasListItems(viewGroup)) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                children.add(viewGroup.getChildAt(i));
            }
            return children;
        }

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                ArrayList<View> grandson = getScrollViewListItems((ViewGroup) child);
                if (grandson.size() != 0) {
                    Logger.println("father: " + getViewText(child));
                    return grandson;
                }
            }
        }

        return children;
    }

    private static boolean hasListItems(ViewGroup viewGroup) {
        if (viewGroup.getChildCount() < 3) {
            return false;
        }

        int width = viewGroup.getChildAt(0).getWidth();
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            // Children must be clickable.
            if (!child.isClickable()) {
                return false;
            }

            // Width of children must be same.
            if (child.getWidth() != width) {
                return false;
            }
        }

        return true;
    }

    private static int getListIndex(View targetList) {
        ArrayList<? extends View> lists = APPTraveler.local.getCurrentViews(targetList.getClass());
        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i).equals(targetList)) {
                return i;
            }
        }
        Logger.println("Can not find " + targetList.toString() + "!!!");
        return -1;
    }

    private static void addAbstractList(View list, int indexAtActivity) {
        if (getAbstractList(list) == null) {
            mAbstractLists.add(new AbstractList(list, indexAtActivity, 1));
        }
    }

    public static AbstractList getAbstractList(View list) {
        for (AbstractList abstractList : mAbstractLists) {
            if (abstractList.list.equals(list)) {
                return abstractList;
            }
        }
        return null;
    }

    public static ArrayList<Operation> getViewOperations(View view, int viewIndex) {
        ArrayList<String> actions = getViewActions(view);
        ArrayList<Operation> operations = new ArrayList<Operation>();

        for (String action : actions) {
            Operation operation = new Operation(view, viewIndex, action,
                    APPTraveler.remote.getTopActivity());
            //            Logger.println("operation: " + operation);
            operations.add(operation);
        }

        return operations;
    }

    public static ArrayList<String> getViewActions(View view) {
        ArrayList<String> actions = new ArrayList<String>();

        if (view instanceof ListView) {
            ListView listView = (ListView) view;
            int lines = listView.getLastVisiblePosition() - listView.getFirstVisiblePosition();
            // Ignore the last line in case that the last line is not visible completely.
            for (int i = 1; i <= lines; i++) {
                //                Logger.println("add ACTION_LISTVIEW" + i);
                actions.add(ACTION_LISTVIEW + i);
            }
            addAbstractList(listView, getListIndex(listView));
            return actions;
        }
        //        else if (view instanceof ScrollView) {
        //            ScrollView scrollView = (ScrollView) view;
        //            int lines = getScrollViewVisibleChildCount(scrollView);
        //            for (int i = 1; i <= lines; i++) {
        //                actions.add(ACTION_SCROLLVIEW + i);
        //            }
        //            addAbstractList(scrollView, getListIndex(scrollView));
        //            return actions;
        //        } else if (view instanceof GridView) {
        //
        //        }

        if (view instanceof EditText) {
            actions.add(ACTION_TEXT);
            return actions;
        }

        if (isInList(view)) {
            return actions;
        }

        // TextView is not clickable but it's parent is clickable
        if (view.isClickable() || isClickableTextView(view)) {
            actions.add(ACTION_CLICK);
        }

        // TODO EditText longclick is not repeatable, so it is disable.
        //        if (view.isLongClickable() && !(view instanceof EditText)) {
        //            actions.add(ACTION_LONG_CLICK);
        //        }

        if (view instanceof ViewFlipper) {
            actions.add(ACTION_SLIDE);
        }

        // content menu

        // menu
        // TODO how to make sure that there is a menu ?

        return actions;
    }

    public static void waitForLoading() {
        long end = System.currentTimeMillis() + TIMEOUT_WAIT_FOR_LOADING;
        while (ViewHelper.hasLoadingView()) {
            try {
                Thread.sleep(INTERVAL_WAIT_FOR_LOADING);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() > end) {
                Logger.println("waitForLoading timeout!");
                break;
            }
        }
    }

    /**
     * Find indeterminate ProgressBar from current visibile views.
     * 
     * @param views
     * @return
     */
    private static boolean hasLoadingView() {
        ArrayList<View> views = APPTraveler.local.getCurrentViews();
        for (View view : views) {
            if (view instanceof ProgressBar) {
                ProgressBar progressBar = (ProgressBar) view;
                if (progressBar.isIndeterminate()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isClickableTextView(View view) {
        if (!(view instanceof TextView)) {
            return false;
        }
        return hasClickableParent(view);
    }

    private static boolean hasClickableParent(View view) {
        View parentView = view;
        while ((parentView = getParent(parentView)) != null) {
            if (parentView.isClickable()) {
                return true;
            }
        }
        return false;
    }

    private static View getParent(View view) {
        if (!(view.getParent() instanceof View)) {
            return null;
        }
        return (View) view.getParent();
    }

    private static boolean isList(View view) {
        return view instanceof ListView || view instanceof ScrollView || view instanceof GridView ? true
                : false;
    }

    private static boolean isInWhiteList(View view) {
        return false;
    }

    private static boolean isInBlackList(View view) {
        for (String str : mBlackList) {
            if (APPTraveler.local.getViewText(view).equals(str)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isAvailable(View view, boolean careListener) {
        int ret = isTargetView(view, careListener);
        String retString = Util.getReturnValueName(ViewHelper.class, ret);
        if (ret <= RET_IN_WHITE_LIST) {
            //            Logger.println("Passed [" + retString + "] " + view.toString() + " " + getViewText(view));
            return true;
        } else {
            //            Logger.println("Failed [" + retString + "] " + view.toString() + " " + getViewText(view));
            return false;
        }
    }

    private static int isTargetView(View view, boolean careListener) {
        if (isInBlackList(view)) {
            return RET_IN_BLACK_LIST;
        }

        if (isInWhiteList(view)) {
            return RET_IN_WHITE_LIST;
        }

        if (!isSuitable(view)) {
            return RET_NOT_SUITABLE;
        }

        if (getViewActions(view).size() == 0) {
            return RET_NO_ACTION;
        }

        if (!APPTraveler.local.isInScreen(view)) {
            return RET_NOT_IN_SCREEN;
        }

        //        if (careListener && !hasListener(view)) {
        //            return RET_NO_LISTENER;
        //        }

        if (view instanceof TextView && !isSuitableTextView((TextView) view)) {
            return RET_IN_LIST;
        }
        return RET_PASS;
    }

    private static boolean isSuitable(View view) {
        if (!view.isShown()) {
            return false;
        }

        //        if (view instanceof ViewGroup) {
        //            return false;
        //        }

        if (!view.isEnabled()) {
            return false;
        }

        return true;
    }

    public static void goBack(String reason) {
        Logger.println("goBack because of [" + reason + "]");
        APPTraveler.local.hideInputMethod();
        APPTraveler.remote.sleep(SLEEP_TIME);

        APPTraveler.remote.goBack();
        APPTraveler.remote.sleep(SLEEP_TIME);
    }

    public static void enterText(EditText editText, String text) {
        // if text == null will cause app crash!
        if (null == text || text.length() == 0) {
            Logger.println("null == text || text.length() == 0");
        } else {
            APPTraveler.local.enterText(editText, text);
            APPTraveler.local.sleep(500);
        }
    }
}
