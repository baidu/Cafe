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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;

/**
 * @author sunyuanzhen@baidu.com
 * @date 2012-2-14
 * @version
 * @todo
 */
class SoloEx extends Solo {

    // In order to use the Non-Public-Class of robotium project, we have to
    // define the following member variables as Object
    // Currently, although some classes is not being used, we still define them
    // in case we may need them in the future
    protected Object              mAsserter;
    protected static Object       mViewFetcher;
    protected Object              mChecker;
    protected Object              mClicker;
    protected Object              mPresser;
    protected Object              mSearcher;
    protected Object              mActivitiyUtils;
    protected Object              mDialogUtils;
    protected Object              mTextEnterer;
    protected Object              mScroller;
    protected Object              mRobotiumUtils;
    protected Object              mSleeper;
    protected Object              mWaiter;
    protected Object              mSetter;

    private final Instrumentation mInstrumentation;
    private final Activity        mActivity;

    public final static int       SEARCHMODE_COMPLETE_MATCHING = 1;
    public final static int       SEARCHMODE_DEFAULT           = 1;
    public final static int       SEARCHMODE_INCLUDE_MATCHING  = 2;

    /**
     * Constructor that takes in the instrumentation and the start activity.
     * 
     * @param instrumentation
     *            the {@link Instrumentation} instance.
     * @param activity
     *            {@link Activity} the start activity
     */
    public SoloEx(Instrumentation instrumentation, Activity activity) {
        super(instrumentation, activity);

        this.mActivity = activity;
        this.mInstrumentation = instrumentation;

        // In order to use the Non-Public-Class of robotium project, we have to
        // reflect the following classes
        try {
            Class Sleeper = Class.forName("com.jayway.android.robotium.solo.Sleeper");
            Class Asserter = Class.forName("com.jayway.android.robotium.solo.Asserter");
            Class ViewFetcher = Class.forName("com.jayway.android.robotium.solo.ViewFetcher");
            Class Checker = Class.forName("com.jayway.android.robotium.solo.Checker");
            Class Clicker = Class.forName("com.jayway.android.robotium.solo.Clicker");
            Class Presser = Class.forName("com.jayway.android.robotium.solo.Presser");
            Class Searcher = Class.forName("com.jayway.android.robotium.solo.Searcher");
            Class ActivityUtils = Class.forName("com.jayway.android.robotium.solo.ActivityUtils");
            Class DialogUtils = Class.forName("com.jayway.android.robotium.solo.DialogUtils");
            Class TextEnterer = Class.forName("com.jayway.android.robotium.solo.TextEnterer");
            Class Scroller = Class.forName("com.jayway.android.robotium.solo.Scroller");
            Class RobotiumUtils = Class.forName("com.jayway.android.robotium.solo.RobotiumUtils");
            Class Waiter = Class.forName("com.jayway.android.robotium.solo.Waiter");
            Class Setter = Class.forName("com.jayway.android.robotium.solo.Setter");

            Constructor sleeperConstructor = Sleeper.getDeclaredConstructor();
            Constructor activitiyUtilsConstructor = ActivityUtils.getDeclaredConstructor(
                    Instrumentation.class, Activity.class, Sleeper);
            Constructor setterConstructor = Setter.getDeclaredConstructor(ActivityUtils);
            Constructor viewFetcherConstructor = ViewFetcher.getDeclaredConstructor(ActivityUtils);
            Constructor scrollerConstructor = Scroller.getDeclaredConstructor(
                    Instrumentation.class, ActivityUtils, ViewFetcher, Sleeper);
            Constructor searcherConstructor = Searcher.getDeclaredConstructor(ViewFetcher,
                    Scroller, Sleeper);
            Constructor waiterConstructor = Waiter.getDeclaredConstructor(ActivityUtils,
                    ViewFetcher, Searcher, Scroller, Sleeper);
            Constructor asserterConstructor = Asserter
                    .getDeclaredConstructor(ActivityUtils, Waiter);
            Constructor dialogUtilsConstructor = DialogUtils.getDeclaredConstructor(ViewFetcher,
                    Sleeper);
            Constructor checkerConstructor = Checker.getDeclaredConstructor(ViewFetcher, Waiter);
            Constructor robotiumUtilsConstructor = RobotiumUtils.getDeclaredConstructor(
                    Instrumentation.class, Sleeper);
            Constructor textEnterer = TextEnterer.getDeclaredConstructor(Instrumentation.class);
            Constructor clickerConstructor = Clicker.getDeclaredConstructor(ViewFetcher, Scroller,
                    RobotiumUtils, Instrumentation.class, Sleeper, Waiter);
            Constructor presserConstructor = Presser.getDeclaredConstructor(Clicker,
                    Instrumentation.class, Sleeper, Waiter);

            sleeperConstructor.setAccessible(true);
            activitiyUtilsConstructor.setAccessible(true);
            setterConstructor.setAccessible(true);
            viewFetcherConstructor.setAccessible(true);
            asserterConstructor.setAccessible(true);
            dialogUtilsConstructor.setAccessible(true);
            scrollerConstructor.setAccessible(true);
            searcherConstructor.setAccessible(true);
            waiterConstructor.setAccessible(true);
            checkerConstructor.setAccessible(true);
            robotiumUtilsConstructor.setAccessible(true);
            clickerConstructor.setAccessible(true);
            presserConstructor.setAccessible(true);
            textEnterer.setAccessible(true);

            mSleeper = sleeperConstructor.newInstance(new Object[] {});
            mActivitiyUtils = activitiyUtilsConstructor.newInstance(mInstrumentation, mActivity,
                    mSleeper);
            mSetter = setterConstructor.newInstance(mActivitiyUtils);
            mViewFetcher = viewFetcherConstructor.newInstance(mActivitiyUtils);
            mScroller = scrollerConstructor.newInstance(mInstrumentation, mActivitiyUtils,
                    mViewFetcher, mSleeper);
            mSearcher = searcherConstructor.newInstance(mViewFetcher, mScroller, mSleeper);
            mWaiter = waiterConstructor.newInstance(mActivitiyUtils, mViewFetcher, mSearcher,
                    mScroller, mSleeper);
            mAsserter = asserterConstructor.newInstance(mActivitiyUtils, mWaiter);
            mDialogUtils = dialogUtilsConstructor.newInstance(mViewFetcher, mSleeper);
            mChecker = checkerConstructor.newInstance(mViewFetcher, mWaiter);
            mRobotiumUtils = robotiumUtilsConstructor.newInstance(mInstrumentation, mSleeper);
            mTextEnterer = textEnterer.newInstance(mInstrumentation);
            mClicker = clickerConstructor.newInstance(mViewFetcher, mScroller, mRobotiumUtils,
                    mInstrumentation, mSleeper, mWaiter);
            mPresser = presserConstructor
                    .newInstance(mClicker, mInstrumentation, mSleeper, mWaiter);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor that takes in the instrumentation.
     * 
     * @param instrumentation
     *            the {@link Instrumentation} instance
     * 
     */
    public SoloEx(Instrumentation instrumentation) throws Throwable {
        this(instrumentation, null);
    }

    public void finalize() throws Throwable {
        invoke(mActivitiyUtils, "finalize"); // mActivityUtils.finalize();
        super.finalize();
    }

    /**
     * Get the field of an object
     * 
     * @param owner
     *            This field's owner object
     * @param name
     *            Field name
     * @return field object
     */
    protected Object getField(Object owner, String name) {
        try {
            // The Non-Public-Classes of robotium do not contain the super
            // classes so that there is no need to consider them at all.
            Field field = owner.getClass().getDeclaredField(name);
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            return field.get(owner);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Invoke the method with no argument
     * 
     * @param owner
     *            This method's owner object
     * @param name
     *            Method name
     */
    protected static Object invoke(Object owner, String name) {
        return invoke(owner, name, new Class[] {}, new Object[] {});
    }

    /**
     * Invoke the method
     * 
     * @param owner
     *            This method's owner object
     * @param name
     *            Method name
     * @param parameterTypes
     *            types array of parameters
     * @param parameters
     *            objects array of parameters
     */
    protected static Object invoke(Object owner, String name, Class[] parameterTypes,
            Object[] parameters) {
        try {
            return ReflectHelper.invoke(owner, 0, name, parameterTypes, parameters);
        } catch (Exception e) {
            print("invoke error:");
            print("name: " + name);
            print("owner: " + owner.toString());
            e.printStackTrace();
            return null;
        }
    }

    private static void print(String message) {
        if (Log.IS_DEBUG && message != null) {
            Log.d("SoloEx", message);
        }
    }

}
