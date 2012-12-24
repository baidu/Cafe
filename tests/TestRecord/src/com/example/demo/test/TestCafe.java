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
    private static final String LAUNCHER_ACTIVITY_FULL_CLASSNAME = "com.baidu.news.MainActivity";
    private static Class<?>     launcherActivityClass;
    private static final String TARGET_PACKAGE                   = "com.baidu.news";
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

    private int getRStringId(String packageName, String stringName) {
        try {
            Class[] classes = Class.forName(packageName + ".R").getDeclaredClasses();
            int stringIndex = 0;
            for (int i = 0; i < classes.length; i++) {
                System.out.println("class: "+classes[i].getName());
                if (classes[i].getName().indexOf("$string") != -1) {
                    stringIndex = i;
                }
            }
            return (Integer) classes[stringIndex].getDeclaredField(stringName).get(
                    classes[stringIndex].newInstance());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void test_sample() {
        getRStringId("com.baidu.news","");
        local.sleep(2000);
        /*
        for (View view : local.getCurrentViews()) {
            try {
                System.out.println("!!!!!!!!!!"+view.getId());
                System.out.println(getInstrumentation().getTargetContext().getResources().getString(view.getId()));
            } catch (Exception e) {
            }
        }
        local.beginRecordCode();
        local.sleep(2000000);
        */
    }

}
