package com.clj.blesample.operation;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;

import com.clj.blesample.R;
import com.clj.blesample.comm.Observer;
import com.clj.blesample.comm.ObserverManager;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;


//继承了AppCompatActivity类，并实现了Observer接口
public class OperationActivity extends AppCompatActivity implements Observer {

    //类常量static final(即类的多个对象的共享常量)
    public static final String KEY_DATA = "key_data";

    //类变量static(即类的多个对象的公共变量)
    //无

    //成员变量(即每个对象独有的变量)
    private BleDevice bleDevice;    //MainActivity通过Intent传入的“当前已连接的设备”
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    private Toolbar toolbar;
    private List<Fragment> fragments = new ArrayList<>();
    private int currentPage = 0;
    private String[] titles = new String[3];
    //3个字符串如下：
    //  服务列表
    //  特征列表
    //  操作控制台


    //成员方法(即类的多个对象的共享方法)
    //继承自父类的方法：2.与父类方法名、参数都相同，但内部语句不同，就是重写Override
    //当本Activity创建时，进行回调
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //为本Activity引入布局
        setContentView(R.layout.activity_operation);
        //初始化数据
        initData();
        //初始化View
        initView();
        //初始化3个Fragment页面，并切换到Fragment页面0（即第一个Fragment页面：ServiceListFragment）
        initPage();
        //把本Activity加入到观察者(订阅者)队列中
        ObserverManager.getInstance().addObserver(this);
    }

    //当本Activity销毁时，进行回调
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //清除指定的bleDevice的4类回调用的HashMap的内容
        //bleNotifyCallbackHashMap
        //bleIndicateCallbackHashMap
        //bleWriteCallbackHashMap
        //bleReadCallbackHashMap
        BleManager.getInstance().clearCharacterCallback(bleDevice);
        //把本Activity移除出观察者(订阅者)队列
        ObserverManager.getInstance().deleteObserver(this);
    }

    //当断开连接时，进行回调
    //因为要实现Observer接口，所以需要实现接口中的disConnected()方法
    //即作为观察者(订阅者)，当收到被观察者(发布者)发来的通知后，相应的处理
    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            //结束本Activity
            finish();
        }
    }

    //当按下Android系统的按键时，进行回调
    //参数1 --- 被按下的按键的键码值
    //参数2 ---
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //若是按下“后退”键
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            //若当前页面号!=0，说明还不是第一个页面，因此可以后退一页
            if (currentPage != 0) {
                currentPage--;
                changePage(currentPage);
                //因为我们代替系统处理了我们想处理的系统按键，不希望再被其他方法处理，所以提前返回true
                return true;
            }
            //若当前页面号==0，说明已是第一个页面，因此结束本Activity
            else {
                finish();
                //因为我们代替系统处理了我们想处理的系统按键，不希望再被其他方法处理，所以提前返回true
                return true;
            }
        }
        //执行到此处，说明按下的系统按键都不是我们希望的，所以将这些按键按下的处理交还给系统自己处理，并且return
        return super.onKeyDown(keyCode, event);
    }

    //初始化本Activity的View
    private void initView() {
        //引入Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //为此Toolbar设置标题，先设置为第一个字符串
        //3个字符串如下：
        //  服务列表
        //  特征列表
        //  操作控制台
        toolbar.setTitle(titles[0]);
        //令自定义的Toolbar类控件toolbar来替代标准的ActionBar控件
        setSupportActionBar(toolbar);
        //允许在左上角图标的左边加上一个返回的图标
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //设置Toolbar的后退键的作用：令页面后退一页
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //若当前页面号!=0，说明还不是第一个页面，因此可以后退一页
                if (currentPage != 0) {
                    currentPage--;
                    changePage(currentPage);
                }
                //若当前页面号==0，说明已是第一个页面，因此结束本Activity
                else {
                    finish();
                }
            }
        });
    }

    //初始化
    private void initData() {
        //取出Intent中携带的数据，通过键来获得对应的值
        //类似JSON
        //键=KEY_DATA
        //值=bleDevice(即当前已连接的设备)
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        //若传递进来的bleDevice为null，说明当前没有连接到BLE设备，或出错，则结束本Activity
        if (bleDevice == null)
            finish();

        //初始化Toolbar上显示标题数组
        //因为操作分为3个层次，所以需要3个标题
        titles = new String[]{
                getString(R.string.service_list),
                getString(R.string.characteristic_list),
                getString(R.string.console)};
    }

    //初始化3个Fragment页面，并切换到Fragment页面0（即第一个Fragment页面：ServiceListFragment）
    private void initPage() {
        //准备Fragment
        //依次创建3个不同用途的Fragment类型的对象，然后添加到List<Fragment>中
        //向getSupportFragmentManager().beginTransaction()依次添加List中的3个Fragment，隐藏，然后commit
        prepareFragment();
        //切换到Fragment页面0（即第一个Fragment页面：ServiceListFragment）
        changePage(0);
    }

    //切换Fragment页面
    public void changePage(int page) {
        //令currentPage=入口传入的page号
        currentPage = page;
        //根据入口传入的page号来更新Toolbar上显示的文本
        toolbar.setTitle(titles[page]);
        //根据入口传入的page号来显示期望的Fragment
        updateFragment(page);
        //若是第二个Fragment页面，从页面List中取出相应的页面对象，然后showData相应数据
        if (currentPage == 1) {
            ((CharacteristicListFragment) fragments.get(1)).showData();
        }
        //若是第三个Fragment页面，从页面List中取出相应的页面对象，然后showData相应数据
        else if (currentPage == 2) {
            ((CharacteristicOperationFragment) fragments.get(2)).showData();
        }
    }

    //准备Fragment
    //依次创建3个不同用途的Fragment类型的对象，然后添加到List<Fragment>中
    //向getSupportFragmentManager().beginTransaction()依次添加List中的3个Fragment，隐藏，然后commit
    private void prepareFragment() {
        fragments.add(new ServiceListFragment());
        fragments.add(new CharacteristicListFragment());
        fragments.add(new CharacteristicOperationFragment());
        for (Fragment fragment : fragments) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, fragment).hide(fragment).commit();
        }
    }

    //根据入口传入的page号来显示期望的Fragment
    private void updateFragment(int position) {
        //若入口传入的page号超出了已注册的Fragment的个数，则返回
        if (position > fragments.size() - 1) {
            return;
        }
        //历遍List，实现：期望显示的就show，其余的就hide
        for (int i = 0; i < fragments.size(); i++) {
            //注意：FragmentTransaction类型对象需要在用的时候当场获取！！！
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            //用fragment指向List中的一个
            Fragment fragment = fragments.get(i);
            //若List中的这个Fragment是要显示的，就show
            if (i == position) {
                transaction.show(fragment);
            }
            //若List中的这个Fragment是不显示的，就hide
            else {
                transaction.hide(fragment);
            }
            //提交本次事务
            transaction.commit();
        }
    }

    //在其它Fragment中调用此方法，可以获得用户当时点选了哪个具体的BleDevice
    //而此BleDevice是MainActivity通过Intent传给OperationActivity的
    public BleDevice getBleDevice() {
        return bleDevice;
    }

    //在其它Fragment中调用此方法，可以获得用户当时点选了哪个具体的Service
    public BluetoothGattService getBluetoothGattService() {
        return bluetoothGattService;
    }

    //用户在ServiceListFragment中点选了具体的Service，后通过此方法保存好那个Service
    public void setBluetoothGattService(BluetoothGattService bluetoothGattService) {
        this.bluetoothGattService = bluetoothGattService;
    }

    //在其它Fragment中调用此方法，可以获得用户当时点选了哪个具体的Characteristic
    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    //用户在CharacteristicListFragment中点选了具体的Characteristic，后通过此方法保存好那个Characteristic
    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    //用户在CharacteristicListFragment中点选Characteristic后弹出对话框中 选择的具体操作
    //注意：该操作的值与此Characteristic的属性相同
    public int getCharaProp() {
        return charaProp;
    }

    //用户在CharacteristicListFragment中点选Characteristic后弹出对话框中 选择的具体操作
    //注意：该操作的值与此Characteristic的属性相同
    public void setCharaProp(int charaProp) {
        this.charaProp = charaProp;
    }


}
