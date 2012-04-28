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

package com.baidu.cafe.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * boot up arms from command line
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2011-6-27
 * @version
 * @todo
 */
public class ArmsBootupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Start UBC Service
        Log.print("ArmsBootupReceiver onReceive");
        if (null == context || null == intent) {
            Log.print("ArmsBootupReceiver parameter is null");
            return;
        }

        String action = intent.getAction();
        if (null == action) {
            Log.print("ArmsBootupReceiver no action in intent");
            return;
        }

        Log.print("ArmsBootupReceiver incoming intent is " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.print("ArmsBootupReceiver Boot completed. Starting Arms");
            context.startService(new Intent("com.baidu.arms.IRemoteArms"));
        }
    }
}
