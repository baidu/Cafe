package com.example.demo.test;

import java.util.ArrayList;

import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.baidu.cafe.CafeTestCase;
import com.baidu.cafe.local.LocalLib;
import com.baidu.cafe.local.Log;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-6-27
 * @version
 * @todo
 */
public class TestCafe extends CafeTestCase {
    private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "{launcher_class}";
    private static Class<?>     launcherActivityClass;
    private static final String TARGET_PACKAGE                   = "{target_package}";
    static {
        try {
            launcherActivityClass = Class.forName(LAUNCHER_ACTIVITY_FULL_CLASSNAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public TestCafe() {
        super(TARGET_PACKAGE, launcherActivityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test_sample() {
        local.sleep(2000);
        local.beginRecordCode();
        local.sleep(2000000);
    }

}
