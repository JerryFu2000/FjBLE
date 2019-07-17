package com.clj.blesample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.blesample.adapter.DeviceAdapter;
import com.clj.blesample.comm.ObserverManager;
import com.clj.blesample.hex.hex;
import com.clj.blesample.operation.OperationActivity;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

//Android的四大组件
//Activity：它是所有Android程序的门面，凡是在应用中看得到的东西，都是放在Activity中的。
//Service：在后台运行，即使用户退出应用，Service还是可以继续运行。
//Broadcast Receiver：允许Android程序相互之间收发消息。
//Content Provider：允许Android程序相互之间共享数据。

//Android中Activity的创建都是通类反射的方法创建的，当打开Activity的时候，Android系统框架根据
//配置文件找到相应的Activity对应的类,通过Activity的默认构造函数创建了Activity的实例,然后创建
//应用的上下文环境,通过wrapper的方法把上下文绑定到Activity实例（也是就常说的依赖注入/依赖倒转）,
//然后调用onXXXX接口进行回调.

//Android是使用任务(Task)来管理Activity的，一个Task就是一组存放在栈Stack里的Activity的集合
//这个栈也被称之为返回栈(Back Stack)。默认情况下，当启动一个新的Activity，就会压入Back Stack
//并处于栈顶位置。而当按下Back键或调用finish()方法销毁一个Activity时，处于栈顶的Activity会出栈
//此时前一个入栈的Activity就会重新处于栈顶的位置。Android系统总是会显示处于栈顶的Activity给用户

//Activity的生命周期的每个环节，对应一个回调方法，总共7个回调方法
//onCreate()：在此完成初始化（加载布局，绑定事件等）
//onStart()：当Activity从 不可见 变为 可见 的时候回调
//onResume()：当Activity准备好与用户交互的时候回调
//onPause()：当系统准备去启动或恢复另一个Activity的时候调用。通常会在此将一些消耗CPU的资源释放掉，并保存一些关键数据。
//              但这个方法的执行速度一定要快，不然会影响到新的栈顶Activity的使用
//onStop()：当Activity变为 完全不可见 时回调。它与onPause()的主要区别：若启动的新Activity是个对话框，
//              那么旧的Activity就不是 完全不可见，那么对于旧的Activity只会回调onPause()，而不会回调onStop()
//onDestroy()：当Activity被销毁前回调，一般需要释放掉所有内存
//onRestart()：当Activity由 停止Stop状态 重新进入 启动状态 的时候回调


//Android的异步消息处理主要由4个部分组成：Message，Handler，MessageQueue，Looper。
//Message：它在线程之间传递消息，可以在内部携带少量的信息，用于在不同线程之间交换数据。
//Handler：它主要用于发送和处理消息。发送消息是使用sendMessage()方法，处理消息是使用handlerMessage()方法。
//MessageQueue：它主要用于存放所有通过Handler发送的消息。每个线程中只会有一个MessageQueue对象。
//Looper：它是每个线程中MessageQueue的管家。当调用Looper的loop()方法后，就会进入到一个无限循环当中，
//      然后每当发现MessageQueue中存在一条消息，就会将它取出，并传递到Handler的handleMessage()方法中。
//异步消息处理的流程：
//1.在主线程中创建一个Handler对象，并重写handleMessage()方法。
//2.当子线程中需要进行UI操作时，就创建一个Message对象，并通过Handler将这条消息发送出去，该消息将被添加到MessageQueue中。
//3.Looper一直在检测是否有新消息，当发现有，就从MessageQueue中取出待处理的消息，通过dispatchMessage()方法
//  发送给Handler的handlerMessage()去处理。




public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //类常量static final(即类的多个对象的共享常量)
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_checkPermissions = 2;

    //类变量static(即类的多个对象的共享变量)
    //需要“权限检查、动态授权、功能开启”的权限数组
    private static String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //成员变量(即每个对象私有的变量)
    private LinearLayout layout_setting;
    private TextView txt_setting;
    private Button btn_scan;
    private TextView txt_timer;
    private EditText et_name, et_mac, et_uuid;
    private Switch sw_auto;
    private ImageView img_loading;

    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;

    private volatile boolean exitThread = false;

    private BleScanRuleConfig scanRuleConfig;

    private Long ScanStartedTime;
    private Long DeviceScanedTime;
    private Long ConnectStartedTime;
    private Long DeviceConnectedTime;

    //静态代码块static{}(当类被JVM加载时执行且只执行一次)       --- Android无
    //无

    //类方法static(即类加载后，无需创建对象，就可以被调用的方法)
    //      若要执行类方法，JVM就会先加载这个类，若该类的静态代码块还未执行过，就会先执行它的静态代码块，然后才执行它的类方法
    //无

    //构造代码块{}(即多个构造函数的共享部分)                    --- Android无
    //      当new对象(任何构造形式)且调用相应构造函数前执行
    //无

    //构造函数(与类名相同，可以有多个构造函数，即重载Overload方法)--- Android无
    //无

    //成员方法(即类的多个对象的共享方法)
    //    继承自父类的方法
    //      1.与父类一模一样的方法，不用明写
    //      2.与父类方法名、参数都相同，但内部语句不同，就是重写Override


//Bundle类型的数据与Map类型的数据相似，都是以key-value的形式存储数据的。
//当Activity创建时，通过传入的savedInstanceState，获得Activity的状态
//@Override
//public void onCreate(Bundle savedInstanceState) {
//  super.onCreate(savedInstanceState);
//  //若传入的为null，说明是第一次运行
//  if (savedInstanceState == null) {
//		mSnakeView.setMode(SnakeView.READY);
//	}
//  //若传入的不为null
//  else {
//		//读取这个Activity的状态
//		Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
//	   	//若状态不为null，则恢复为上次的状态
//		if (map != null) {
//			mSnakeView.restoreState(map);
//		}
//	   	//若状态为null，则恢复为暂停状态
//     	else {
//			mSnakeView.setMode(SnakeView.PAUSE);
//		}
//	}
//
//onSaveInstanceState方法是用来保存Activity的状态的。当一个Activity在生命周期结束前，会调用该方法保存状态。这个方法有一个参数名称与onCreate方法参数名称相同。如下所示：
//
//public void onSaveInstanceState(Bundle savedInstanceState){
//	super.onSaveInstanceState(savedInsanceState);
//}
//
//重写onSavedInstanceState()，此方法会在Activity结束时调用
//@Override
//public void onSaveInstanceState(Bundle outState) {
//	//Store the game state
//	outState.putBundle(ICICLE_KEY, mSnakeView.saveState());
//}
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //调用父类的构造函数
        super.onCreate(savedInstanceState);
        //为本Activity引入布局
        setContentView(R.layout.activity_main);
        //初始化View
        initView();

        //先获取本应用的实例，然后用它作为参数传入，这样BleManager内部就可以保存好本程序实例
        //然后在BleManager内部调用一些方法，必须用到“本程序实例”，例如：context.getSystemService(Context.BLUETOOTH_SERVICE)
        //初始化BleManager
        BleManager.getInstance().init(getApplication());

        //设置BLE的连接参数
        BleManager.getInstance()
                .enableLog(true)            //Log功能=使能
                .setReConnectCount(1, 5000)//重连次数=1，重连时间间隔=5000ms
                .setConnectOverTime(20000)  //
                .setOperateTimeout(5000);   //操作超时=5000ms

        //判断本设备是否支持BLE
        if(BleManager.getInstance().isSupportBle()) {
            Toast.makeText(MainActivity.this, "恭喜！此设备支持BLE", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(MainActivity.this, "抱歉！此设备不支持BLE！", Toast.LENGTH_SHORT).show();
        }

//        //期望通过广播的方式获得RSSI，但未能实现
//        // 注册开始发现广播。 
//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
//        this.registerReceiver(mReceiver, filter);

    }

//    //期望通过广播的方式获得RSSI，但未能实现
//    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            //当设备开始扫描时。
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                //从Intent得到blueDevice对象
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//
//                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//
//                    //信号强度。 
//                    short RSSI_val = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
//                    mDeviceAdapter.getItem(1).setRssi((int)RSSI_val);
//                    mDeviceAdapter.notifyDataSetChanged();
//                }
//            }
//        }
//    };




    //当Activity进入前台，能与用户交互时
    @Override
    protected void onResume() {
        super.onResume();
        //显示当前已连接设备
        //1.获得最新的已连接设备的deviceList
        //2.移除bleDeviceList中旧的已连接设备
        //3.将deviceList中的设备添加到bleDeviceList中
        //4.通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
        showConnectedDevice();
    }

    //当Activity销毁时
    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitThread = true;//结束线程
        //断开所有设备，并销毁BleManager
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    //本Activity上的按键按下后的回调，入口参数是View对象，即按下的具体是哪个按键
    @Override
    public void onClick(View v) {
        //根据入口传入的View对象获得它的ID，然后散转
        switch (v.getId()) {
            //是按下“扫描键”
            case R.id.btn_scan:
                //若当前按键上显示“开始扫描”，则先检测权限是否已开启
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
                    exitThread = false;
                    //通过匿名类的方法创建一个新线程，并启动此线程
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //计时器
                            int i = 0;
                            //通过一个标志位来实现：其它线程可以结束本线程
                            while (!exitThread) {
                                //令本线程休眠1s
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                //创建一个Message类对象message
                                Message message = handler.obtainMessage();
                                //在message中填入数据（即当前计时值）
                                message.what = 1;
                                message.arg1 = i++;
                                message.obj = "计时=";
                                //发送消息给其它线程
                                handler.sendMessage(message);
                            }
                        }
                    }).start();
                }
                //若当前按键上显示“停止扫描”，则停止扫描
                else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    BleManager.getInstance().cancelScan();
                }
                break;

            //是按下“展开搜索设置键”
            case R.id.txt_setting:
                //若当前搜索设置区域是可见的，则令其GONE，并令按键上显示“展开搜索设置”
                if (layout_setting.getVisibility() == View.VISIBLE) {
                    layout_setting.setVisibility(View.GONE);
                    txt_setting.setText(getString(R.string.expand_search_settings));
                }
                //若当前搜索设置区域是不可见的，则令其可见，并令按键上显示“收起搜索设置”
                else {
                    layout_setting.setVisibility(View.VISIBLE);
                    txt_setting.setText(getString(R.string.retrieve_search_settings));
                }
                break;
        }
    }

    //3.Looper一直在检测是否有新消息，当发现有，就从MessageQueue中取出待处理的消息，通过dispatchMessage()方法
    //  发送给Handler的handlerMessage()去处理。
    private Handler handler= new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //取出Message内携带的数据
            int arg1 = msg.arg1;
            String info= (String) msg.obj;
            //通过what来判断是哪个线程发来的消息，然后进行相应处理
            if (msg.what==1){
                //在文本框中显示“计时=x”
                txt_timer.setText(info+arg1);
            }
        }
    };


//通过调用findViewById来获取布局中的控件元素
//它的完整形式是this.findViewById()
//		readText = (EditText) findViewById(R.id.ReadValues);
//		writeText = (EditText) findViewById(R.id.WriteValues);
//若所需要的控件不存在本Activity的布局中，那么在获取时需改为：
//		ImageView view=(ImageView)view.findViewById(R.id.imageview);

    //成员方法(即类的多个对象的共享方法)
    //本类新增的方法：1.完全新增的方法
    private void initView() {
        //获取布局中自定义的Toolbar控件toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //设定用自定义的Toolbar控件toolbar来替代原本的ActionBar
        setSupportActionBar(toolbar);

        //获取布局中自定义的Button控件btn_scan
        btn_scan = (Button) findViewById(R.id.btn_scan);
        //设置此控件上显示的文本
        btn_scan.setText(getString(R.string.start_scan));
        //为此Button控件对象设置监听
        btn_scan.setOnClickListener(this);

        //获取布局中自定义的TextView控件txt_timer
        txt_timer = (TextView) findViewById(R.id.txt_timer);
        //设置此控件上显示的文本
        txt_timer.setText("倒计时");

        //令以下LinearLayout(包含有1个tv，3个et，1个sw)暂时消失
        //获取布局中自定义的EditText控件et_name
        et_name = (EditText) findViewById(R.id.et_name);
        //获取布局中自定义的EditText控件et_mac
        et_mac = (EditText) findViewById(R.id.et_mac);
        //获取布局中自定义的EditText控件et_uuid
        et_uuid = (EditText) findViewById(R.id.et_uuid);
        //获取布局中自定义的Switch控件sw_auto
        sw_auto = (Switch) findViewById(R.id.sw_auto);
        //获取布局中自定义的LinearLayout控件layout_setting
        layout_setting = (LinearLayout) findViewById(R.id.layout_setting);
        //令自定义的LinearLayout控件layout_setting消失
        layout_setting.setVisibility(View.GONE);

        //获取布局中自定义的TextView控件txt_setting
        txt_setting = (TextView) findViewById(R.id.txt_setting);
        //设置此控件上显示的文本
        txt_setting.setText(getString(R.string.expand_search_settings));
        //为此TextView控件对象设置监听
        txt_setting.setOnClickListener(this);

        //获取布局中自定义的ImageView控件img_loading
        img_loading = (ImageView) findViewById(R.id.img_loading);

        //从指定的xml中获取并创建动画对象，并用operatingAnim来指向它
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        //为动画对象设置“插值器”（它是一个指定动画如何变化的属性）
        operatingAnim.setInterpolator(new LinearInterpolator());

        //创建进度对话框对象，并用progressDialog来指向它
        progressDialog = new ProgressDialog(this);

//匿名内部类：
//它会隐式的继承一个类或者实现一个接口，也可以说，匿名内部类是一个继承了该类或者实现了该接口的“子类匿名对象”。
//1.不需定义类名，用父类构造器直接创建出匿名内部类的对象来使用，且只能使用一次。
//2.因为创建出来的对象无名，所以在方法的参数列表()中创建然后立刻传入给方法。
//      xxx方法(new 父类构造器(参数列表) 或 实现接口() {
//	        匿名内部类的类体部分，一般需要重写Override父类的方法，来实现具体的操作
//      });
//例如按键的单击监听器
//		//为“发送键”设置单击监听器
//		writeButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				......
//			}
//		});
//      //采用Lambda写法如下
        //当方法的入口参数是一个接口，且该接口只有一个待实现的方法，就可以采用Lambda表达式
//      writeButton.setOnClickListener((view)-> {
//				......
//		});


//Intent是程序中各个组件之间进行交互的一种重要方式，它不仅可以指明当前组件想要执行的动作，还可以传递数据。
//它一般可用于启动Activity，启动Service，发送Broadcast等场景。  Intent分为两种：显式，隐式。
//隐式Intent：在AndroidManifest.xml中的<intent-filter>中进行设置
//------------------------------------------------------------------------
//显式Intent（只向想要启动的Activity发送数据）：
//发送方Activity
//1.创建一个Intent类对象intent
//      入口参数1（启动活动的上下文）= MainActivity.this
//      入口参数2（想要启动的Activity）= AdvDataActivity.class
//  Intent intent = new Intent(MainActivity.this, AdvDataActivity.class);
//2.向intent中存入一个键值对(类似JSON)：键=extra_data，值=deviceKey+"\n"+advdata
//  intent.putExtra("extra_data", deviceKey+"\n"+advdata);
//3.用预先设置好的intent来启动一个Activity
//  startActivity(intent);
//接收方Activity
//1.创建Intent类对象intent，并通过getIntent()方法来获得启动本Activity时传入的Intent
//  Intent intent = getIntent();
//2.通过传入的“键”来获得相应的“值”
//  String data = intent.getStringExtra("extra_data");//若传入的是String型数据
//  Integer data = intent.getIntExtra("extra_data");//若传入的是Integer型数据
//  Boolean data = intent.getBooleanExtra("extra_data");//若传入的是Boolean型数据
//------------------------------------------------------------------------
//显式Intent（向想要启动的Activity发送数据，并且当启动的Activity销毁时返回数据）：
//发送方Activity
//1.创建一个Intent类对象intent
//      入口参数1（启动活动的上下文）= MainActivity.this
//      入口参数2（想要启动的Activity）= AdvDataActivity.class
//  Intent intent = new Intent(MainActivity.this, AdvDataActivity.class);
//或者
//  此处是跳转到Android系统内部的“定位设置界面”
//  Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//2.向intent中存入一个键值对(类似JSON)：键=extra_data，值=deviceKey+"\n"+advdata
//  intent.putExtra("extra_data", deviceKey+"\n"+advdata);
//3.用预先设置好的intent来启动一个新Activity
//  startActivityForResult(intent, XXX);
//4.当开启的新Activity销毁时返回数据，回调本函数
//      //参数1=请求码，即用startActivityForResult开启了哪个新Activity，就从这这个Activity返回了数据
//      //参数2=结果码（RESULT_OK 或 RESULT_CANCELED）
//      //参数3=返回数据
//  @Override
//  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//      super.onActivityResult(requestCode, resultCode, data);
//      //根据“请求码”散转处理。
//      switch(requestCode){
//          //若是从XXX的Activity返回
//          case XXX:
//              if (resultCode == RESULT_OK) {
//                  ......
//              }
//          break;
//          //若是从YYY的Activity返回
//          case YYY:
//              if (resultCode == RESULT_OK) {
//                  ......
//              }
//          break;
//          default:
//          break;
//      }
//  }
//接收方Activity
//1.创建Intent类对象intent，并通过getIntent()方法来获得启动本Activity时传入的Intent
//  Intent intent = getIntent();
//2.通过传入的“键”来获得相应的“值”
//  String data = intent.getStringExtra("extra_data");//若传入的是String型数据
//  Integer data = intent.getIntExtra("extra_data");//若传入的是Integer型数据
//  Boolean data = intent.getBooleanExtra("extra_data");//若传入的是Boolean型数据
//3.重写本Activity的Back键方法，那么当本Activity销毁前就能返回数据了。
//  @Override
//  public void onBackPressed(){
//      Intent intent = new Intent();
//      intent.putExtra("return_data", "This is a return_data!");
//      setResult(RESULT_OK, intent);
//      finish();
//  }



        //创建适配器类对象，并用mDeviceAdapter来指向它
        mDeviceAdapter = new DeviceAdapter(this);
        //为DeviceAdapter类对象设置监听
        //此处的监听器是一个DeviceAdapter.OnDeviceClickListener回调接口，它包含了4个回调的钩子
        //因此需要在此处具体实现这4个回调函数的功能
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            //当点击表格中“显示一个item用的View”内的“整个item”后进行回调
            @Override
            public void onAdvData(BleDevice bleDevice) {
                //获得点击的item对应的bleDevice的“名称+地址”
                String deviceKey = bleDevice.getKey();
                //获得点击的item对应的bleDevice的“扫描结果”的byte[]型数据
                byte[] advdata_byte = bleDevice.getScanRecord();
                //将byte[]型数据转换为String
                String advdata = hex.byteArray_to_String(advdata_byte);
                //创建一个Intent类对象intent
                //入口参数1（启动活动的上下文）= MainActivity.this
                //入口参数2（想要启动的Activity）= AdvDataActivity.class
                Intent intent = new Intent(MainActivity.this, AdvDataActivity.class);
                //向intent中存入一个键值对(类似JSON)：键=extra_data，值=deviceKey+"\n"+advdata
                intent.putExtra("extra_data", deviceKey+"\n"+advdata);
                //用预先设置好的intent来启动一个Activity
                startActivity(intent);
                Toast.makeText(MainActivity.this, "AdvData", Toast.LENGTH_SHORT).show();
            }

            //当点击表格中“显示一个item用的View”内的“连接”键后进行回调
            @Override
            public void onConnect(BleDevice bleDevice) {
                //若尚未连接，则停止扫描，连接设备
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            //当点击表格中“显示一个item用的View”内的“断开”键后进行回调
            @Override
            public void onDisConnect(final BleDevice bleDevice) {
                //若已连接，则断线
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice);
                }
            }

            //当点击表格中“显示一个item用的View”内的“进入”键后进行回调
            @Override
            public void onDetail(BleDevice bleDevice) {
                //若已连接
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    //创建一个Intent类对象intent
                    //入口参数1（启动活动的上下文）= MainActivity.this
                    //入口参数2（想要启动的Activity）= OperationActivity.class
                    Intent intent = new Intent(MainActivity.this, OperationActivity.class);
                    //向intent中存入一个键值对(类似JSON)：键=KEY_DATA，值=bleDevice(即当前已连接的设备)
                    intent.putExtra(OperationActivity.KEY_DATA, bleDevice);
                    //用预先设置好的intent来启动一个Activity
                    startActivity(intent);
                }
            }
        });

        //获取布局中自定义的ListView控件listView_device
        //后续将用此ListView来列出所有扫描到的设备信息
        ListView listView_device = (ListView) findViewById(R.id.list_device);
        //为此ListView控件对象设置适配器
        listView_device.setAdapter(mDeviceAdapter);
    }

    //显示当前已连接设备
    //1.获得最新的已连接设备的deviceList
    //2.移除bleDeviceList中旧的已连接设备
    //3.将deviceList中的设备添加到bleDeviceList中
    //4.通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
    private void showConnectedDevice() {
        //获得所有已连接设备的deviceList
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        //将已连接的设备从bleDeviceList中移出
        mDeviceAdapter.clearConnectedDevice();
//for(循环变量类型  循环变量名称 : 要被遍历的对象){
//      循环体；
//}
        //将deviceList中的设备添加到bleDeviceList中
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
        mDeviceAdapter.notifyDataSetChanged();
    }

    //设置扫描规则
    private void setScanRule() {
        String[] uuids;
        //获取用户输入的UUID字符串。若未输入，则为null；若有输入，则用,来分割为String数组
        String str_uuid = et_uuid.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        //先声明一个空的UUID型数组
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            //用户输入的 可用,分割的UUID有几组，就创建相应大小的UUID类型的数组
            serviceUuids = new UUID[uuids.length];
            //依次检查每个用,分割出来的UUID是否合格
            for (int i = 0; i < uuids.length; i++) {
                //先获取一个UUID，然后用"-"来分割
                String name = uuids[i];
                String[] components = name.split("-");
                //若分割出来的数组元素不是5个，则认为不合格
                if (components.length != 5) {
                    serviceUuids[i] = null;
                }
                //若分割出来的数组元素是5个，则转换为UUID类型，存入serviceUuids数组中
                else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        //获取用户输入的设备名称。若未输入，则为null；若有输入，则用,来分割为String数组
        String str_name = et_name.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

        //获取用户输入的MAC地址
        //MAC地址的输入格式：xx:xx:xx:xx:xx:xx
        String mac = et_mac.getText().toString();

        //获取用户输入的“自动连接”选项
        boolean isAutoConnect = sw_auto.isChecked();

        //构建扫描规则
        scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        //用刚构建好的扫描规则 来 初始化扫描规则
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    //启动扫描
    private void startScan() {
        //在此设置扫描时间
        scanRuleConfig.setScanTimeOut(5000);
        //启动扫描，因为无法立刻完成扫描，所以需要注册并实现具体的回调函数
        //此处的BleScanCallback是一个回调接口，它包含了4个回调的钩子
        //因此需要在此处具体实现这4个回调函数的功能
        BleManager.getInstance().scan(new BleScanCallback() {
            //当开始扫描时，进行回调
            @Override
            public void onScanStarted(boolean success) {
                //清除bleDeviceList
                mDeviceAdapter.clearScanDevice();
                //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
                mDeviceAdapter.notifyDataSetChanged();
                //在ImageView区域启动动画
                img_loading.startAnimation(operatingAnim);
                //令ImageView可见
                img_loading.setVisibility(View.VISIBLE);
                //在扫描键上显示“停止扫描”
                btn_scan.setText(getString(R.string.stop_scan));

                ScanStartedTime = System.currentTimeMillis();

            }

            //当扫描完一个BLE设备后，进行回调
            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
                //获得本次扫描的bleDevice的“扫描结果”的byte[]型数据
                byte[] scanrecord = bleDevice.getScanRecord();
                //将byte[]型数据转换为String
                String encoded = hex.byteArray_to_String(scanrecord);
                //获取当前的系统时间，它返回的是1970年1月1日0点到现在经过的毫秒数
                DeviceScanedTime = System.currentTimeMillis();
                long timeNow = DeviceScanedTime;

                if(bleDevice.getMac().equals("0C:61:CF:AB:24:E6")){
//                    Toast.makeText(MainActivity.this, encoded, Toast.LENGTH_SHORT).show();
//                    Toast.makeText(MainActivity.this, ""+time, Toast.LENGTH_SHORT).show();
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
                //获得该设备最新的RSSI值
                int rssi = bleDevice.getRssi();
                //依次检查适配器内的设备List中的每个设备
                for(int i=0; i<mDeviceAdapter.getCount(); i++){
                    //找到当前扫描完成的设备 在 设备List中的位置
                    //通过MAC地址来匹配查找
                    if(bleDevice.getMac().equals(mDeviceAdapter.getItem(i).getMac())){
                        //将最新RSSI值保存到此设备的RSSI属性中，便于下次刷新时能显示出来
                        mDeviceAdapter.getItem(i).setRssi(rssi);
                        //计算出本设备从 上次扫描 到 本次扫描 之间的时间间隔
                        long time = timeNow - mDeviceAdapter.getItem(i).getTimestampNanos();
                        //将本次的时间，保存到本设备中，便于下次再计算扫描时间间隔
                        mDeviceAdapter.getItem(i).setTimestampNanos(timeNow);
                        //将本次与上次之间的扫描时间间隔 保存到本设备中便于下次刷新时能显示出来
                        mDeviceAdapter.getItem(i).setTime(time);
                    }
                }
                //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
                mDeviceAdapter.notifyDataSetChanged();
            }

            //当扫描到一个新设备后，进行回调
            //入口：bleDevice --- 扫描到的一个新设备
            @Override
            public void onScanning(BleDevice bleDevice) {
                //将扫描到的新设备添加到ListView适配器的数据源中
                mDeviceAdapter.addDevice(bleDevice);
                //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
                mDeviceAdapter.notifyDataSetChanged();
            }

            //扫描完成后，进行回调
            //入口：scanResultList --- 扫描到的Ble设备列表
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                //令ImageView区域动画消失，并且不可见
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                //在扫描键上显示“开始扫描”
                btn_scan.setText(getString(R.string.start_scan));
                exitThread = true;//结束线程
                String str = "";
                str += "ScanStartedTime = " + ScanStartedTime + "\n";
                str += "DeviceScanedTime = " + DeviceScanedTime + "\n";
                str += "Scan_Time = " + (DeviceScanedTime-ScanStartedTime) + "ms\n";
                for(int i=0; i<scanResultList.size(); i++){
                    str += scanResultList.get(i).getKey() + "\n";
                }
                //创建一个Intent类对象intent
                //入口参数1（启动活动的上下文）= MainActivity.this
                //入口参数2（想要启动的Activity）= AdvDataActivity.class
                Intent intent = new Intent(MainActivity.this, AdvDataActivity.class);
                //向intent中存入一个键值对(类似JSON)：键=extra_data，值=str(即扫描结果的汇总)
                intent.putExtra("extra_data", str);
                //用预先设置好的intent来启动一个Activity
                startActivity(intent);
            }
        });
    }

    //连接指定设备
    private void connect(final BleDevice bleDevice) {
        //启动连接入口传入的BLE设备，因为无法立刻完成连接，所以需要注册并实现具体的回调函数
        //此处的BleGattCallback是一个回调接口，它包含了4个回调的钩子
        //因此需要在此处具体实现这4个回调函数的功能
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            //当开始连接时，进行回调
            //显示进度条progressDialog
            @Override
            public void onStartConnect() {
                ConnectStartedTime = System.currentTimeMillis();
                progressDialog.show();
            }

            //当连接失败时，进行回调
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                //销毁进度条progressDialog
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            //当连接成功时，进行回调
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                DeviceConnectedTime = System.currentTimeMillis();
                //销毁进度条progressDialog
                progressDialog.dismiss();
                //添加设备
                mDeviceAdapter.addDevice(bleDevice);
                //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
                mDeviceAdapter.notifyDataSetChanged();

                setMtu(bleDevice, 247);
//-----------------------------------------------------------------------------------------
//通过下述一些方法，获得已连接设备的内部服务及特征值等信息，并启动新AdvDataActivity来显示这些数据
                String str = "";
                str += "Scan_Time = " + (DeviceScanedTime-ScanStartedTime) +"ms\n";
                str += "Connect_Time = " + (DeviceConnectedTime-ConnectStartedTime) +"ms\n";
                for(int i=0; i<gatt.getServices().size(); i++){
                    str += "\n服务"+i+"\n"
                            //获得指定服务的UUID
                            +gatt.getServices().get(i).getUuid().toString()
                            //获得指定服务的类型
                            +"\nType="+ gatt.getServices().get(i).getType();
                }
                for(int i=0; i<gatt.getServices().get(3).getCharacteristics().size(); i++){
                    //获得指定服务下指定特征值的属性
                    int charaProp = gatt.getServices().get(3).getCharacteristics().get(i).getProperties();
                    String property = "";
                    //若属性中包含有READ，则添加“Read, ”
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        property += "Read";
                        property += " , ";
                    }
                    //若属性中包含有WRITE，则添加“Write, ”
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                        property += "Write";
                        property += " , ";
                    }
                    //若属性中包含有WRITE_NO_RESPONSE，则添加“Write No Response, ”
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                        property += "Write No Response";
                        property += " , ";
                    }
                    //若属性中包含有NOTIFY，则添加“Notify, ”
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        property += "Notify";
                        property += " , ";
                    }
                    //若属性中包含有INDICATE，则添加“Indicate, ”
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                        property += "Indicate";
                        property += " , ";
                    }
                    str += "\n特征"+i+"\n"
                            +gatt.getServices().get(3).getCharacteristics().get(i).getUuid().toString()
                            +"\n属性="+ property;
                }


                Intent intent = new Intent(MainActivity.this, AdvDataActivity.class);
                intent.putExtra("extra_data", str);
                startActivity(intent);
            }

            //当断线时，进行回调
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                //销毁进度条progressDialog
                progressDialog.dismiss();
                //移除设备
                mDeviceAdapter.removeDevice(bleDevice);
                //通知所有观察者(即向所有订阅者发布)，数据已发生变化，即刷新UI
                mDeviceAdapter.notifyDataSetChanged();

                //若是主动断线
                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                }
                //若是被动断线
                else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }
            }
        });

    }

    //读取指定BLE设备的RSSI值
    private void readRssi(BleDevice bleDevice) {
        //启动读RSSI，因为无法立刻读到，所以需要注册并实现具体的回调函数
        //此处的BleRssiCallback是一个回调接口，它包含了2个回调的钩子
        //因此需要在此处具体实现这2个回调函数的功能
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            //当读取RSSI失败时，进行回调
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            //当读取RSSI成功时，进行回调
            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    //设置指定BEL设备的MTU（即一次传输中的用户最多数据的字节数+3）
    //对于BLE4.0/4.1：MTU=20+3=23字节
    //对于BLE4.2/4.1：MTU=244+3=247字节
    //注意：本函数必须在连接成功后才能调用
    private void setMtu(BleDevice bleDevice, int mtu) {
        //启动设置MTU，因为无法立刻设置，所以需要设置回调
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            //当设置失败
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
                Toast.makeText(MainActivity.this, "设置MTU失败！", Toast.LENGTH_LONG).show();
            }

            //当设置完成
            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
                Toast.makeText(MainActivity.this, "设置MTU成功！", Toast.LENGTH_LONG).show();
            }
        });
    }


    //通过ActivityCompat.requestPermissions()弹出对话框，等用户操作完授权后的回调
    //参数1=当时的请求码，即是谁发起了本次授权进程
    //参数2=待授权权限的数组，因为在一次授权进程中可能会提出多个权限让用户处理，所以需要采用数组的形式
    //参数3=用户授权的结果的数组，值为PackageManager.PERMISSION_GRANTED 或 PackageManager.PERMISSION_DENIED
    //    因为在一次授权进程中可能会提出多个权限让用户处理，所以需要采用数组的形式
    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //根据请求码进行散转，即依此来判断是谁发起了本次授权进程
        switch (requestCode) {
            //是checkPermissions发起的授权进程
            case REQUEST_CODE_checkPermissions:
                //若授权结果数组中元素个数>0，说明权限请求被用户处理过
                if (grantResults.length > 0) {
                    //依次判断用户的授权结果
                    for (int i = 0; i < grantResults.length; i++) {
                        //若获得授权
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            //对已被用户授权的权限进行后续处理（例如若已授权了精准定位的权限，则在此处提示用户跳转到相应的设置页面去打开GPS）
                            onPermissionGranted(permissions[i]);
                        }
                        //若未获得授权
                        else{
                            Toast.makeText(this, "未授权"+permissions[i] , Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    //检测权限是否已开启
    //1.获取BluetoothAdapter类对象，通过它来检测蓝牙是否已打开，若未打开则Toast提示后退出
    //2.已打开蓝牙，则进行“权限检查、动态授权、功能开启”
    //  (1)在String型数组permissions中填入期望申请的权限，每个权限占用一个元素位置
    //  (2)依次处理permissions中的每个元素
    //    读取本权限的状态，若已被授权，则进一步提示用户跳转到相应的设置页面去设置，例如对于精准定位的权限，将跳转去打开GPS
    //              若未被授权，则将它加入到permissionDeniedList
    //3.若permissionDeniedList不为空，说明有权限未被授权，通过ActivityCompat类的requestPermissions()方法来弹出对话框，让用户进行授权

    private void checkPermissions() {
        //1.获取BluetoothAdapter类对象，通过它来检测蓝牙是否已打开，若未打开则Toast提示后退出
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        //执行到此处说明已打开蓝牙
        //2.已打开蓝牙，则进行“权限检查、动态授权、功能开启”
        //  (1)在String型数组permissions中填入期望申请的权限，每个权限占用一个元素位置
        //  (2)依次处理permissions中的每个元素
        //    读取本权限的状态，若已被授权，则进一步提示用户跳转到相应的设置页面去设置，例如对于精准定位的权限，将跳转去打开GPS
        //              若未被授权，则将它加入到permissionDeniedList

        List<String> permissionDeniedList = new ArrayList<>();
        //依次判断permissions权限数组中每个待检查的权限
        for (String permission : permissions) {
            //获取本权限的状态(已授权or未授权)
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            //若本权限已被授权
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                //对已被用户授权的权限进行后续处理（例如若已授权了精准定位的权限，则在此处提示用户跳转到相应的设置页面去打开GPS）
                onPermissionGranted(permission);
            }
            //若本权限未被授权，则加入到permissionDeniedList中
            else {
                permissionDeniedList.add(permission);
            }
        }
        //3.若permissionDeniedList不为空，说明有权限未被授权，通过ActivityCompat类的requestPermissions()方法来弹出对话框，让用户进行授权
        if (!permissionDeniedList.isEmpty()) {
            //将permissionDeniedList转换为String型数组
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            //通过ActivityCompat类的requestPermissions()方法来弹出对话框，让用户进行授权
            //参数1=上下文
            //参数2=希望被授权的权限的String型数组
            //参数3=请求码（即谁发起了授权进程，后续当用户操作完成回调onRequestPermissionsResult时，作为依据）
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_checkPermissions);
        }
    }

    //对已被用户授权的权限进行后续处理（例如若已授权了精准定位的权限，则在此处提示用户跳转到相应的设置页面去打开GPS）
    //参数=已被用户授权的权限
    //调用本函数的前提：
    //1.当用户按下扫描键后，在checkPermissions()中检测到已授权，则调用onPermissionGranted()进行后续处理
    //2.当用户按下扫描键后，在checkPermissions()中检测到未授权，则ActivityCompat.requestPermissions()弹出授权对话框
    //  等用户操作后回调onRequestPermissionsResult()中判断若已授权，则调用onPermissionGranted()进行后续处理
    private void onPermissionGranted(String permission) {
        //根据“已被用户授权的权限”进行散转
        switch (permission) {
            //若是“访问精准定位”的权限，例如GPS
            case Manifest.permission.ACCESS_FINE_LOCATION:
                //若 当前设备的API_Level>=23(M版) 且 GPS未打开
                //则弹出对话框，提示用户去打开Android系统内部的“定位设置界面”去打开GPS
                if ( (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (!checkGPSIsOpen()) ) {
                    //构建对话框
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            //若按下“取消键”则直接finish()
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            //若按下“前往设置”则
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            //构建Intent
                                            //此处是跳转到Android系统内部的“定位设置界面”
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            //启动新的Activity，并携带请求码=REQUEST_CODE_OPEN_GPS
                                            //这样当新Activity销毁时会返回数据及此请求码并回调本Activity的onActivityResult()
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })
                            //不显示第三个按键
                            .setCancelable(false)
                            //显示对话框
                            .show();

                //openGPS(this);  //强行打开GPS的方法不行！！！
                }
                //若 当前设备的API_Level<23(M版) 或者 GPS已打开
                else {
                    //设置扫描规则，启动扫描
                    setScanRule();
                    startScan();
                }
            break;
            //若是“访问外部存储器”的权限
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                Toast.makeText(this, "已允许访问外部存储器！", Toast.LENGTH_SHORT).show();
            break;
        }
    }

    //检测GPS是否已打开
    //返回：true=GPS已打开；false=GPS已关闭
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }


    //当由本Activity用startActivityForResult开启的新Activity销毁且返回数据时，回调本函数
    //参数1=请求码，即用startActivityForResult开启了哪个新Activity，就从这这个Activity返回了数据
    //参数2=结果码
    //参数3=返回数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //根据“请求码”散转处理
        //若是从“设置GPS”的Activity返回
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            //若已打开GPS
            if (checkGPSIsOpen()) {
                //设置扫描规则，然后启动扫描
                setScanRule();
                startScan();
            }
            //此处若用户没有打开GPS的话......
            else{
                Toast.makeText(this,"没有打开GPS！", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
