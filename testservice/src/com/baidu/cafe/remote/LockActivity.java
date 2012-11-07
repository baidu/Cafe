package com.baidu.cafe.remote;

import com.baidu.cafe.remote.SystemLib.TimeLocker;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
