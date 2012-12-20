package com.baidu.cafe.record;

import android.view.View;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-12-20
 * @version
 * @todo
 */
class OutputEvent {
    public final static int PRIORITY_DRAG  = 1;
    public final static int PRIORITY_CLICK = 2;

    public int              proity         = 0;
    public View             view           = null;
    protected String        code           = "";
    protected String        log            = "";

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
}

class ClickEvent extends OutputEvent {
    public ClickEvent(View view) {
        this.view = view;
        this.proity = PRIORITY_CLICK;
    }
}

class DragEvent extends OutputEvent {
    public DragEvent(View view) {
        this.view = view;
        this.proity = PRIORITY_DRAG;
    }
}
