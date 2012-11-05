package com.baidu.cafe.remote;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class LockActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_lock, menu);
        return true;
    }

    
}
