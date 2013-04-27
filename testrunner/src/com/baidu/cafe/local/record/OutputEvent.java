/*
 * Copyright (C) 2013 Baidu.com Inc
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

package com.baidu.cafe.local.record;

import android.view.View;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2013-4-12
 * @version
 * @todo
 */
public class OutputEvent {
    final static int PRIORITY_DRAG              = 1;
    final static int PRIORITY_KEY               = 2;
    final static int PRIORITY_SCROLL            = 3;
    final static int PRIORITY_CLICK             = 4;

    final static int PRIORITY_WEBELEMENT_CLICK  = 10;
    final static int PRIORITY_WEBELEMENT_CHANGE = 10;

    /**
     * NOTICE: This field can not be null!
     */
    public View      view                       = null;
    public int       priority                   = 0;
    protected String code                       = "";
    protected String log                        = "";

    public String getCode() {
        return code;
    }

    public String getLog() {
        return log;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLog(String log) {
        this.log = log;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", view, priority);
    }

    @Override
    public boolean equals(Object o) {
        if (null == o) {
            return false;
        }
        OutputEvent target = (OutputEvent) o;
        return this.view.equals(target.view) && this.priority == target.priority ? true : false;
    }

}