package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleScanPresenterImp;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.utils.BleLog;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleScanner {

    //类方法static(即类加载后，无需创建对象，就可以被调用的方法)
    public static BleScanner getInstance() {
        return BleScannerHolder.sBleScanner;
    }

    //私有的静态内部类（不需要实例化，就能用）
    private static class BleScannerHolder {
        //类常量static final(即类的多个对象的共享常量)
        //无论调用多少次，都只会是第一次new出的BleManager实例
        private static final BleScanner sBleScanner = new BleScanner();
    }

    //成员变量(即每个对象私有的变量)
    //保存BleScanner的当前状态
    private BleScanState mBleScanState = BleScanState.STATE_IDLE;

    private BleScanPresenter mBleScanPresenter = new BleScanPresenter() {

        //当启动扫描时，进行回调
        @Override
        public void onScanStarted(boolean success) {
            //取出在mBleScanPresenter内注册的回调函数，若回调存在，就进行回调
            //回调函数是在执行mBleScanPresenter.prepare()方法时，进行注册的
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanStarted(success);
            }
        }

        //当扫描到一个BLE设备时，进行回调
        @Override
        public void onLeScan(BleDevice bleDevice) {
            //若使能“扫描后自动连接”
            if (mBleScanPresenter.ismNeedConnect()) {
                //取出在mBleScanPresenter内注册的回调函数，若回调存在，就进行回调
                //回调函数是在执行mBleScanPresenter.prepare()方法时，进行注册的
                BleScanAndConnectCallback callback = (BleScanAndConnectCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            }
            //若禁止“扫描后自动连接”
            else {
                //取出在mBleScanPresenter内注册的回调函数，若回调存在，就进行回调
                //回调函数是在执行mBleScanPresenter.prepare()方法时，进行注册的
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onLeScan(bleDevice);
                }
            }
        }

        //当扫描到一个新设备后，进行回调
        @Override
        public void onScanning(BleDevice result) {
            //取出在mBleScanPresenter内注册的回调函数，若回调存在，就进行回调
            //回调函数是在执行mBleScanPresenter.prepare()方法时，进行注册的
            BleScanPresenterImp callback = mBleScanPresenter.getBleScanPresenterImp();
            if (callback != null) {
                callback.onScanning(result);
            }
        }

        //当扫描完成时进行回调
        @Override
        public void onScanFinished(List<BleDevice> bleDeviceList) {
            //若使能“扫描后自动连接”
            if (mBleScanPresenter.ismNeedConnect()) {
                //取出在mBleScanPresenter内注册的回调函数，若回调存在，就进行回调
                //回调函数是在执行mBleScanPresenter.prepare()方法时，进行注册的
                final BleScanAndConnectCallback callback = (BleScanAndConnectCallback)
                        mBleScanPresenter.getBleScanPresenterImp();
                if (bleDeviceList == null || bleDeviceList.size() < 1) {
                    if (callback != null) {
                        callback.onScanFinished(null);
                    }
                } else {
                    if (callback != null) {
                        callback.onScanFinished(bleDeviceList.get(0));
                    }
                    final List<BleDevice> list = bleDeviceList;
//利用Handle的postDelayed方法来实现：当定时100ms到后启动连接
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BleManager.getInstance().connect(list.get(0), callback);
                        }
                    }, 100);
                }
            } else {
                BleScanCallback callback = (BleScanCallback) mBleScanPresenter.getBleScanPresenterImp();
                if (callback != null) {
                    callback.onScanFinished(bleDeviceList);
                }
            }
        }
    };

    //启动扫描（只扫描不连接）
    //参数1 --- 用户预置的服务UUID
    //参数2 --- 用户预置的设备名称
    //参数3 --- 用户预置的MAC地址
    //参数4 --- 用户预置的Fuzzy
    //参数5 --- 用户预置的扫描时间
    //参数6 --- 一个BleScanCallback类的实例（即回调函数）
    public void scan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                     long timeOut, final BleScanCallback callback) {

        //启动扫描
        //参数5 --- 只扫描不连接
        startLeScan(serviceUuids, names, mac, fuzzy, false, timeOut, callback);
    }

    //启动扫描（扫描后连接）
    //参数1 --- 用户预置的服务UUID
    //参数2 --- 用户预置的设备名称
    //参数3 --- 用户预置的MAC地址
    //参数4 --- 用户预置的Fuzzy
    //参数5 --- 用户预置的扫描时间
    //参数6 --- 一个BleScanCallback类的实例（即回调函数）
    public void scanAndConnect(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                               long timeOut, BleScanAndConnectCallback callback) {

        //启动扫描
        //参数5 --- 扫描后连接
        startLeScan(serviceUuids, names, mac, fuzzy, true, timeOut, callback);
    }

    //启动扫描
    //参数1 --- 用户预置的服务UUID
    //参数2 --- 用户预置的设备名称
    //参数3 --- 用户预置的MAC地址
    //参数4 --- 用户预置的Fuzzy
    //参数5 --- 扫描后是否连接
    //参数6 --- 用户预置的扫描时间
    //参数7 --- 一个BleScanCallback类的实例（即回调函数）
    private synchronized void startLeScan(UUID[] serviceUuids, String[] names, String mac, boolean fuzzy,
                                          boolean needConnect, long timeOut, BleScanPresenterImp imp) {
        //若当前不是IDLE状态，则说明已启动扫描，则Log报错，若有传入回调，则回调报错
        if (mBleScanState != BleScanState.STATE_IDLE) {
            BleLog.w("scan action already exists, complete the previous scan action first");
            if (imp != null) {
                imp.onScanStarted(false);
            }
            return;
        }

        //执行到此处说明当前是IDLE状态，则根据入口传入的参数，初始化mBleScanPresenter
        mBleScanPresenter.prepare(names, mac, fuzzy, needConnect, timeOut, imp);

        //启动BLE扫描
        //参数1 --- 用户预置的服务UUID
        //参数2 --- mBleScanPresenter
        boolean success = BleManager.getInstance().getBluetoothAdapter()
                .startLeScan(serviceUuids, mBleScanPresenter);
        mBleScanState = success ? BleScanState.STATE_SCANNING : BleScanState.STATE_IDLE;
        //通知UI线程，扫描已启动
        mBleScanPresenter.notifyScanStarted(success);
    }

    public synchronized void stopLeScan() {
        BleManager.getInstance().getBluetoothAdapter().stopLeScan(mBleScanPresenter);
        mBleScanState = BleScanState.STATE_IDLE;
        mBleScanPresenter.notifyScanStopped();
    }

    public BleScanState getScanState() {
        return mBleScanState;
    }


}
