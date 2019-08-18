package com.clj.blesample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class LineChartActivity extends AppCompatActivity implements View.OnClickListener {

    private LineChart mLineChart;
    private List<Entry> entries0;
    private List<Entry> entries1;
    private List<Entry> entries2;


    private XAxis xAxis;
    private YAxis leftYAxis;
    private YAxis rightYAxis;
    private float X_Value;

    private Description description;
    private LineChartMarkerView lineChartMarkerView;

    private Button btn_Start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linechart);

        //初始化View
        initView();

        //创建Intent类对象intent，并通过getIntent()方法来获得启动本Activity时传入的Intent
        Intent intent = getIntent();
        //通过传入的“键”来获得相应的“值”
        String data = intent.getStringExtra("extra_data");


    }


    //成员方法(即类的多个对象的共享方法)
    //本类新增的方法：1.完全新增的方法
    private void initView() {

        //折线图
        mLineChart = (LineChart) findViewById(R.id.lineChart);

        mLineChart.setNoDataText("暂时尚无数据");

        description = new Description();
        description.setText("X轴描述");
        description.setTextColor(Color.RED);
        description.setEnabled(true);
        mLineChart.setDescription(description);

        xAxis = mLineChart.getXAxis();
        leftYAxis = mLineChart.getAxisLeft();
        rightYAxis = mLineChart.getAxisRight();

        leftYAxis.setAxisMinimum(0f);
        leftYAxis.setAxisMaximum(100f);
        rightYAxis.setAxisMinimum(0f);
        rightYAxis.setAxisMaximum(100f);
        //右侧Y轴不显示
        rightYAxis.setEnabled(false);

        //X轴设置显示位置在底部
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10, true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(9f);
        //显示边界
        mLineChart.setDrawBorders(true);


        ArrayList<LineDataSet> allLinesList = new ArrayList<LineDataSet>();



        //设置数据
        entries0 = new ArrayList<>();
        entries1 = new ArrayList<>();
        entries2 = new ArrayList<>();
        X_Value = 0;
        for (int i = 0; i < 10; i++) {
            entries0.add(new Entry(i, (float) (Math.random()) * 80));
            entries1.add(new Entry(i, (float) (Math.random()) * 80));
            entries2.add(new Entry(i, (float) (Math.random()) * 80));
            ++X_Value;
        }

        LineDataSet lineDataSet0, lineDataSet1, lineDataSet2;

        //一个LineDataSet就是一条线
        lineDataSet0 = new LineDataSet(entries0, "温度");
        lineDataSet0.setAxisDependency(YAxis.AxisDependency.LEFT);
        //设置线型=曲线
        lineDataSet0.setMode(LineDataSet.Mode.LINEAR);
        //设置曲线值的圆点：false=实心；true=空心
        lineDataSet0.setDrawCircleHole(false);
        //设置折线图内部填充：false=不填充；true=填充
        lineDataSet0.setDrawFilled(false);
        //
        lineDataSet0.setColor(Color.BLUE);
        lineDataSet0.setCircleColor(Color.BLUE);



        //一个LineDataSet就是一条线
        lineDataSet1 = new LineDataSet(entries1, "湿度");
        lineDataSet1.setAxisDependency(YAxis.AxisDependency.LEFT);
        //设置线型=曲线
        lineDataSet1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //设置曲线值的圆点：false=实心；true=空心
        lineDataSet1.setDrawCircleHole(false);
        //设置折线图内部填充：false=不填充；true=填充
        lineDataSet1.setDrawFilled(false);
        //
        lineDataSet1.setColor(Color.GREEN);
        lineDataSet1.setCircleColor(Color.GREEN);


        //一个LineDataSet就是一条线
        lineDataSet2 = new LineDataSet(entries2, "天气");
        lineDataSet2.setAxisDependency(YAxis.AxisDependency.LEFT);
        //设置线型=曲线
        lineDataSet2.setMode(LineDataSet.Mode.LINEAR);
        //设置曲线值的圆点：false=实心；true=空心
        lineDataSet2.setDrawCircleHole(false);
        //设置折线图内部填充：false=不填充；true=填充
        lineDataSet2.setDrawFilled(false);
        //
        lineDataSet2.setColor(Color.DKGRAY);
        lineDataSet2.setCircleColor(Color.DKGRAY);


        LineData data = new LineData(lineDataSet0, lineDataSet1, lineDataSet2);
        mLineChart.setData(data);
        //mLineChart.getLineData().addDataSet(lineDataSet1);
        //mLineChart.getLineData().addDataSet(lineDataSet2);

        lineChartMarkerView = new LineChartMarkerView(this);
        mLineChart.setMarker(lineChartMarkerView);



        //获取布局中自定义的Button控件btn_scan
        btn_Start = (Button) findViewById(R.id.btn_start);
        //设置此控件上显示的文本
        btn_Start.setText(getString(R.string.line_start));
        //为此Button控件对象设置监听
        btn_Start.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        //根据入口传入的View对象获得它的ID，然后散转
        switch (v.getId()) {
            //是按下“扫描键”
            case R.id.btn_start:

                //生成一个新数据
                Entry entry0 = new Entry(X_Value, (float) (Math.random()) * 80);
                Entry entry1 = new Entry(X_Value, (float) (Math.random()) * 80);
                Entry entry2 = new Entry(X_Value, (float) (Math.random()) * 80);

                LineData data = mLineChart.getLineData();
                data.addEntry(entry0, 0);
                data.addEntry(entry1, 1);
                data.addEntry(entry2, 2);

                ++X_Value;
                xAxis.setAxisMinimum(X_Value - 10);
                xAxis.setAxisMaximum(X_Value - 1);

                mLineChart.notifyDataSetChanged();
                mLineChart.invalidate();

                break;
        }
    }


}
