package com.baidu.cafe;

import android.os.Bundle;
import android.test.AndroidTestRunner;

import com.baidu.cafe.utils.ReflectHelper;
import com.zutubi.android.junitreport.JUnitReportTestRunner;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2012-6-25
 * @version
 * @todo
 */
public class CafeTestRunner extends JUnitReportTestRunner {
    public static Bundle mArguments = null;

    @Override
    public void onCreate(Bundle arguments) {
        mArguments = arguments;
        super.onCreate(arguments);
    }

    @Override
    protected AndroidTestRunner getAndroidTestRunner() {
        AndroidTestRunner runner = makeAndroidTestRunner();
        try {
            String superClassName = "com.zutubi.android.junitreport.JUnitReportTestRunner";
            String mReportFile = (String) ReflectHelper.getField(this, superClassName,
                    "mReportFile");
            String mReportDir = (String) ReflectHelper.getField(this, superClassName, "mReportDir");
            boolean mFilterTraces = (Boolean) ReflectHelper.getField(this, superClassName,
                    "mFilterTraces");
            boolean mMultiFile = (Boolean) ReflectHelper.getField(this, superClassName,
                    "mMultiFile");
            CafeListener listener = new CafeListener(getContext(), getTargetContext(), mReportFile,
                    mReportDir, mFilterTraces, mMultiFile, this);
            ReflectHelper.setField(this, superClassName, "mListener", listener);
            runner.addTestListener(listener);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return runner;
    }

}
