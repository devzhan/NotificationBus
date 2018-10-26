package com.sample.notificationsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.sample.notificationsample.log.NLog;
import com.sample.notificatonlibrary.notification.NotificationCenter;
import com.sample.notificatonlibrary.notification.TopicSubscriber;

public class ThirdActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TOP_KEY = "top_key2";

    TopicSubscriber<String> subscriber = new TopicSubscriber<String>() {
        @Override
        public void onEvent(String topic, String data) {
            NLog.i(NotificationCenter.TAG,"class is %s,topic is %s",getClass(),topic);
        }
    };
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);
        NotificationCenter.defaultCenter().subscriber(TOP_KEY, subscriber);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationCenter.defaultCenter().unsubscribe(TOP_KEY,subscriber);

    }

    private void initView() {
        button = findViewById(R.id.bt_send);
        button.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        int viewId = v.getId();
        if (viewId==R.id.bt_send){
            NotificationCenter.defaultCenter().publish("top_key",null);
            EventSubscriber eventSubscriber = new EventSubscriber();
            NotificationCenter.defaultCenter().publish(eventSubscriber);
        }
    }


}
