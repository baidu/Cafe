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

public class MyIntent {

    public static final String ACTION_PROXY           = "com.baidu.arms.Proxy";
    public static final String ACTION_INSTALL_BEGIN   = "com.baidu.arms.install.begin";
    public static final String ACTION_INSTALL_END     = "com.baidu.arms.install.end";
    public static final String ACTION_SCREENCAP_BEGIN = "com.baidu.arms.screencap.begin";
    public static final String ACTION_SCREENCAP_END   = "com.baidu.arms.screencap.end";
    public static final String EXTRA_INSTALL          = "installapk";
    public static final String EXTRA_SCREENCAP        = "screencap";

    public static final String EXTRA_OPERATION        = "operation";
    public static final String EXTRA_ARG1             = "arg1";

    public static final String EXTRA_FINISH           = "finish";
}
