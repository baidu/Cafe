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

package com.baidu.cafe.local.traveler;

import java.util.ArrayList;

import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.baidu.cafe.local.traveler.Util.Equal;
import com.baidu.cafe.local.traveler.ViewHelper.AbstractList;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-1-2
 * @version
 * @todo
 */
public class Operation {
    public final static int  STATE_UNDONE         = 0;
    public final static int  STATE_DOING          = 1;
    public final static int  STATE_DONE           = 2;

    private final static int RANDOM_STRING_LENGTH = 10;
    // activity index isDone
    private View             mView                = null;
    private String           mViewString          = null;
    private int[]            mViewLocation        = new int[2];
    private int              mState               = STATE_UNDONE;
    private int              mLevel               = 0;
    private String           mAction              = null;
    private boolean          mEnable              = true;
    private String           mActivity            = null;
    private boolean          mShouldRepeat        = false;

    /**
     * index on activity
     */
    private int              mViewIndex           = -1;

    public Operation(View view, int viewIndex, String action, String activity) {
        mView = view;
        mViewString = view.toString();
        mView.getLocationOnScreen(mViewLocation);
        mViewIndex = viewIndex;
        mAction = action;
        mActivity = activity;
    }

    /**
     * @return click point (x, y)
     */
    public int[] doOperation() {
        mState = STATE_DOING;
        int[] xy = APPTraveler.local.getViewCenter(mView);
        View targetView = mView;

        if (ViewHelper.ACTION_CLICK.equals(mAction)) {
            APPTraveler.local.clickViewWithoutAssert(mView);
        } else if (ViewHelper.ACTION_TEXT.equals(mAction)) {
            ViewHelper.enterText((EditText) mView, Util.getRandomString(RANDOM_STRING_LENGTH));
        } else if (ViewHelper.ACTION_LONG_CLICK.equals(mAction)) {
            APPTraveler.local.clickLongOnView(mView);
        } else if (ViewHelper.ACTION_SLIDE.equals(mAction)) {
            APPTraveler.local.dragScreenToRight(10);
        } else if (mAction.indexOf(ViewHelper.ACTION_LISTVIEW) != -1) {
            AbstractList abstractListView = ViewHelper.getAbstractList(mView);
            targetView = ((ListView) abstractListView.list)
                    .getChildAt(abstractListView.cursor++ - 1);
            xy = APPTraveler.local.getViewCenter(targetView);
            APPTraveler.local.clickViewWithoutAssert(targetView);
        }
        //        else if (mAction.indexOf(ViewOperator.ACTION_SCROLLVIEW) != -1) {
        //            AbstractList abstractScrollView = ViewOperator.getAbstractList(mView);
        //            Logger.println("get scrollview: " + abstractScrollView.cursor);
        //            targetView = ((ScrollView) abstractScrollView.list).getChildAt(abstractScrollView.cursor++ - 1);
        //            ViewOperator.clickViewWithoutAssert(targetView);
        //        }

        mState = STATE_DONE;
        if (isInScreen(xy)) {
            Logger.println(mAction + " on [" + getText() + "] " + mView.toString());
            APPTraveler.local.hideInputMethod();
            ViewHelper.sleep();
        } else {
            Logger.println("click can not be performed because " + targetView + " is out of screen");
            return null;
        }

        return xy;
    }

    public boolean isInScreen(int[] xy) {
        return xy[0] < 0 || xy[0] > APPTraveler.mDisplayX || xy[1] < 0
                || xy[1] > APPTraveler.mDisplayY ? false : true;
    }

    public int[] getViewLocation() {
        return mViewLocation;
    }

    public void setViewLocation(int[] mViewLocation) {
        this.mViewLocation = mViewLocation;
    }

    public String getViewString() {
        return mViewString;
    }

    public void setViewString(String mViewString) {
        this.mViewString = mViewString;
    }

    public String getActivity() {
        return mActivity;
    }

    public void setActivity(String mActivity) {
        this.mActivity = mActivity;
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int mLevel) {
        this.mLevel = mLevel;
    }

    public boolean isShouldRepeat() {
        return mShouldRepeat;
    }

    public void setShouldRepeat(boolean shouldRepeat) {
        this.mShouldRepeat = shouldRepeat;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        this.mState = state;
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        this.mView = view;
    }

    public String getAction() {
        return mAction;
    }

    public void setAction(String action) {
        this.mAction = action;
    }

    public boolean isEnable() {
        return mEnable;
    }

    public void setEnable(boolean enable) {
        this.mEnable = enable;
    }

    private String getText() {
        return ViewHelper.getViewText(mView);
    }

    public int getViewIndex() {
        return mViewIndex;
    }

    public void setViewIndex(int viewIndex) {
        this.mViewIndex = viewIndex;
    }

    @Override
    public String toString() {
        String state = mEnable ? String.valueOf(mState) : "F";
        return mViewString.substring(mViewString.indexOf('@') + 1) + "(" + getText() + ")" + "."
                + mAction + "(" + state + ")" + mLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Operation)) {
            return false;
        }

        Operation operation = (Operation) o;

        if (!isTheSameView(operation)) {
            return false;
        }

        //        if (!mActivity.equals(operation.mActivity)) {
        //            return false;
        //        }

        if (!operation.mAction.equals(this.mAction)) {
            return false;
        }

        return true;
    }

    //    public boolean equals(Object o) {
    //        if (!(o instanceof Operation)) {
    //            return false;
    //        }
    //
    //        Operation operation = (Operation) o;
    //        // view.toString() is like that "android.widget.ImageButton@402f8d80"
    //        // so there is no need to compare this.index
    //        if (!operation.mViewString.equals(this.mViewString)) {
    //            return false;
    //        }
    //
    //        if (!operation.mAction.equals(this.mAction)) {
    //            return false;
    //        }
    //
    //        return true;
    //    }

    /**
     * stricter than equals()
     * 
     * @param operation
     * @return
     */
    public boolean isTheSameOperation(Operation operation) {
        if (!mViewString.equals(operation.mViewString)) {
            return false;
        }

        if (!this.equals(operation)) {
            return false;
        }

        return true;
    }

    private boolean isTheSameView(Operation operation) {
        if (!isTheSameLocation(operation)) {
            return false;
        }

        // view.toString() is like that "android.widget.ImageButton@402f8d80"
        // so there is no need to compare this.index
        if (!getTypeString(mViewString).equals(getTypeString(operation.mViewString))) {
            return false;
        }

        //        if (Util.getViewSnapshot(view1).equals(Util.getViewSnapshot(view2))) {
        //            return true;
        //        }

        return true;
    }

    private boolean isTheSameLocation(Operation operation) {
        int[] location = operation.mViewLocation;
        return mViewLocation[0] == location[0] && mViewLocation[1] == location[1] ? true : false;
    }

    private String getTypeString(String viewString) {
        return viewString.substring(0, viewString.indexOf("@"));
    }

    /**
     * @param level
     *            means count of recursion
     * @return operations changed including added and removed
     */
    public static ArrayList<Operation> updateOperations(int level) {
        ArrayList<Operation> newOperations = new ArrayList<Operation>();
        ArrayList<Operation> operations = getCurrentOperations();
        String topActivity = APPTraveler.remote.getTopActivity();
        String topPackage = APPTraveler.remote.getTopPackage();
        Logger.println("topActivity: " + topActivity);

        if (operations.size() == 0) {
            if (!APPTraveler.local.mPackageName.equals(topPackage)) {
                Logger.println("topPackage: " + topPackage);
                Logger.println(topActivity + " is out of package!");
                if (APPTraveler.remote.isHome()) {
                    Logger.println("Top activity is home!");
                    Logger.println("Travel end!");
                    APPTraveler.mIsEnd = true;
                    return null;
                }
            }
            if (level > 5) {
                Logger.println("updateOperations is over 5 times!");
                return newOperations;
            }
            ViewHelper.goBack("operations.size() == 0");
            return updateOperations(level + 1);
        }

        // add operations. It's dup!!!!
        for (Operation operation : operations) {
            // TODO use getValidViews() to avoid to add dup views' operation
            newOperations.add(operation);
        }

        return newOperations;
    }

    public static ArrayList<Operation> getCurrentOperations() {
        ArrayList<View> views = ViewHelper.getAllOperatableViews();
        ArrayList<Operation> operations = new ArrayList<Operation>();
        for (int i = 0; i < views.size(); i++) {
            operations.addAll(ViewHelper.getViewOperations(views.get(i), i));
        }
        return operations;
    }

    private boolean isSameDiff(ArrayList<View> diff, ArrayList<View> lastDiff) {
        return Util.isArraySame(diff, lastDiff, new Equal<View>() {

            @Override
            public boolean isEqual(View v1, View v2) {
                if (v1.getWidth() != v2.getWidth() || v1.getHeight() != v2.getHeight()) {
                    return false;
                }
                if (Util.getViewSnapshot(v1).equals(Util.getViewSnapshot(v2))) {
                    return true;
                }
                return false;
            }

        });
    }

    /**
     * TODO unfinished!!!
     * 
     * @param newViews
     * @return
     */
    //    private ArrayList<View> getValidViews(ArrayList<View> newViews) {
    //        ArrayList<View> diff = Util.subArray(newViews, mOldViews);
    //        ArrayList<View> validViews = new ArrayList<View>();
    //
    //        if (!isSameDiff(diff, mLastDiff)) {
    //            validViews = diff;
    //        }
    //
    //        mOldViews = newViews;
    //        mLastDiff = diff;
    //
    //        return validViews;
    //    }

}
