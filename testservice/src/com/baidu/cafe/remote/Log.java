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

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-3-3
 * @version
 * @todo
 */
public class Log {
    public final static boolean DEBUG = true;

    public static void print(String msg) {
        // get the classname of invoker 
        String className = Thread.currentThread().getStackTrace()[3].getClassName();
        String shortName = className.substring(className.lastIndexOf('.') + 1);
        // remove $
        if (shortName.contains("$")) {
            shortName = shortName.substring(0, shortName.lastIndexOf('$'));
        }
        android.util.Log.i(shortName, msg);
    }
}
