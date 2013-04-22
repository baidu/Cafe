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

package com.baidu.cafe;

import com.baidu.cafe.remote.Armser;

import android.app.Service;
import android.test.ServiceTestCase;

/**
 * @author luxiaoyu01@baidu.com
 * @date 2011-9-20
 * @version
 * @todo
 */
public class CafeServiceTestCase<T extends Service> extends ServiceTestCase<T> {
    protected static Armser remote          = null;
    private static String   mPackageName    = null;
    private TearDownHelper  mTearDownHelper = null;

    public CafeServiceTestCase(Class<T> serviceClass) {
        super(serviceClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mPackageName = this.getClass().getName();
        remote = new Armser(getContext());
        remote.bind(getContext());
        mTearDownHelper = new TearDownHelper(remote);
    }

    @Override
    protected void tearDown() throws Exception {
        mTearDownHelper.backToHome();
        mTearDownHelper.killWindowsFromBirthToNow();
        mTearDownHelper = null;
        remote.waitForAllDumpCompleted();
        remote = null;
        super.tearDown();
    }

}
