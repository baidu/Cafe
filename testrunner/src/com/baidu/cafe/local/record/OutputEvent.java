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