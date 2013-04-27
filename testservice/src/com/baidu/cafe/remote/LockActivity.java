/*
 * Copyright (C) 2012 Baidu.com Inc
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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.baidu.cafe.remote.SystemLib.TimeLocker;

/**
 * This activity will called by SystemLib.lockDangerousActivity() for protecting
 * some other activities.
 * 
 * @author luxiaoyu01@baidu.com
 * @date 2012-7-3
 * @version
 * @todo
 */
public class LockActivity extends Activity {
    public static String  unlockPassword       = null;
    public static boolean lock_activity_enable = false;
    public static boolean keep_state_enable    = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        Button buttonUnlock = (Button) findViewById(R.id.button_unlock);
        buttonUnlock.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextPassword = (EditText) findViewById(R.id.editTextPassword);
                String input = editTextPassword.getText().toString();
                if (unlockPassword != null && unlockPassword.equals(input)) {
                    Log.print("Unlock!");
                    TimeLocker.unlock();
                    finish();
                }
            }
        });
    }

}
