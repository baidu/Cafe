package com.baidu.cafe.test;

import com.baidu.cafe.CafeTestCase;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-6-27
 * @version
 * @todo
 */
public class CafeTraveler extends CafeTestCase {
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

    public CafeTraveler() {
        super(TARGET_PACKAGE, launcherActivityClass);
    }

    @Override
    protected void setUp() throws Exception{
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception{
        super.tearDown();
    }

    public void test_travel() {
        local.sleep(2000);
        local.travel();
    }

}
