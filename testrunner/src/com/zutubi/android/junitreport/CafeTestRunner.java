package com.zutubi.android.junitreport;

import com.baidu.cafe.utils.ReflectHelper;

import android.os.Bundle;
import android.test.AndroidTestRunner;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2013-6-25
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
            String mReportFile = (String) ReflectHelper.getObjectProperty(this, 1, "mReportFile");
            String mReportDir = (String) ReflectHelper.getObjectProperty(this, 1, "mReportDir");
            boolean mFilterTraces = (Boolean) ReflectHelper.getObjectProperty(this, 1,
                    "mFilterTraces");
            boolean mMultiFile = (Boolean) ReflectHelper.getObjectProperty(this, 1, "mMultiFile");
            CafeListener listener = new CafeListener(getContext(), getTargetContext(), mReportFile,
                    mReportDir, mFilterTraces, mMultiFile, this);
            ReflectHelper.setObjectProperty(this, 1, "mListener", listener);
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
