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

import java.io.PrintWriter;
import java.io.StringWriter;

import android.test.ActivityInstrumentationTestCase2;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-13
 * @version
 * @todo
 */
public class Log {
    public final static boolean                     IS_DEBUG     = true;

    private static ActivityInstrumentationTestCase2 mTestcase    = null;

    /**
     * Default log tag use PACKAGE_NAME
     */
    public final static int                         DEFAULT      = 2;

    /**
     * use case name as log tag
     */
    public final static int                         CASE_NAME    = 1;

    /**
     * use package name as log tag
     */
    public final static int                         PACKAGE_NAME = 2;

    /**
     * distance from getTag() to invoker
     */
    private final static int                        DISTANCE     = 4;
    private static int                              mTag         = DEFAULT;

    /**
     * Priority constant for the println method; use Log.v.
     */
    public static final int                         VERBOSE      = 2;

    /**
     * Priority constant for the println method; use Log.d.
     */
    public static final int                         DEBUG        = 3;

    /**
     * Priority constant for the println method; use Log.i.
     */
    public static final int                         INFO         = 4;

    /**
     * Priority constant for the println method; use Log.w.
     */
    public static final int                         WARN         = 5;

    /**
     * Priority constant for the println method; use Log.e.
     */
    public static final int                         ERROR        = 6;

    /**
     * Priority constant for the println method.
     */
    public static final int                         ASSERT       = 7;

    private Log() {
    }

    /**
     * @param testcase
     *            If used in class which super class is
     *            ActivityInstrumentationTestCase2, assign this here
     * @param tag
     *            the tag of logs. It should be one of below. Log.DEFAULT
     *            Log.PACKAGE_NAME Log.CASE_NAME
     */
    public static void init(ActivityInstrumentationTestCase2 testcase, int tag) {
        mTestcase = testcase;
        switch (tag) {
        case CASE_NAME:
            mTag = CASE_NAME;
            break;
        case PACKAGE_NAME:
            mTag = PACKAGE_NAME;
            break;
        default:
            mTag = DEFAULT;
            break;
        }
    }

    private static String getTag() {
        switch (mTag) {
        case CASE_NAME:
            return Thread.currentThread().getStackTrace()[DISTANCE].getMethodName();
        case PACKAGE_NAME:
            return mTestcase.getClass().getName();
        default:
            return "NO_TAG";
        }
    }

    public static String getThreadInfo(int distance) {
        StackTraceElement invoker = Thread.currentThread().getStackTrace()[distance];
        return String.format("at %s.%s():%s", invoker.getClassName(), invoker.getMethodName(),
                invoker.getLineNumber());
    }

    public static String getThreadInfo() {
        return getThreadInfo(4);
    }

    /**
     * Send a {@link #VERBOSE} log message.
     * 
     * @param msg
     *            The message you would like logged.
     */
    public static int v(String msg) {
        return println(VERBOSE, getTag(), msg);
    }

    /**
     * Send a {@link #VERBOSE} log message with custom tag.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int v(String tag, String msg) {
        return println(VERBOSE, tag, msg);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @param tr
     *            An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
        return println(VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #DEBUG} log message.
     * 
     * @param msg
     *            The message you would like logged.
     */
    public static int d(String msg) {
        return println(DEBUG, getTag(), msg);
    }

    /**
     * Send a {@link #DEBUG} log message with custom tag.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int d(String tag, String msg) {
        return println(DEBUG, tag, msg);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @param tr
     *            An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
        return println(DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send an {@link #INFO} log message.
     * 
     * @param msg
     *            The message you would like logged.
     */
    public static int i(String msg) {
        return println(INFO, getTag(), msg);
    }

    /**
     * Send an {@link #INFO} log message with custom tag.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int i(String tag, String msg) {
        return println(INFO, tag, msg);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @param tr
     *            An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
        return println(INFO, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #WARN} log message.
     * 
     * @param msg
     *            The message you would like logged.
     */
    public static int w(String msg) {
        return println(WARN, getTag(), msg);
    }

    /**
     * Send a {@link #WARN} log message with custom tag.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int w(String tag, String msg) {
        return println(WARN, tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @param tr
     *            An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
        return println(WARN, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Checks to see whether or not a log for the specified tag is loggable at
     * the specified level.
     * 
     * The default level of any tag is set to INFO. This means that any level
     * above and including INFO will be logged. Before you make any calls to a
     * logging method you should check to see if your tag should be logged. You
     * can change the default level by setting a system property: 'setprop
     * log.tag.&lt;YOUR_LOG_TAG> &lt;LEVEL>' Where level is either VERBOSE,
     * DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS. SUPRESS will turn off all
     * logging for your tag. You can also create a local.prop file that with the
     * following in it: 'log.tag.&lt;YOUR_LOG_TAG>=&lt;LEVEL>' and place that in
     * /data/local.prop.
     * 
     * @param tag
     *            The tag to check.
     * @param level
     *            The level to check.
     * @return Whether or not that this is allowed to be logged.
     * @throws IllegalArgumentException
     *             is thrown if the tag.length() > 23.
     */
    public static boolean isLoggable(String tag, int level) {
        return android.util.Log.isLoggable(tag, level);
    }

    /*
     * Send a {@link #WARN} log message and log the exception.
     * 
     * @param tag Used to identify the source of a log message. It usually
     * identifies the class or activity where the log call occurs.
     * 
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
        return println(WARN, tag, getStackTraceString(tr));
    }

    /**
     * Send an {@link #ERROR} log message.
     * 
     * @param msg
     *            The message you would like logged.
     */
    public static int e(String msg) {
        return println(ERROR, getTag(), msg);
    }

    /**
     * Send an {@link #ERROR} log message with custom tag.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int e(String tag, String msg) {
        return println(ERROR, tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @param tr
     *            An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
        return android.util.Log.e(tag, msg, tr);
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     * 
     * @param tr
     *            An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Low-level logging call.
     * 
     * @param priority
     *            The priority/type of this log message
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
        return android.util.Log.println(priority, tag, msg);
    }
}
