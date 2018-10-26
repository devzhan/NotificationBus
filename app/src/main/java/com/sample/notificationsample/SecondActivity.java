package com.sample.notificationsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.sample.notificationsample.log.NLog;
import com.sample.notificatonlibrary.notification.NotificationCenter;
import com.sample.notificatonlibrary.notification.TopicSubscriber;

public class SecondActivity extends AppCompatActivity implements View.OnClickListener {
    private Button button;
    public static final String TOP_KEY ="top_key";

    TopicSubscriber<String> subscriber = new TopicSubscriber<String>() {
        @Override
        public void onEvent(String topic, String data) {
            NLog.i(NotificationCenter.TAG,"class is %s,topic is %s",getClass(),topic);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        NotificationCenter.defaultCenter().subscriber(TOP_KEY,subscriber);
        initView();
    }
    private void initView() {
        button = findViewById(R.id.bt_second);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId==R.id.bt_second){
            Intent intent = new Intent(this,ThirdActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.defaultCenter().unsubscribe(TOP_KEY,subscriber);

    }
}
