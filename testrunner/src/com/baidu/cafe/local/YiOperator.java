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

package com.baidu.cafe.local;

import java.util.ArrayList;

import com.baidu.cafe.local.PrivateOperator;

import junit.framework.Assert;
import android.app.Activity;
import android.app.Instrumentation;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import yi.app.BaiduAppTitle;
import yi.widget.ButtonGroup;
import yi.widget.PopupMenu;
import yi.widget.PopupSubMenu;
import yi.widget.Switcher;

/**
 * YI widgets operation lib only for CHUNLEI
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-9-22
 * @version
 * @todo
 */
public class YiOperator extends SoloEx {
    private Instrumentation mInstrumentation = null;

    public YiOperator(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);
        mInstrumentation = instrumentation;
    }

    private void print(String message) {
        Log.d("YiOperator", "" + message);
    }

    /**
     * Returns an ArrayList with the button groups located in the current
     * activity
     * 
     * @return ArrayList of the button groups contained in the current activity
     */
    public ArrayList<ButtonGroup> getCurrentButtonGroups() {
        ArrayList<ButtonGroup> buttonList = new ArrayList<ButtonGroup>();
        ArrayList<View> viewList = getViews();
        for (View view : viewList) {
            if (view instanceof yi.widget.ButtonGroup)
                buttonList.add((ButtonGroup) view);
        }
        return buttonList;
    }

    /**
     * This method returns a button group with a certain index.
     * 
     * @param index
     *            the index of the button group
     * @return the button group with the specific index
     * 
     */
    public ButtonGroup getButtonGroup(int index) {
        ArrayList<ButtonGroup> buttonGroupList = getCurrentButtonGroups();
        ButtonGroup buttonGroup = null;
        try {
            buttonGroup = buttonGroupList.get(index);
        } catch (Throwable e) {
        }
        return buttonGroup;
    }

    /**
     * Clicks on a button group with a certain item.
     * 
     * @param index
     *            the index number of button groups.
     * @param item
     *            the item of the button group that should be clicked on.
     */
    public void clickOnButtonGroup(int index, int item) {
        ButtonGroup buttonGroup = null;
        try {
            buttonGroup = getButtonGroup(index);
            if (buttonGroup == null) {
                Assert.assertTrue("ButtonGroup is null", false);
            }
            buttonGroup.setCheckedButton(item);
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue("item is not valid", false);
        }
    }

    /**
     * Returns a baiduAppTitle with a certain text in current activity
     * 
     * @param text
     *            name of baiduAppTitle in current activity
     * @return the baiduAppTitle with a certain text in current activity
     */
    public BaiduAppTitle getBaiduAppTitle() {
        BaiduAppTitle baiduAppTitle = null;
        Activity activity = this.getCurrentActivity();
        print("activity is " + activity);

        try {
            print("BaiduAppTitle is " + BaiduAppTitle.class.getName());
            ArrayList<String> names = PrivateOperator.getPropertyNameByType(activity, 0, BaiduAppTitle.class.getName());
            Assert.assertTrue("names length is not 1", 1 == names.size());
            Object object = PrivateOperator.getObjectProperty(activity, 0, names.get(0));
            baiduAppTitle = (BaiduAppTitle) object;
        } catch (SecurityException se) {
            se.printStackTrace();
        } catch (NoSuchFieldException ne) {
            ne.printStackTrace();
        } catch (IllegalArgumentException ie) {
            ie.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } finally {
            return baiduAppTitle;
        }
    }

    /**
     * click on rice title left button with a certain text in current activity
     * 
     */
    public void clickOnRiceTitleLeftButton() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button left = baiduAppTitle.getRiceTitleLeftButton();
        if (left == null) {
            Assert.assertTrue("left button is null", false);
        }
        clickOnView(left);
    }

    /**
     * click on rice title right button with a certain text in current activity
     * 
     */
    public void clickOnRiceTitleRightButton() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button right = baiduAppTitle.getRiceTitleRightButton();
        if (right == null) {
            Assert.assertTrue("right button is null", false);
        }
        clickOnView(right);
    }

    /**
     * Get rice title left button status with a certain text in current activity
     * 
     */
    public boolean isRiceTitleLeftButtonEnabled() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button left = baiduAppTitle.getRiceTitleLeftButton();
        if (left == null) {
            Assert.assertTrue("left button is null", false);
        }
        return left.isEnabled();
    }

    /**
     * Get rice title right button status with a certain text in current
     * activity
     * 
     */

    public boolean isRiceTitleRightButtonEnabled() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button right = baiduAppTitle.getRiceTitleRightButton();
        if (right == null) {
            Assert.assertTrue("right button is null", false);
        }
        return right.isEnabled();
    }

    /**
     * Returns a text on rice title left button in current activity.
     * 
     * 
     * @return the text on rice title left button in current activity.
     */
    public String getRiceTitleLeftButtonText() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button left = baiduAppTitle.getRiceTitleLeftButton();
        if (left == null) {
            Assert.assertTrue("left button is null", false);
        }
        return left.getText().toString();
    }

    /**
     * Returns a text on rice title Right button in current activity.
     * 
     * 
     * @return the text on rice title Right button in current activity.
     */
    public String getRiceTitleRightButtonText() {
        BaiduAppTitle baiduAppTitle = getBaiduAppTitle();
        if (baiduAppTitle == null) {
            Assert.assertTrue("baiduAppTitle is null", false);
        }
        Button right = baiduAppTitle.getRiceTitleRightButton();
        if (right == null) {
            Assert.assertTrue("right button is null", false);
        }
        return right.getText().toString();
    }

    /**
     * Get the text of RiceTile in the current activity.
     * 
     * @return the text of RiceTile in the current activity.
     */
    public String getRiceTitleText() {
        return getCurrentActivity().getTitle().toString();
    }

    /**
     * Returns a popUpMenu with a certain text in current activity
     * 
     * @param activity
     *            current activity
     * @param text
     *            name of popUpMenu in current activity
     * @return the pupUpMenu with a certain text in current activity
     */
    public LinearLayout getPopupMenu(Activity activity, String text) {
        LinearLayout popupMenu = null;
        try {
            Object object = PrivateOperator.getObjectProperty(activity, 0, text);
            if (object instanceof PopupMenu) {
                print("PopupMenu");
                popupMenu = (LinearLayout) PrivateOperator.getObjectProperty((PopupMenu) object, 0, "mPopupMenu");
            } else if (object instanceof PopupSubMenu) {
                print("PopupSubMenu");
                popupMenu = (LinearLayout) PrivateOperator.getObjectProperty((PopupSubMenu) object, 0, "mPopupMenu");
            } else {
                print("Unknown type of popupMenu!");
            }
        } catch (SecurityException se) {
            se.printStackTrace();
        } catch (NoSuchFieldException ne) {
            ne.printStackTrace();
        } catch (IllegalArgumentException ie) {
            ie.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } finally {
            return popupMenu;
        }
    }

    /**
     * Returns a button in a pupUpMenu with a certain index in current activity
     * 
     * @param activity
     *            current activity
     * @param text
     *            name of popUpMenu in current activity
     * @param index
     *            button's position in the popUpMenu
     * @return the button in the pupUpMenu with a certain index in current
     *         activity
     */
    public Button getPopupMenuButton(Activity activity, String text, int index) {
        LinearLayout popupMenu = getPopupMenu(activity, text);
        Assert.assertTrue("popupMenu is null", popupMenu != null);
        if (!(popupMenu.getChildAt(0) instanceof Button)) {
            index++;
        }
        Button item = null;
        try {
            item = (Button) popupMenu.getChildAt(index);
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue("Index is not valid", false);
        }
        return item;
    }

    /**
     * Returns a string on the button of popUpMenu with a certain index in
     * current activity
     * 
     * @param activity
     *            current activity
     * @param text
     *            name of popUpMenu in current activity
     * @param index
     *            button's position in the popUpMenu
     * @return the string on the button of popuUpMenu with a certain index in
     *         current activity
     */
    public String getPopupMenuButtonText(Activity activity, String text, int index) {
        Button item = getPopupMenuButton(activity, text, index);
        if (item == null) {
            return null;
        }
        return item.getText().toString();
    }

    /**
     * click the button of popUpMenu with a certain index in current activity
     * 
     * @param activity
     *            current activity
     * @param menuString
     *            name of popUpMenu in current activity
     * @param index
     *            button's position in the popUpMenu
     */
    public void clickPopupMenuButton(Activity activity, String text, int index) {
        Button item = null;
        try {
            item = getPopupMenuButton(activity, text, index);
            if (item == null) {
                Assert.assertTrue("popupmenu button is null", false);
            }
            clickOnView(item);
        } catch (IndexOutOfBoundsException e) {
            Assert.assertTrue("Index is not valid", false);
        }
    }

    /**
     * set a switcher to the special status
     * 
     * @param index
     *            the index of switcher, from 0
     * @param checked
     *            the status of switcher
     * @return true means operation succeed; false means index does not exist
     */
    public boolean setSwitcherChecked(int index, boolean checked) {
        ArrayList<Switcher> switchers = getCurrentViews(Switcher.class);
        if (index < switchers.size()) {
            final Switcher switcher = switchers.get(index);
            final boolean fChecked = checked;
            mInstrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    switcher.setChecked(fChecked);
                }
            });
            return true;
        }

        return false;
    }

    /**
     * get status of special switcher
     * 
     * @param index
     *            the index of switcher, from 0
     * @return the status of switcher
     */
    public boolean getSwitcherChecked(int index) {
        ArrayList<Switcher> switchers = getCurrentViews(Switcher.class);

        if (index > switchers.size()) {
            print("index:" + index + "> switchers.size():" + switchers.size());
            return false;
        }

        return switchers.get(index).isChecked();
    }

}
