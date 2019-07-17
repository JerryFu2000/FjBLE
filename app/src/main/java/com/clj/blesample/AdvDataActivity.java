package com.clj.blesample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.TextView;

public class AdvDataActivity extends AppCompatActivity {

    private TextView txt_advdata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adv_data);

        //初始化View
        initView();

        //创建Intent类对象intent，并通过getIntent()方法来获得启动本Activity时传入的Intent
        Intent intent = getIntent();
        //通过传入的“键”来获得相应的“值”
        String data = intent.getStringExtra("extra_data");

        txt_advdata.setText(data);

    }


    //成员方法(即类的多个对象的共享方法)
    //本类新增的方法：1.完全新增的方法
    private void initView() {
        txt_advdata = (TextView) findViewById(R.id.txt_advdata);
    }
}
