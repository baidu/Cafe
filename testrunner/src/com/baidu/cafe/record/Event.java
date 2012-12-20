package com.baidu.cafe.record;

import android.view.View;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-12-20
 * @version
 * @todo
 */
class Event {
    public final static int PRIORITY_CLICK = 1;
    public final static int PRIORITY_TOUCH = 2;

    public int              proity         = 0;
    public View             view           = null;
    protected String        code           = "";
    protected String        log            = "";

    public void getCode() {

    }

    public void getLog() {

    }
}

class ClickEvent extends Event {
    public ClickEvent(View view) {
        this.view = view;
        this.proity = PRIORITY_CLICK;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
