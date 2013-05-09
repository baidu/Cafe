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

import android.util.Log;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-1-7
 * @version
 * @todo
 */
public class Logger {
    public final static String  TAG_LOG  = "traveler";
    public final static String  TAG_TEXT = "DumpText";
    //    public final static String  TAG   = Logger.class.getName();
    public final static boolean DEBUG    = true;

    public static void printTextln(String msg) {
        Log.i(TAG_TEXT, msg);
    }

    public static void println(String msg) {
        if (DEBUG) {
            Log.i(TAG_LOG, msg);
        }
    }

    /**
     * For long msg, android.util.Log is not support for long msg.
     * 
     * @param msg
     */
    public static void splitPrint(String msg) {
        for (String line : msg.split("\n")) {
            println(line);
        }
    }

    public static <T> void println(ArrayList<T> msgs) {
        for (T t : msgs) {
            println(t.toString());
        }
    }

}
