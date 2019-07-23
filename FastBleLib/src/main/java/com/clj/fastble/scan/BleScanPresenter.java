package com.clj.fastble.scan;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.clj.fastble.callback.BleScanPresenterImp;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleMsg;
import com.clj.fastble.utils.BleLog;
import com.clj.fastble.utils.HexUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class BleScanPresenter implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames;
    private String mDeviceMac;
    private boolean mFuzzy;
    private boolean mNeedConnect;
    private long mScanTimeout;
    private BleScanPresenterImp mBleScanPresenterImp;

    private List<BleDevice> mBleDeviceList = new ArrayList<>();

    //在本线程内创建一个Handler类对象mMainHandler，该Handler是关联到UI线程的Looper的，因此可以通过此Handler返回UI线程
    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    //声明一个HandlerThread类对象，用于
    private HandlerThread mHandlerThread;
    //声明一个Handler类对象mHandler，用于
    private Handler mHandler;
    private boolean mHandling;

    private static final class ScanHandler extends Handler {

        private final WeakReference<BleScanPresenter> mBleScanPresenter;

        ScanHandler(Looper looper, BleScanPresenter bleScanPresenter) {
            super(looper);
            mBleScanPresenter = new WeakReference<>(bleScanPresenter);
        }

        //在mHandlerThread线程内部处理通过mHandler收到的消息
        @Override
        public void handleMessage(Message msg) {
            BleScanPresenter bleScanPresenter = mBleScanPresenter.get();
            if (bleScanPresenter != null) {
                //若是扫描到一个BLE设备后发来的信息
                if (msg.what == BleMsg.MSG_SCAN_DEVICE) {
                    //取出信息中的BLE设备对象，若非空，则返回给UI线程
                    final BleDevice bleDevice = (BleDevice) msg.obj;
                    if (bleDevice != null) {
                        bleScanPresenter.handleResult(bleDevice);
                    }
                }
            }
        }
    }

    private void handleResult(final BleDevice bleDevice) {
        //从当前线程返回UI线程后执行onLeScan
        //将扫描到的BLE设备返回给UI线程
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onLeScan(bleDevice);
            }
        });
        checkDevice(bleDevice);
    }

    public void prepare(String[] names, String mac, boolean fuzzy, boolean needConnect,
                        long timeOut, BleScanPresenterImp bleScanPresenterImp) {
        mDeviceNames = names;
        mDeviceMac = mac;
        mFuzzy = fuzzy;
        mNeedConnect = needConnect;
        mScanTimeout = timeOut;
        mBleScanPresenterImp = bleScanPresenterImp;

        //创建HandlerThread对象（即创建了一个新线程mHandlerThread），入口参数=工作线程的名称，用来做标记用
        mHandlerThread = new HandlerThread(BleScanPresenter.class.getSimpleName());
        //启动线程
        mHandlerThread.start();
        //创建一个与mHandlerThread线程相关联的Handle（这样可以在mHandlerThread线程内部处理通过mHandler收到的消息）
        mHandler = new ScanHandler(mHandlerThread.getLooper(), this);
        mHandling = true;
    }

    public boolean ismNeedConnect() {
        return mNeedConnect;
    }

    public BleScanPresenterImp getBleScanPresenterImp() {
        return mBleScanPresenterImp;
    }

    //若扫描到一个BLE设备
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null)
            return;

        if (!mHandling)
            return;

        //创建与mHandler相关联的Message类对象
        Message message = mHandler.obtainMessage();
        //设定本消息的标志
        message.what = BleMsg.MSG_SCAN_DEVICE;
        //根据入口传入的信息，创建一个BLE设备，并赋给message
        message.obj = new BleDevice(device, rssi, scanRecord, System.currentTimeMillis());
        //发送本消息给mHandler
        mHandler.sendMessage(message);
    }

    private void checkDevice(BleDevice bleDevice) {
        if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
            correctDeviceAndNextStep(bleDevice);
            return;
        }

        if (!TextUtils.isEmpty(mDeviceMac)) {
            if (!mDeviceMac.equalsIgnoreCase(bleDevice.getMac()))
                return;
        }

        if (mDeviceNames != null && mDeviceNames.length > 0) {
            AtomicBoolean equal = new AtomicBoolean(false);
            for (String name : mDeviceNames) {
                String remoteName = bleDevice.getName();
                if (remoteName == null)
                    remoteName = "";
                if (mFuzzy ? remoteName.contains(name) : remoteName.equals(name)) {
                    equal.set(true);
                }
            }
            if (!equal.get()) {
                return;
            }
        }

        correctDeviceAndNextStep(bleDevice);
    }


    private void correctDeviceAndNextStep(final BleDevice bleDevice) {
        if (mNeedConnect) {
            BleLog.i("devices detected  ------"
                    + "  name:" + bleDevice.getName()
                    + "  mac:" + bleDevice.getMac()
                    + "  Rssi:" + bleDevice.getRssi()
                    + "  scanRecord:" + HexUtil.formatHexString(bleDevice.getScanRecord()));

            mBleDeviceList.add(bleDevice);
            //从当前线程返回UI线程后执行stopLeScan
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    BleScanner.getInstance().stopLeScan();
                }
            });

        } else {
            AtomicBoolean hasFound = new AtomicBoolean(false);
            for (BleDevice result : mBleDeviceList) {
                if (result.getDevice().equals(bleDevice.getDevice())) {
                    hasFound.set(true);
                }
            }
            if (!hasFound.get()) {
                BleLog.i("device detected  ------"
                        + "  name: " + bleDevice.getName()
                        + "  mac: " + bleDevice.getMac()
                        + "  Rssi: " + bleDevice.getRssi()
                        + "  scanRecord: " + HexUtil.formatHexString(bleDevice.getScanRecord(), true));

                mBleDeviceList.add(bleDevice);
                //从当前线程返回UI线程后执行onScanning
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onScanning(bleDevice);
                    }
                });
            }
        }
    }

    public final void notifyScanStarted(final boolean success) {
        //先将之前的扫描结果清空
        mBleDeviceList.clear();

        //清除mMainHandler，mHandler这两个队列内的消息
        removeHandlerMsg();

        //若入口传入的success为TRUE(表示启动扫描成功)，且扫描时间>0，则利用Handle的postDelayed方法来实现：当定时到后停止BLE扫描
        if (success && mScanTimeout > 0) {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BleScanner.getInstance().stopLeScan();
                }
            }, mScanTimeout);
        }

        //从当前线程返回UI线程后执行onScanStarted
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onScanStarted(success);
            }
        });
    }

    public final void notifyScanStopped() {
        mHandling = false;
        //结束线程
        mHandlerThread.quit();
        //清除mMainHandler，mHandler这两个队列内的消息
        removeHandlerMsg();
        //从当前线程返回UI线程后执行onScanFinished
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                onScanFinished(mBleDeviceList);
            }
        });
    }

    //清除mMainHandler，mHandler这两个队列内的消息
    public final void removeHandlerMsg() {
        mMainHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onLeScan(BleDevice bleDevice);

    public abstract void onScanning(BleDevice bleDevice);

    public abstract void onScanFinished(List<BleDevice> bleDeviceList);
}
