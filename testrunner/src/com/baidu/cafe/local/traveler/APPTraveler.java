/*
         _          __________                              _,
     _.-(_)._     ."          ".      .--""--.          _.-{__}-._
   .'________'.   | .--------. |    .'        '.      .:-'`____`'-:.
  [____________] /` |________| `\  /   .'``'.   \    /_.-"`_  _`"-._\
  /  / .\/. \  \|  / / .\/. \ \  ||  .'/.\/.\'.  |  /`   / .\/. \   `\
  |  \__/\__/  |\_/  \__/\__/  \_/|  : |_/\_| ;  |  |    \__/\__/    |
  \            /  \            /   \ '.\    /.' / .-\                >/-.
  /'._  --  _.'\  /'._  --  _.'\   /'. `'--'` .'\/   '._-.__--__.-_.'
\/_   `""""`   _\/_   `""""`   _\ /_  `-./\.-'  _\'.    `""""""""`'`\
(__/    '|    \ _)_|           |_)_/            \__)|        '        
  |_____'|_____|   \__________/|;                  `_________'________`;-'
  s'----------'    '----------'   '--------------'`--------------------`
     S T A N    L I A N G Y U J U N   K E N N Y        L U X I A O Y U

 */

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

import junit.framework.Assert;
import android.view.View;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.remote.Armser;
import com.baidu.cafe.utils.TreeNode;
import com.baidu.cafe.utils.TreeNode.NodeCallBack;

/**
 * travel app's operation path
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-11-28
 * @version
 * @todo
 */
public class APPTraveler {
    public final static int           MODE_PREORDER                     = 0;
    public final static int           MODE_BREADTH_FIRST                = 1;
    public final static int           MODE_ACTIVITY                     = 2;
    public final static int           MODE_GRID                         = 3;
    public final static int           TRAVEL_MODE                       = MODE_PREORDER;

    public static TreeNode<Operation> mRoot                             = null;
    public static boolean             mCrashBeforeTravel                = true;
    public static boolean             mIsEnd                            = false;
    public static int                 mDisplayX                         = 0;
    public static int                 mDisplayY                         = 0;
    public static Armser              remote                            = null;
    public static LocalLib            local                             = null;
    public static String              mUsername                         = null;
    public static String              mPassword                         = null;

    private final static int          LOOP_MAX_LENGTH                   = 100;
    private final static int          LOOP_MIN_LENGTH                   = 4;
    private final static int          TIMEOUT_WAIT_FOR_SCREEN_SHOT      = 1000 * 5;
    private final static int          TIMEOUT_WAIT_FOR_LAUNCH_COMPLETED = 1000 * 10;
    private final static int          TRAVEL_TIME_OUT                   = 1000 * 60 * 30;
    private final static int          MAX_TRAVEL_DEPTH                  = 3;
    private final static int          RET_PASS                          = 0;
    private final static int          RET_NOT_ENABLE                    = 1;
    private final static int          RET_SHOULD_NOT_REPEAT             = 2;
    private final static int          RET_NOT_UNDOEN                    = 3;
    private final static int          RET_NOT_AVAILABLE                 = 4;

    private static int                mScreenShotCounter                = 1;
    private long                      mEnd                              = 0;
    private ArrayList<Operation>      mAllOperations                    = new ArrayList<Operation>();
    private ArrayList<Operation>      mOldOperations                    = new ArrayList<Operation>();

    public APPTraveler(Armser r, LocalLib l, String username, String password) {
        local = l;
        remote = r;
        mUsername = username;
        mPassword = password;
        mEnd = System.currentTimeMillis() + TRAVEL_TIME_OUT;
        mDisplayX = (int) local.getDisplayX();
        mDisplayY = (int) local.getDisplayY();
        init();
    }

    private void init() {
        TreeNode.vertical = remote.getStringByName("vertical");
        TreeNode.horizontal = remote.getStringByName("horizontal");
        TreeNode.big_horizontal = remote.getStringByName("big_horizontal");
        TreeNode.vertical_T = remote.getStringByName("vertical_T");
        TreeNode.horizontal_T = remote.getStringByName("horizontal_T");
        TreeNode.left = remote.getStringByName("left");
        ViewHelper.mBlackList.add(remote.getStringByName("update"));
        ViewHelper.mBlackList.add(remote.getStringByName("download"));
        ViewHelper.mBlackList.add(remote.getStringByName("update_immediately"));
        ViewHelper.mBlackList.add(remote.getStringByName("download_immediately"));
        //local.traceFPS();
    }

    public void travel(int depth) {
        long end = System.currentTimeMillis() + TIMEOUT_WAIT_FOR_LAUNCH_COMPLETED;
        while (remote.getTopActivity().indexOf(CafeTestCase.mActivityClass.getName()) == -1) {
            local.sleep(1000);
            Logger.println("wait for " + CafeTestCase.mActivityClass.getName());
            if (System.currentTimeMillis() > end) {
                Logger.println("TIMEOUT_WAIT_FOR_LAUNCH_COMPLETED");
                break;
            }
        }

        mCrashBeforeTravel = false;
        // prepare root node
        mRoot = newEmptyTreeNode();
        updateNodeChildren(mRoot, 1);
        doUndoneOperation(mRoot);
        Logger.splitPrint(mRoot.drawTree());
        System.out.println("Travel end!");
    }

    private TreeNode<Operation> newEmptyTreeNode() {
        ArrayList<View> views = local.getViews();
        if (views.size() == 0) {
            screenShot(new int[] { -1, -1 });
            Assert.assertTrue("views.size() == 0!", false);
        }
        Operation emptyOperation = new Operation(views.get(0), 0, "root", null);
        emptyOperation.setEnable(false);
        return new TreeNode<Operation>(emptyOperation);
    }

    private boolean shouldDo(Operation operation) {
        int ret = isSuitable(operation);
        String retString = Util.getReturnValueName(APPTraveler.class, ret);
        if (ret == RET_PASS) {
            Logger.println("Pass [" + retString + "] " + operation);
            return true;
        } else {
            Logger.println("Fail [" + retString + "] " + operation);
            return false;
        }
    }

    private int isSuitable(Operation operation) {
        if (!operation.isEnable()) {
            return RET_NOT_ENABLE;
        }

        //        if (!operation.isShouldRepeat()) {
        //            return RET_SHOULD_NOT_REPEAT;
        //        }

        if (operation.getState() != Operation.STATE_UNDONE) {
            return RET_NOT_UNDOEN;
        }

        // Used to substitute for disable brother
        //        if (!ViewHelper.isAvailable(operation.getView(), false)) {
        //            return RET_NOT_AVAILABLE;
        //        }

        return RET_PASS;
    }

    /**
     * Check mOldOperations that whether there is a loop whose length is from
     * LOOP_MIN_LENGTH to LOOP_MAX_LENGTH. This function is used to prevent that
     * APPTraveler falls into a infinite loop.
     */
    private boolean hasLoop() {
        for (int i = 0; i <= LOOP_MAX_LENGTH - LOOP_MIN_LENGTH; i++) {
            if (hasSameQueue(LOOP_MIN_LENGTH + i)) {
                Logger.println("There is a loop whose length is " + (LOOP_MIN_LENGTH + i) + "!!!!");
                Logger.println("Travel must be stopped!!!!");
                return true;
            }
        }
        return false;
    }

    /**
     * Check mOldOperations from its tail that whether there are two same
     * neighbor-queue whose length are queueLength.
     * 
     * @param queueLength
     * @return
     */
    private boolean hasSameQueue(int queueLength) {
        int tail = mOldOperations.size() - 1;
        for (int i = 0; i < queueLength; i++) {
            int currentPointer = tail - i;
            int lastPointer = currentPointer - 2 * queueLength + 1;

            if (lastPointer < 0 || currentPointer < 0) {
                return false;
            }

            if (!mOldOperations.get(currentPointer).isTheSameOperation(
                    mOldOperations.get(lastPointer))) {
                return false;
            }
        }

        for (int i = tail - 2 * queueLength + 1; i < mOldOperations.size(); i++) {
            Logger.println("loop: " + mOldOperations.get(i).toString());
        }
        return true;
    }

    private boolean isTimeout() {
        if (System.currentTimeMillis() > mEnd) {
            Logger.println("Travel is over " + TRAVEL_TIME_OUT / 60000
                    + " minutes. It must be stopped!!!");
            return true;
        }
        return false;
    }

    private void doUndoneOperation(TreeNode<Operation> tree) {
        screenShot(new int[] { -1, -1 });

        switch (TRAVEL_MODE) {
        case MODE_PREORDER:
            //            preorderTraversal(tree);
            preorderTraversal2();
            break;
        }
    }

    public static void screenShot(int[] xy) {
        if (null == xy) {
            return;
        }
        // TODO: no pointer
        local.screenShotNamedCaseName("" + mScreenShotCounter++);
        //        mDeviceServer.executeCommandOnPC("SCREENSHOT %s_traveler/" + mScreenShotCounter++ + ".png@"
        //                + xy[0] + "," + xy[1], TIMEOUT_WAIT_FOR_SCREEN_SHOT);

        //        CafeCaller.getLocal().dumpActivityText(false);
    }

    int level = 0;

    private void preorderTraversal2() {
        while (hasUndoneOperation(level)) {
            Operation operation = getCurrentOperation(level);
            //            level = getCurrentLevel(level);
            //            Logger.println("level: " + level);
            if (null == operation) {
                ViewHelper.goBack("getCurrentOperation() == null");
                level--;
                screenShot(new int[] { -1, -1 });// no focus
            }
            /*
            else if (level > 3) {
                ViewHelper.goBack("level > 3");
                level--;
                screenShot(new int[] { -1, -1 });// no focus
            }*/
            else {
                screenShot(operation.doOperation());
                //                level++;
            }
        }
    }

    private boolean hasUndoneOperation(int level) {
        // update first
        getCurrentOperation(level);
        for (Operation operation : mAllOperations) {
            if (operation.getState() == Operation.STATE_UNDONE) {
                return true;
            }
        }
        return false;
    }

    private Operation getCurrentOperation(int level) {
        ArrayList<Operation> operations = getAvailableOperations(level);
        return operations.size() == 0 ? null : operations.get(0);
    }

    private void preorderTraversal(TreeNode<Operation> tree) {
        Logger.println("Preorder traversal.");
        tree.preorderTraversal(new NodeCallBack<Operation>() {

            @Override
            public void doSomething(TreeNode<Operation> node, int maxDepth) {
                Logger.splitPrint(mRoot.drawTree());
                int treeLevel = MAX_TRAVEL_DEPTH - maxDepth;
                int currentLevel = getCurrentLevel(treeLevel);
                Logger.println("TreeLevel: " + treeLevel);
                if (treeLevel == 0 || currentLevel < treeLevel) {
                    Logger.println("return from " + node.getData().toString());
                    return;
                } else if (currentLevel > treeLevel) {
                    ViewHelper.goBack("currentLevel > treeLevel");
                    screenShot(new int[] { -1, -1 });// no focus
                    updateNodeChildren(node, treeLevel + 1);
                }
                Operation operation = node.getData();
                if (shouldDo(operation)) {
                    screenShot(operation.doOperation());
                    // handle speical case
                    if (ViewHelper.handlePasswordEditText()) {
                        ViewHelper.sleep();
                        ViewHelper.sleep();
                    }
                    // mOldOperations.add(operation);
                    updateNodeChildren(node, treeLevel + 1);
                    operation.setView(null);
                }
            }

            @Override
            public void doWhenCompleted(TreeNode<Operation> node, int maxDepth) {
                Logger.println("COMPELTED: " + node.getData().toString());
                int treeLevel = MAX_TRAVEL_DEPTH - maxDepth;
                updateNodeChildren(node.getParent(), treeLevel);
                if (hasLoop() /*|| isTimeout()*/) {
                    ViewHelper.goBack("back becasue hasLoop()");
                    //                    System.exit(0);
                }
            }

            /**
             * repeat operation for dialog model
             */
            @Override
            public boolean shouldRepeat(TreeNode<Operation> node) {
                return node.getData().isShouldRepeat();
            }

            @Override
            public boolean shouldStop(TreeNode<Operation> node) {
                return APPTraveler.mIsEnd /*|| isTimeout()*/;
            }
        }, MAX_TRAVEL_DEPTH);
    }

    private int getCurrentLevel(int treeLevel) {
        ArrayList<Operation> operations = getAvailableOperations(treeLevel);
        int[] levels = new int[MAX_TRAVEL_DEPTH + 2];

        if (operations.size() == 0) {
            Logger.println("getCurrentLevel: operations.size() == 0");
        }

        // count levels
        for (Operation operation : operations) {
            //            Logger.println(operation + ": " + operation.getLevel());
            levels[operation.getLevel()]++;
        }
        int max = 0;
        int maxIndex = 0;

        if (levels.length > treeLevel && levels[treeLevel] != 0) {
            return treeLevel;
        }

        // get max count level
        for (int i = 0; i < levels.length; i++) {
            Logger.println("level " + i + ": " + levels[i]);
            if (levels[i] > max) {
                max = levels[i];
                maxIndex = i;
            }
        }
        Logger.println("CurrentLevel: " + maxIndex);
        operations = null;
        return maxIndex;
    }

    private void updateNodeChildren(TreeNode<Operation> node, int level) {
        // set screen orientation if needed
        // TODO resume later
        //        if (ParameterManager.getOrientation() == Orientation.LANDSCAPE) {
        //            Activity topActivity = CafeCaller.getLocal().getCurrentActivity();
        //            if (topActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        //                topActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //                Logger.println("LANDSCAPE: " + topActivity.getClass().getName());
        //            }
        //        }

        if (null == node) {
            return;
        }
        //        CafeCaller.getLocal().hideInputMethod();
        //        waitForLoading();
        ArrayList<Operation> operations = Operation.updateOperations(0);
        if (null == operations) {
            Logger.println("null == operations at updateChildren");
            return;
        }
        Logger.println("UPDATE " + operations.size() + " CHILDREN OF : " + node.getData());

        disableUndoneOperatoins();

        for (Operation operation : operations) {
            //            Logger.println("" + operation);
            int index = mAllOperations.indexOf(operation);
            if (index == -1) {
                operation.setLevel(level);
                //                Logger.println("add: " + operation);
                node.addChild(new TreeNode<Operation>(operation));
                mAllOperations.add(operation);
            } else {
                mAllOperations.get(index).setEnable(true);
                Logger.println("enable: " + mAllOperations.get(index));
            }
        }
        operations = null;
    }

    private ArrayList<Operation> getAvailableOperations(int level) {
        ArrayList<Operation> operations = Operation.getCurrentOperations();
        ArrayList<Operation> availableOperations = new ArrayList<Operation>();
        for (Operation operation : operations) {
            int index = mAllOperations.indexOf(operation);
            if (index == -1) {
                operation.setLevel(level);
                mAllOperations.add(operation);
                availableOperations.add(operation);
            } else if (mAllOperations.get(index).getState() == Operation.STATE_UNDONE) {
                mAllOperations.get(index).setLevel(level);
                availableOperations.add(mAllOperations.get(index));
            }
        }
        operations = null;
        String log = String.format("getAvailableOperations:%s mAllOperations:%s",
                availableOperations.size(), mAllOperations.size());
        Logger.println(log);
        printUndoneOperations();
        return availableOperations;
    }

    private void printUndoneOperations() {
        int i = 0;
        for (Operation operation : mAllOperations) {
            if (operation.getState() == Operation.STATE_UNDONE) {
                Logger.println(operation.toString());
                i++;
            }
        }
        Logger.println("############## undone operation: " + i);
    }

    private void disableUndoneOperatoins() {
        for (Operation operation : mAllOperations) {
            if (operation.isEnable() && operation.getState() == Operation.STATE_UNDONE) {
                operation.setEnable(false);
            }
        }
    }

    private boolean isTheSameLocation(View view, String locationString) {
        int[] location = new int[2];
        int[] viewLocation = new int[2];

        try {
            String[] locationSplit = locationString.split(",");
            location[0] = Integer.valueOf(locationSplit[0]);
            location[1] = Integer.valueOf(locationSplit[1]);
            view.getLocationOnScreen(viewLocation);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (location[0] == viewLocation[0] && location[1] == viewLocation[1]) {
            return true;
        }

        Logger.println("" + view + ":" + viewLocation[0] + "," + viewLocation[1]);
        Logger.println("is not TheSameLocation !");

        return false;
    }

    private void setLeftBrothersDone(TreeNode<Operation> node) {
        ArrayList<TreeNode<Operation>> brothers = node.getBrothers();
        for (TreeNode<Operation> brother : brothers) {
            if (node.getIndex() > brother.getIndex()) {
                brother.getData().setState(Operation.STATE_DONE);
            }
        }
    }

    private TreeNode<Operation> findNodeFromTree(Operation operation,
            ArrayList<TreeNode<Operation>> children) {
        for (TreeNode<Operation> child : children) {
            if (child.getData().equals(operation)) {
                return child;
            }
        }
        return null;
    }

    public static boolean isInPackage(String className) {
        // TODO Dialog will be ignored by this function!!!
        if (null == className) {
            return true;
        }
        return className.contains(APPTraveler.local.mPackageName);
    }
}
