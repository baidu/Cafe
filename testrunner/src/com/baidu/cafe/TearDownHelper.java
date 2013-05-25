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

package com.baidu.cafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.view.KeyEvent;

import com.baidu.cafe.local.Log;
import com.baidu.cafe.remote.Armser;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-8-11
 * @version
 * @todo
 */
public class TearDownHelper {
    private Armser   mRemote         = null;
    private String[] mWindowsAtBirth = null;

    /**
     * This function must be invoke at setUp()
     * 
     * @param remote
     */
    public TearDownHelper(Armser remote) {
        mRemote = remote;
        mWindowsAtBirth = getWindowPackageName();
    }

    private void print(String msg) {
        Log.i("TearDownHelper", msg);
    }

    public void backToHome() {
        String focusedWindow = mRemote.getFocusedWindow();
        print("focusedWindow: " + focusedWindow);

        if (null == focusedWindow) {
            return;
        }

        if (!focusedWindow.contains("launcher")) {
            mRemote.pressKey(KeyEvent.KEYCODE_HOME);
            print("backToHome");
        }
    }

    /**
     * This function must be invoke once at tearDown()
     */
    public void killWindowsFromBirthToNow() {
        String[] mWindowsAtNow = getWindowPackageName();
        if (null == mWindowsAtNow) {
            print("null == mWindowsAtNow at killWindowsFromBirthToNow");
            return;
        }

        String[] newWindows = arrayUnique(subArray(mWindowsAtNow, mWindowsAtBirth));

        for (String window : newWindows) {
            print("kill:" + window);
            mRemote.killBackgroundProcesses(window);
        }
    }

    private String[] arrayUnique(String[] a) {
        List<String> list = new LinkedList<String>();

        for (int i = 0; i < a.length; i++) {
            if (!list.contains(a[i])) {
                list.add(a[i]);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    private String[] subArray(String[] bigArray, String[] smallArray) {
        List<String> bigArrayList = new ArrayList<String>(Arrays.asList(bigArray));

        for (String s : smallArray) {
            if (bigArrayList.contains(s)) {
                bigArrayList.remove(s);
            } else {
                bigArrayList.add(s);
            }
        }

        return bigArrayList.toArray(new String[bigArrayList.size()]);
    }

    private String[] getWindowPackageName() {
        String[] windowList = mRemote.getWindowList();
        if (null == windowList) {
            print("null == windowList at getWindowPackageName");
            return null;
        }

        String[] windowPackageName = new String[windowList.length];

        for (int i = 0; i < windowList.length; i++) {
            String[] windowSplit = windowList[i].split(" ");

            if (windowSplit.length < 2) {
                continue;
            }

            if (windowSplit[1].contains("/")) {
                windowPackageName[i] = windowSplit[1].substring(0, windowSplit[1].indexOf("/"));
            } else {
                windowPackageName[i] = windowSplit[1];
            }
        }

        return windowPackageName;
    }

}
