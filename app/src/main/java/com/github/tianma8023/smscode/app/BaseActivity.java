package com.github.tianma8023.smscode.app;

import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

//        MobclickAgent.onResume(this);
    }


    @Override
    protected void onPause() {
        super.onPause();

//        MobclickAgent.onPause(this);
    }
}
