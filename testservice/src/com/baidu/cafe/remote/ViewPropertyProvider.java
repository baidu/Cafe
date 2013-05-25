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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Dump view property from view server. It works when "adb shell getprop
 * ro.debuggable" returns 1.
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-7-6
 * @version
 * @todo
 */
public class ViewPropertyProvider {
    public final static int           SEARCHMODE_COMPLETE_MATCHING = 1;
    public final static int           SEARCHMODE_DEFAULT           = 1;
    public final static int           SEARCHMODE_INCLUDE_MATCHING  = 2;

    private final static int          DUMP_TIMEOUT                 = 30000;
    private final static int          VIEWSERVER_PORT              = 4939;

    private int                       mHeight                      = 0;
    private int                       mHeightOfStatusBar           = 0;
    private BufferedReader            mIn                          = null;
    private ArrayList<BufferedReader> mIns                         = null;
    private ArrayList<String>         mDumpedLines                 = null;
    private BufferedWriter            mOut                         = null;
    private int                       mRootHeight                  = 0;
    private String                    mRootLayoutType              = null;
    private String                    mRootLayoutWidth             = null;
    private int                       mRootWidth                   = 0;
    private Socket                    mSocket                      = null;
    private SystemLib                 mSystemLib                   = null;
    private ArrayList<Integer>        mTargetIndexes               = null;
    private int                       mThreadNumber                = 0;
    private int                       mWidth                       = 0;

    public ViewPropertyProvider(SystemLib systemLib) {
        mSystemLib = systemLib;
        mIns = new ArrayList<BufferedReader>();
        init();
        // for test git
    }

    public void setStatusBarHeight(int height) {
        mHeightOfStatusBar = height;
        Log.print("mHeightOfStatusBar:" + mHeightOfStatusBar);
    }

    /**
     * check whether the given process alive or not
     * 
     * @param processName
     *            the name string of process e.g.
     *            "com.baidu.baiduclock/com.baidu.baiduclock.BaiduClock"
     * @return true:alive false:dead
     */
    public boolean checkProcessAlive(String processName) {
        String processNumber = getProcessNumber(processName);
        if (processNumber == null) {
            return false;
        }
        return true;
    }

    /**
     * get focused window name
     * 
     * @return the name string of focused window
     */
    public String getFocusedWindow() {
        dumpTargetViews("GET_FOCUS", "", "", SEARCHMODE_COMPLETE_MATCHING, 1, true, false);

        if (mDumpedLines.size() != 1) {
            return null;
        }

        Log.print("GET_FOCUS:" + mDumpedLines.get(0));
        /*
        String[] focusSplit = mKnownLines.get(0).split(" ");
        
        if (focusSplit.length < 2){
            print("focusSplit.length < 2");
            return null;
        }*/

        //return focusSplit[1];
        return mDumpedLines.get(0);
    }

    /**
     * get input method status
     * 
     * @return true means VISIBLE; false means GONE
     */
    public boolean getInputMethodStatus() {
        String processNumber = getProcessNumber("InputMethod");
        if (null == processNumber) {
            Log.print("Cannot found InputMethod!\nUse default status[false] of InputMethod!");
            return false;
        }

        ArrayList<String> targetLines = dumpTargetViews("DUMP " + processNumber, "layout_type",
                "TYPE_INPUT_METHOD", SEARCHMODE_INCLUDE_MATCHING, 1, true, false);
        if (null == targetLines || targetLines.size() == 0) {
            return false;
        }

        return isVisible(targetLines.get(0));
    }

    /**
     * get icon state on statusbar
     * 
     * @param slotName
     *            mSlot of StatusBarIconView e.g. 3G icon's name is
     *            data_connection
     * @return state of icon; true means VISIBLE, false means GONE
     */
    public boolean getStatusBarIconState(String slotName) {
        String processNumber = getProcessNumber("StatusBar");
        ArrayList<String> targetLines = dumpTargetViews("DUMP " + processNumber, "name",
                "StatusBarIconView", SEARCHMODE_COMPLETE_MATCHING, 20, true, false);

        for (String targetLine : targetLines) {
            String sName = getPropertyValue(targetLine, "mSlot");
            // Log.print("sName:" + sName);
            if (null == sName) {
                continue;
            }

            if (sName.contains(slotName)) {
                return isVisible(targetLine);
            }
        }
        Log.print("iconName[" + slotName + "] has not found!");

        return false;
    }

    /**
     * get view's properties from focused window
     * 
     * @param searchKey
     *            property's name use to search
     * @param searchValue
     *            property's value use to search
     * @param targetNumber
     *            target index in search result. Beacuse a pair of searchKey and
     *            searchValue can return not only one search result.
     * @param getKeys
     *            property's name use to get
     * @param getNew
     *            True means start a new dump. If UI has changed, use true.
     *            False means get propery from last dump. If UI has not changed,
     *            use false.
     * 
     * @return String[] getValues
     */
    public String[] getViewProperties(String searchKey, String searchValue, int searchMode,
            int targetNumber, String[] getKeys, boolean getNew) {
        ArrayList<String[]> viewsProperties = getViewsProperties(searchKey, searchValue,
                searchMode, targetNumber, getKeys, getNew, false);
        return viewsProperties.size() < targetNumber ? null : viewsProperties.get(targetNumber - 1);
    }

    /**
     * get views' properties according to key, value and mode
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
     * @param getKeys
     *            what the keys u wanna to get its properties
     * @param getNew
     *            true if the view is already dumped, else false
     * @param onlyVisible
     *            true if u just wanna dump visible views
     * @return all of view properties dumped
     */
    public ArrayList<String[]> getViewsProperties(String searchKey, String searchValue,
            int searchMode, int targetNumber, String[] getKeys, boolean getNew, boolean onlyVisible) {
        if (null == searchKey || searchKey.equals("") || null == searchValue || null == getKeys
                || targetNumber < 1) {
            Log.print("getViewProperties()'s arguments is not correct!");
            return null;
        }

        setCurrentXY();

        // get target lines
        // Log.print("searchKey:" + searchKey + "\nsearchValue:" + searchValue + "\ntargetNumber:" + targetNumber);
        ArrayList<String> targetLines = dumpTargetViews("DUMP -1", searchKey, searchValue,
                searchMode, targetNumber, getNew, onlyVisible);
        for (String line : targetLines) {
            Log.print("target line:" + line);
        }

        // get properties
        ArrayList<String[]> targets = new ArrayList<String[]>();

        for (int i = 0; i < targetLines.size(); i++) {
            String[] properties = new String[getKeys.length];
            for (int j = 0; j < getKeys.length; j++) {
                if (getKeys[j].equals("coordinate")) {
                    properties[j] = getAbsoluteCoordinates(mTargetIndexes.get(i));
                    // Log.print(properties[j]);
                    continue;
                }
                properties[j] = getPropertyValue(targetLines.get(i), getKeys[j]);
            }

            targets.add(properties);

            //            for (String property : properties) {
            //                Log.print(property);
            //            }
        }

        return targets;
    }

    /**
     * get the current window list
     * 
     * @return the current window list
     */
    public String[] getWindowList() {
        dumpTargetViews("LIST", "", "", SEARCHMODE_COMPLETE_MATCHING, 1, true, false);
        return mDumpedLines.toArray(new String[mDumpedLines.size()]);
    }

    /**
     * wait for all dumping in threads completing It only can be invoked once in
     * a process!
     */
    public void waitForAllDumpCompleted() {
        Log.print("Wait for all dumping...");
        String line;
        Long begin = System.currentTimeMillis();
        Boolean isTimeout = false;

        for (BufferedReader in : mIns) {
            if (isTimeout) {
                // tolerate 30s dump, and then it will exit that will cause socket exception in logcat at w level
                break;
            }

            if (null == in) {
                continue;
            }

            try {
                while (true) {
                    if (System.currentTimeMillis() - begin > DUMP_TIMEOUT) {
                        isTimeout = true;
                        Log.print("Dump time is over than DUMP_TIMEOUT!\nSocket to ViewServer will be closed And it will throw [java.net.SocketException: Broken pipe].");
                        break;
                    }

                    if ((line = in.readLine()) == null || "DONE.".equalsIgnoreCase(line)
                            || "DONE".equalsIgnoreCase(line)) {
                        //Log.print("break@wait:" + line);
                        break;
                    }
                    //Log.print("wait:" + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*
               try {
               in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            */
        }

        try {
            /*
            if (mOut != null) {
                mOut.close();
            }
            
            if (mIn != null) {
                mIn.close();
            }
            */
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.print("All dump has completed. It cost " + (System.currentTimeMillis() - begin) + "ms");
    }

    /**
     * format the dumped tree lines to its level
     * 
     * @param lines
     *            the dumped tree lines
     * @return the dumped tree level such as 0.1.3.4
     */
    private String[] formatDumpedLevels(ArrayList<String> lines) {
        String[] newLines = new String[lines.size()];

        // change the first line
        String levelString = "0";
        newLines[0] = levelString;

        // change other lines
        for (int i = 1; i < lines.size(); i++) {
            // select current head from head
            StringBuffer levelBuf = new StringBuffer();
            String[] levelSplit = levelString.split("\\.");
            int level = countFrontWhitespace(lines.get(i));
            for (int j = 0; j < level; j++) {
                levelBuf.append(levelSplit[j]).append('.');
            }
            String currentLevel = levelBuf.toString();
            levelString = currentLevel + i;
            newLines[i] = levelString;
            // Log.print(newLines[i]);
        }

        return newLines;
    }

    /**
     * open a new thread to complete remaining dumping job.
     */
    private void completeRemainingDump() {
        final int index = mThreadNumber;
        new Thread(new Runnable() {
            public void run() {
                BufferedReader in = mIns.get(index);
                String line;
                try {
                    while (true) {
                        if ((line = in.readLine()) == null || "DONE.".equalsIgnoreCase(line)
                                || "DONE".equalsIgnoreCase(line)) {
                            // Log.print("break:" + line);
                            break;
                        }
                        // Log.print(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "completeRemainingDump").start();
        mThreadNumber++;
    }

    /**
     * count the number of the white space of the given line
     * 
     * @param line
     *            the dumped line to be searched
     * @return the number of the white space
     */
    private int countFrontWhitespace(String line) {
        int count = 0;
        while (line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /**
     * search the given line according to given key, value and mode.
     * 
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            dumping mode including SEARCHMODE_PARTIAL_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param line
     *            the dumped line to be searched
     * @return if found, return true; else false
     */
    private boolean findTarget(String searchKey, String searchValue, int searchMode, String line) {
        if ("name".equals(searchKey)) {
            if (searchValue.equals(getName(line))) {
                return true;
            }
        } else {
            String getValue = getPropertyValue(line, searchKey);
            if (null == getValue) {
                return false;
            }
            return isMatch(searchValue, getValue, searchMode);
        }

        return false;
    }

    private String getAbsoluteCoordinates(int viewIndex) {
        String[] newLines = formatDumpedLevels(mDumpedLines);
        int[] relativeCoordinates = new int[4];
        int x = 0;
        int y = 0;

        String[] fathersLevel = newLines[viewIndex].split("\\.");
        for (int j = 0; j < fathersLevel.length; j++) {
            // Log.print("at level " + new Integer(fathersLevel[j]));
            relativeCoordinates = getRelativeCoordinates(mDumpedLines.get(
                    new Integer(fathersLevel[j])).trim());

            // accumulate relative coordinates
            if (relativeCoordinates != null) {
                // x += left - mScrollX
                x += relativeCoordinates[0] - relativeCoordinates[2];
                // y += top - mScrollY
                y += relativeCoordinates[1] - relativeCoordinates[3];
            }
        }
        x += relativeCoordinates[2];
        y += relativeCoordinates[3];

        String widthString = getPropertyValue(
                mDumpedLines.get(new Integer(fathersLevel[fathersLevel.length - 1])).trim(),
                "getWidth()");
        String heightString = getPropertyValue(
                mDumpedLines.get(new Integer(fathersLevel[fathersLevel.length - 1])).trim(),
                "getHeight()");
        if (heightString == null || widthString == null) {
            return null;
        }
        int width = new Integer(widthString);
        int height = new Integer(heightString);

        parseRoot();

        Log.print("[mRootHeight,mRootWidth]:[" + mRootHeight + "," + mRootWidth + "]");
        /*
        if (isFullScreen(mKnownLines.get(0))) {
            if (isExpandFullScreen(mKnownLines.get(0))) {
                // expand absoluteCoordinates to full screen
                Log.print("expand to full screen");
                float scale = (float) mHeight / (float) mRootHeight;
                Log.print("scale:" + scale);
                x *= scale;
                y *= scale;
                width *= scale;
                height *= scale;
            }
        } else {
            // add non full screen window height offset
            y += getNonFullScreenWindowHeightOffset();
        }
        */

        if (!isFullScreen()) {
            // add non full screen window height offset
            y += getNonFullScreenWindowHeightOffset();
        }
        Log.print("x = " + x);
        Log.print("y = " + y);
        Log.print("width = " + width);
        Log.print("height = " + height);

        return "" + x + "," + y + "," + width + "," + height;
    }

    /**
     * set X value and Y value of the current coordinates,
     */
    private void setCurrentXY() {
        mHeight = mSystemLib.getDisplayY();
        mWidth = mSystemLib.getDisplayX();
        Log.print("mHeight:" + mHeight);
        Log.print("mWidth:" + mWidth);
    }

    /**
     * get the height of the input method.
     * 
     * @return the height of the input method
     */
    private int dumpInputMethodHeight() {
        String processNumber = getProcessNumber("InputMethod");

        if (processNumber != null) {
            ArrayList<String> targetLines = dumpTargetViews("DUMP " + processNumber, "layout_type",
                    "TYPE_INPUT_METHOD", SEARCHMODE_INCLUDE_MATCHING, 1, true, false);
            int heightOfInputMethod = Integer.valueOf(getPropertyValue(targetLines.get(0),
                    "getHeight()"));
            if (heightOfInputMethod > 0) {
                mSystemLib.setSystemProperties("persist.sys.inputmethod.h", ""
                        + heightOfInputMethod);
                return heightOfInputMethod;
            }
        }

        return 0;
    }

    private int getLengthNumberWidth(String line, int beginIndex) {
        int length = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.substring(beginIndex + i, beginIndex + i + 1).equals(",")) {
                length = i;
                break;
            }
        }
        return length;
    }

    /**
     * search views from dumped lines according to key, value and search mode.
     * 
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param targetNumber
     * @param onlyVisible
     * @return lines which are selected
     */
    private ArrayList<String> searchDumpedLines(String searchKey, String searchValue,
            int searchMode, int targetNumber, boolean onlyVisible) {
        ArrayList<String> targetLines = new ArrayList<String>();
        if (null == mDumpedLines || mDumpedLines.size() == 0) {
            Log.print("mDupmedLines is empty!\nThere is no dump to find.");
            return null;
        }

        for (int i = 0; i < mDumpedLines.size(); i++) {
            String line = mDumpedLines.get(i);
            if (findTarget(searchKey, searchValue, searchMode, line)) {
                if (onlyVisible && !isFamilyVisible(i)) {
                    Log.print("invisible line: " + line);
                    continue;
                }

                Log.print("old: " + searchKey + " == " + searchValue + " at [" + line + "]");
                targetLines.add(line);
                mTargetIndexes.add(i);

                if (targetLines.size() == targetNumber) {
                    break;
                }
            }
        }

        return targetLines;
    }

    public static boolean isViewServerOpen() {
        boolean open = true;
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", VIEWSERVER_PORT));
        } catch (IOException e) {
            //            e.printStackTrace();
            open = false;
        } finally {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return open;
    }

    /**
     * @return
     */
    public ArrayList<String> dumpAllLines() {
        ArrayList<String> lines = new ArrayList<String>();
        Long begin = System.currentTimeMillis();
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress("127.0.0.1", VIEWSERVER_PORT));
            mOut = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "utf-8"));

            // send command
            mOut.write("DUMP -1");
            mOut.newLine();
            mOut.flush();

            while (true) {
                String line = null;
                if ((line = mIn.readLine()) == null || "DONE.".equalsIgnoreCase(line)) {
                    break;
                }
                lines.add(line);
                //                Log.print(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.print("Dump time cost: " + (System.currentTimeMillis() - begin) + "ms");
        return lines;
    }

    /**
     * dump lines from view server by the given dumping command
     * 
     * @param command
     *            the dumping command
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            the dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param targetNumber
     * @param onlyVisible
     * @return the lines dumped
     */
    private ArrayList<String> dumpLinesFromViewServer(String command, String searchKey,
            String searchValue, int searchMode, int targetNumber, boolean onlyVisible) {
        // init
        mDumpedLines = new ArrayList<String>();
        ArrayList<String> targetLines = new ArrayList<String>();
        boolean isCompleted = false;

        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress("127.0.0.1", VIEWSERVER_PORT));
            mOut = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream(), "utf-8"));

            // send command
            mOut.write(command);
            mOut.newLine();
            mOut.flush();

            Long begin = System.currentTimeMillis();
            while (true) {
                String line;

                if ((line = mIn.readLine()) == null || "DONE.".equalsIgnoreCase(line)) {
                    isCompleted = true;
                    break;
                }

                //Log.print(line);
                mDumpedLines.add(line);
                int viewIndex = Integer.valueOf(mDumpedLines.size() - 1);
                // got targets already
                if (findTarget(searchKey, searchValue, searchMode, line)) {
                    if (onlyVisible && !isFamilyVisible(viewIndex)) {
                        Log.print("invisible line: " + line);
                        continue;
                    }

                    Log.print("new: " + searchKey + " == " + searchValue + " at [" + line + "]");
                    targetLines.add(line);
                    mTargetIndexes.add(viewIndex);

                    if (targetLines.size() == targetNumber) {
                        break;
                    }
                }
            }

            if (!isCompleted) {
                // save socket
                mIns.add(mIn);
                completeRemainingDump();
            }

            Log.print("Dump time cost: " + (System.currentTimeMillis() - begin) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return targetLines;
    }

    /**
     * get last property from dumped line e.g.
     * com.android.calculator2.ColorButton@43e49bf8, then ColorButtor will be
     * returned
     * 
     * @param line
     *            the dumped line to be searched
     * @return the last property
     */
    private String getName(String line) {
        String fullName = line.substring(0, line.indexOf("@"));
        String[] names = fullName.split("\\.");
        return names[names.length - 1];
    }

    private int getNonFullScreenWindowHeightOffset() {
        int heightOffset = 0;

        if (mRootHeight < mHeight || mRootWidth < mWidth) {
            if (("TYPE_BASE_APPLICATION".equals(mRootLayoutType) && "WRAP_CONTENT"
                    .equals(mRootLayoutWidth))
                    || "TYPE_SYSTEM_ALERT".equals(mRootLayoutType)
                    || ("TYPE_APPLICATION".equals(mRootLayoutType) && !"MATCH_PARENT"
                            .equals(mRootLayoutWidth))) {
                // middle of screen
                // some dialogs are TYPE_APPLICATION, some are TYPE_BASE_APPLICATION(such as dialog of launcher)
                // and all of full screen activity is TYPE_BASE_APPLICATION
                heightOffset = (mHeight - mHeightOfStatusBar - mRootHeight) / 2
                        + mHeightOfStatusBar;

                // get input method state
                if (getInputMethodStatus()) {
                    heightOffset = (mHeight - mHeightOfStatusBar - getInputMethodHeight() - mRootHeight)
                            / 2 + mHeightOfStatusBar;
                }
                Log.print("middle of screen");
            } else if ("TYPE_APPLICATION_PANEL".equals(mRootLayoutType)
                    || "TYPE_APPLICATION_ATTACHED_DIALOG".equals(mRootLayoutType)
                    || ("TYPE_APPLICATION".equals(mRootLayoutType) && "MATCH_PARENT"
                            .equals(mRootLayoutWidth))) {
                // bottom of screen
                // heightOffset = mHeight - mHeightOfStatusBar - height;
                heightOffset = mHeight - mRootHeight;
                Log.print("bottom of screen");
            } else if ("TYPE_APPLICATION_SUB_PANEL".equals(mRootLayoutType)
                    || ("TYPE_BASE_APPLICATION".equals(mRootLayoutType) && "MATCH_PARENT"
                            .equals(mRootLayoutWidth))) {
                // top of screen
                heightOffset = 0;
                Log.print("top of screen");
            } else if ("TYPE_STATUS_BAR_PANEL".equals(mRootLayoutType)
                    || "TYPE_KEYGUARD".equals(mRootLayoutType)) {
                // at status bar expanded or keyguard
                heightOffset = mHeightOfStatusBar;
                Log.print("at status bar expanded or keyguard");
            } else {
                Log.print("Unknown type appeared!");
            }
            Log.print("heightOffset:" + heightOffset);
        }

        return heightOffset;
    }

    /**
     * get the process number by the given process name
     * 
     * @param processName
     *            which process number to be queried
     * @return the process number
     */
    private String getProcessNumber(String processName) {
        dumpTargetViews("LIST", "", "", SEARCHMODE_COMPLETE_MATCHING, 1, true, false);
        for (String line : mDumpedLines) {
            String[] lineSplit = line.split(" ");
            if (null == lineSplit || lineSplit.length < 2) {
                Log.print("null == lineSplit || lineSplit.length < 2");
                return null;
            }
            if (processName.equals(lineSplit[1])) {
                return lineSplit[0];
            }
        }
        return null;
    }

    /**
     * get the value from the given line according to the given key
     * 
     * @param searchKey
     *            key property to be dumped
     * @param line
     *            the dumped line to be searched
     * @return the matched value
     */
    private String getPropertyValue(String line, String searchKey) {
        try {
            if (searchKey.length() != 0 && line.indexOf(searchKey + "=") != -1) {
                int beginIndex = line.indexOf(searchKey) + searchKey.length() + 1;
                int endIndex = beginIndex + getLengthNumberWidth(line, beginIndex);
                int length = new Integer(line.substring(beginIndex, endIndex));
                beginIndex = endIndex + 1;
                endIndex = beginIndex + length;
                return line.substring(beginIndex, endIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Log.print(property + " has not be found at " + line);
        return null;
    }

    /**
     * get the relative coordinates of the given line
     * 
     * @param line
     *            the line to be calculated
     * @return the relative coordinates
     */
    private int[] getRelativeCoordinates(String line) {
        String property = null;
        int[] relativeCoordinates = new int[4];

        property = getPropertyValue(line, "mLeft");
        if (property == null) {
            return null;
        }
        relativeCoordinates[0] = new Integer(property);

        property = getPropertyValue(line, "mTop");
        if (property == null) {
            return null;
        }
        relativeCoordinates[1] = new Integer(property);

        property = getPropertyValue(line, "mScrollX");
        if (property == null) {
            return null;
        }
        relativeCoordinates[2] = new Integer(property);

        property = getPropertyValue(line, "mScrollY");
        if (property == null) {
            return null;
        }
        relativeCoordinates[3] = new Integer(property);

        return relativeCoordinates;
    }

    /**
     * dump target views, if already dumped, just return; else dump it from view
     * server
     * 
     * @param command
     *            dumping command
     * @param searchKey
     *            key property to be dumped
     * @param searchValue
     *            key value to be dumped
     * @param searchMode
     *            dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @param targetNumber
     *            the amount of views you wanna to dump
     * @param getNew
     *            true if the view is already dumped, else false
     * @param onlyVisible
     *            true if u just wanna dump visible views
     * @return all of dumped views
     */
    private ArrayList<String> dumpTargetViews(final String command, final String searchKey,
            final String searchValue, final int searchMode, final int targetNumber,
            final boolean getNew, final boolean onlyVisible) {
        mTargetIndexes = new ArrayList<Integer>();

        // getViewLines from dumped lines
        if (!getNew) {
            return searchDumpedLines(searchKey, searchValue, searchMode, targetNumber, onlyVisible);
        }

        return dumpLinesFromViewServer(command, searchKey, searchValue, searchMode, targetNumber,
                onlyVisible);
    }

    /**
     * adjust whether the searched key has its value
     * 
     * @param key
     *            the key to be searched
     * @return true if the key has its value
     */
    private boolean hasValue(String key) {
        String value = mSystemLib.getSystemProperties(key);
        return (null == value || value.equals("") || Integer.valueOf(value) == 0) ? false : true;
    }

    /**
     * init process include: 1. set the height of the status bar
     */
    private void init() {
        // set by setStatusBarHeight()
        //        mHeightOfStatusBar = mSystemLib.getStatusBarHeight();
        Log.print("mHeightOfStatusBar:" + mHeightOfStatusBar);
    }

    private int getInputMethodHeight() {
        int heightOfInputMethod;
        if (hasValue("persist.sys.inputmethod.h")) {
            heightOfInputMethod = Integer.valueOf(mSystemLib
                    .getSystemProperties("persist.sys.inputmethod.h"));
        } else {
            heightOfInputMethod = dumpInputMethodHeight();
            mSystemLib.setSystemProperties("persist.sys.inputmethod.h", "" + heightOfInputMethod);
        }
        Log.print("heightOfInputMethod:" + heightOfInputMethod);

        return heightOfInputMethod;
    }

    /**
     * adjust whether the family is visible of the dumped tree lines
     * 
     * @param viewIndex
     *            the index to get father level
     * @return true if family is visible, else false
     */
    private boolean isFamilyVisible(int viewIndex) {
        String[] dumpedLevels = formatDumpedLevels(mDumpedLines);
        String[] fathersLevel = dumpedLevels[viewIndex].split("\\.");

        for (int j = 0; j < fathersLevel.length; j++) {
            String fatherLine = mDumpedLines.get(new Integer(fathersLevel[j])).trim();
            if (!isVisible(fatherLine)) {
                return false;
            }
        }

        return true;
    }

    /**
     * adjust if the screen is full
     * 
     * @return true if full screen, else false
     */
    private boolean isFullScreen() {
        return (mRootHeight < mHeight || mRootWidth < mWidth) ? false : true;
    }

    /**
     * adjust whether the got value and to be searched value are matched
     * 
     * @param searchValue
     *            key value to be dumped
     * @param getValue
     *            the already got value
     * @param searchMode
     *            dumping mode including SEARCHMODE_INCLUDE_MATCHING,
     *            SEARCHMODE_COMPLETE_MATCHING
     * @return true if matched, else false
     */
    private boolean isMatch(String searchValue, String getValue, int searchMode) {
        switch (searchMode) {
        case SEARCHMODE_COMPLETE_MATCHING:
            return searchValue.equals(getValue) ? true : false;
        case SEARCHMODE_INCLUDE_MATCHING:
            return getValue.indexOf(searchValue) != -1 ? true : false;
        default:
            Log.print("Unknown type of SEARCHMODE");
            return false;
        }
    }

    // landscape
    private boolean isPortrait() {
        setCurrentXY();
        return mHeight > mWidth;
    }

    /**
     * adjust whether the given line is visible
     * 
     * @param line
     *            the dumped line to be searched
     * @return true if visible, else false
     */
    private boolean isVisible(String line) {
        return "VISIBLE".equals(getPropertyValue(line, "getVisibility()")) ? true : false;
    }

    /**
     * parse root dumped line including its height, width, layout type and
     * layout width.
     */
    private void parseRoot() {
        String rootView = mDumpedLines.get(0);
        mRootHeight = Integer.valueOf(getPropertyValue(rootView, "getHeight()"));
        mRootWidth = Integer.valueOf(getPropertyValue(rootView, "getWidth()"));
        mRootLayoutType = getPropertyValue(rootView, "layout_type");
        mRootLayoutWidth = getPropertyValue(rootView, "layout_width");
        Log.print("layoutType:" + mRootLayoutType);
        Log.print("mRootLayoutWidth:" + mRootLayoutWidth);
    }

}
