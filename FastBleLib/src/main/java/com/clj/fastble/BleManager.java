package com.clj.fastble;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;

import com.clj.fastble.bluetooth.BleBluetooth;
import com.clj.fastble.bluetooth.MultipleBluetoothController;
import com.clj.fastble.bluetooth.SplitWriter;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.exception.OtherException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.scan.BleScanner;
import com.clj.fastble.utils.BleLog;

import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {

    private Application context;
    private BleScanRuleConfig bleScanRuleConfig;
    private BluetoothAdapter bluetoothAdapter;
    private MultipleBluetoothController multipleBluetoothController;
    private BluetoothManager bluetoothManager;

    public static final int DEFAULT_SCAN_TIME = 10000;
    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;
    private static final int DEFAULT_CONNECT_RETRY_COUNT = 0;
    private static final int DEFAULT_CONNECT_RETRY_INTERVAL = 5000;
    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;
    private static final int DEFAULT_WRITE_DATA_SPLIT_COUNT = 20;
    private static final int DEFAULT_CONNECT_OVER_TIME = 10000;

    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;
    private int operateTimeout = DEFAULT_OPERATE_TIME;
    private int reConnectCount = DEFAULT_CONNECT_RETRY_COUNT;
    private long reConnectInterval = DEFAULT_CONNECT_RETRY_INTERVAL;
    private int splitWriteNum = DEFAULT_WRITE_DATA_SPLIT_COUNT;
    private long connectOverTime = DEFAULT_CONNECT_OVER_TIME;

    //类方法static(即类加载后，无需创建对象，就可以被调用的方法)
    public static BleManager getInstance() {
        return BleManagerHolder.sBleManager;
    }

    //私有的静态内部类（不需要实例化，就能用）
    private static class BleManagerHolder {
        //类常量static final(即类的多个对象的共享常量)
        //无论调用多少次，都只会是第一次new出的BleManager实例
        private static final BleManager sBleManager = new BleManager();
    }

    //初始化
    //入口传入调用本函数的实例对象，然后暂存到context
    public void init(Application app) {
        //
        if (context == null && app != null) {
            //保存入口传入的上下文
            context = app;
            //判断当前Android设备是否支持BLE
            if (isSupportBle()) {
                //若支持BLE，则从当前设备的系统服务链表中获得BluetoothManager实例
                bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            }
            //获得当前设备的BluetoothAdapter实例，只有它能实现扫描，连接等具体功能
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            //创建MultipleBluetoothController类对象，用于对多个连接的Ble设备进行管理（1主对多从）
            //因此可以称之为“多个从机BLE控制器”
            multipleBluetoothController = new MultipleBluetoothController();
            //创建扫描规则类对象，后续用于存放具体的扫描规则
            bleScanRuleConfig = new BleScanRuleConfig();
        }
    }

    /**
     * Get the Context
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * Get the BluetoothManager
     *
     * @return
     */
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    /**
     * Get the BluetoothAdapter
     *
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * get the ScanRuleConfig
     *
     * @return
     */
    public BleScanRuleConfig getScanRuleConfig() {
        return bleScanRuleConfig;
    }

    /**
     * Get the multiple Bluetooth Controller
     *
     * @return
     */
    public MultipleBluetoothController getMultipleBluetoothController() {
        return multipleBluetoothController;
    }

    /**
     * Configure scan and connection properties
     *
     * @param config
     */
    public void initScanRule(BleScanRuleConfig config) {
        this.bleScanRuleConfig = config;
    }

    /**
     * Get the maximum number of connections
     *
     * @return
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     *
     * @param count
     * @return BleManager
     */
    public BleManager setMaxConnectCount(int count) {
        if (count > DEFAULT_MAX_MULTIPLE_DEVICE)
            count = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = count;
        return this;
    }

    /**
     * Get operate timeout
     *
     * @return
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     *
     * @param count
     * @return BleManager
     */
    public BleManager setOperateTimeout(int count) {
        this.operateTimeout = count;
        return this;
    }

    /**
     * Get connect retry count
     *
     * @return
     */
    public int getReConnectCount() {
        return reConnectCount;
    }

    /**
     * Get connect retry interval
     *
     * @return
     */
    public long getReConnectInterval() {
        return reConnectInterval;
    }

    /**
     * Set connect retry count and interval
     *
     * @param count
     * @return BleManager
     */
    public BleManager setReConnectCount(int count) {
        return setReConnectCount(count, DEFAULT_CONNECT_RETRY_INTERVAL);
    }

    /**
     * Set connect retry count and interval
     *
     * @param count
     * @return BleManager
     */
    public BleManager setReConnectCount(int count, long interval) {
        if (count > 10)
            count = 10;
        if (interval < 0)
            interval = 0;
        this.reConnectCount = count;
        this.reConnectInterval = interval;
        return this;
    }


    /**
     * Get operate split Write Num
     *
     * @return
     */
    public int getSplitWriteNum() {
        return splitWriteNum;
    }

    /**
     * Set split Writ eNum
     *
     * @param num
     * @return BleManager
     */
    public BleManager setSplitWriteNum(int num) {
        if (num > 0) {
            this.splitWriteNum = num;
        }
        return this;
    }

    /**
     * Get operate connect Over Time
     *
     * @return
     */
    public long getConnectOverTime() {
        return connectOverTime;
    }

    /**
     * Set connect Over Time
     *
     * @param time
     * @return BleManager
     */
    public BleManager setConnectOverTime(long time) {
        if (time <= 0) {
            time = 100;
        }
        this.connectOverTime = time;
        return this;
    }

    /**
     * print log?
     *
     * @param enable
     * @return BleManager
     */
    public BleManager enableLog(boolean enable) {
        BleLog.isPrint = enable;
        return this;
    }

    /**
     * scan device around
     *
     * @param callback
     */
    public void scan(BleScanCallback callback) {
        //若没有传入回调，则抛出异常
        if (callback == null) {
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        }

        //若蓝牙没有使能，则log输出，并回调报错
        if (!isBlueEnable()) {
            BleLog.e("Bluetooth not enable!");
            callback.onScanStarted(false);
            return;
        }

        //获取用户预置的扫描规则
        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getScanTimeOut();

        //启动扫描
        //参数1 --- 用户预置的服务UUID
        //参数2 --- 用户预置的设备名称
        //参数3 --- 用户预置的MAC地址
        //参数4 --- 用户预置的Fuzzy
        //参数5 --- 用户预置的扫描时间
        //参数6 --- 一个BleScanCallback类的实例（即回调函数）
        BleScanner.getInstance().scan(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * scan device then connect
     *
     * @param callback
     */
    public void scanAndConnect(BleScanAndConnectCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanAndConnectCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            BleLog.e("Bluetooth not enable!");
            callback.onScanStarted(false);
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getScanTimeOut();

        BleScanner.getInstance().scanAndConnect(serviceUuids, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * connect a known device
     *
     * @param bleDevice
     * @param bleGattCallback
     * @return
     */
    public BluetoothGatt connect(BleDevice bleDevice, BleGattCallback bleGattCallback) {
        if (bleGattCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            BleLog.e("Bluetooth not enable!");
            bleGattCallback.onConnectFail(bleDevice, new OtherException("Bluetooth not enable!"));
            return null;
        }

        if (Looper.myLooper() == null || Looper.myLooper() != Looper.getMainLooper()) {
            BleLog.w("Be careful: currentThread is not MainThread!");
        }

        if (bleDevice == null || bleDevice.getDevice() == null) {
            bleGattCallback.onConnectFail(bleDevice, new OtherException("Not Found Device Exception Occurred!"));
        } else {
            BleBluetooth bleBluetooth = multipleBluetoothController.buildConnectingBle(bleDevice);
            boolean autoConnect = bleScanRuleConfig.isAutoConnect();
            return bleBluetooth.connect(bleDevice, autoConnect, bleGattCallback);
        }

        return null;
    }

    /**
     * connect a device through its mac without scan,whether or not it has been connected
     *
     * @param mac
     * @param bleGattCallback
     * @return
     */
    public BluetoothGatt connect(String mac, BleGattCallback bleGattCallback) {
        BluetoothDevice bluetoothDevice = getBluetoothAdapter().getRemoteDevice(mac);
        BleDevice bleDevice = new BleDevice(bluetoothDevice, 0, null, 0);
        return connect(bleDevice, bleGattCallback);
    }


    /**
     * Cancel scan
     */
    public void cancelScan() {
        BleScanner.getInstance().stopLeScan();
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     */
    public void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       BleNotifyCallback callback) {
        notify(bleDevice, uuid_service, uuid_notify, false, callback);
    }

    /**
     * notify
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param useCharacteristicDescriptor
     * @param callback
     */
    public void notify(BleDevice bleDevice,
                       String uuid_service,
                       String uuid_notify,
                       boolean useCharacteristicDescriptor,
                       BleNotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onNotifyFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_notify)
                    .enableCharacteristicNotify(callback, uuid_notify, useCharacteristicDescriptor);
        }
    }

    /**
     * indicate
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @param callback
     */
    public void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         BleIndicateCallback callback) {
        indicate(bleDevice, uuid_service, uuid_indicate, false, callback);
    }

    /**
     * indicate
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @param useCharacteristicDescriptor
     * @param callback
     */
    public void indicate(BleDevice bleDevice,
                         String uuid_service,
                         String uuid_indicate,
                         boolean useCharacteristicDescriptor,
                         BleIndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleIndicateCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onIndicateFailure(new OtherException("This device not connect!"));
        } else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_indicate)
                    .enableCharacteristicIndicate(callback, uuid_indicate, useCharacteristicDescriptor);
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @return
     */
    public boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify) {
        return stopNotify(bleDevice, uuid_service, uuid_notify, false);
    }

    /**
     * stop notify, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_notify
     * @param useCharacteristicDescriptor
     * @return
     */
    public boolean stopNotify(BleDevice bleDevice,
                              String uuid_service,
                              String uuid_notify,
                              boolean useCharacteristicDescriptor) {
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            return false;
        }
        boolean success = bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_notify)
                .disableCharacteristicNotify(useCharacteristicDescriptor);
        if (success) {
            bleBluetooth.removeNotifyCallback(uuid_notify);
        }
        return success;
    }

    /**
     * stop indicate, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @return
     */
    public boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate) {
        return stopIndicate(bleDevice, uuid_service, uuid_indicate, false);
    }

    /**
     * stop indicate, remove callback
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_indicate
     * @param useCharacteristicDescriptor
     * @return
     */
    public boolean stopIndicate(BleDevice bleDevice,
                                String uuid_service,
                                String uuid_indicate,
                                boolean useCharacteristicDescriptor) {
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            return false;
        }
        boolean success = bleBluetooth.newBleConnector()
                .withUUIDString(uuid_service, uuid_indicate)
                .disableCharacteristicIndicate(useCharacteristicDescriptor);
        if (success) {
            bleBluetooth.removeIndicateCallback(uuid_indicate);
        }
        return success;
    }

    /**
     * write
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_write
     * @param data
     * @param callback
     */
    public void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      BleWriteCallback callback) {
        write(bleDevice, uuid_service, uuid_write, data, true, callback);
    }

    /**
     * write
     *
     * @param bleDevice
     * @param uuid_service
     * @param uuid_write
     * @param data
     * @param split
     * @param callback
     */
    public void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      boolean split,
                      BleWriteCallback callback) {

        write(bleDevice, uuid_service, uuid_write, data, split, true, 0, callback);
    }

    /**
     * write
     *
     * @param bleDevice --- 指定的bleDevice
     * @param uuid_service --- 指定的服务的UUID
     * @param uuid_write --- 指定的特征的UUID
     * @param data --- 指定的待写入的字节数组
     * @param split --- TRUE=使能分包发送； FALSE=禁止分包发送
     * @param sendNextWhenLastSuccess
     * @param intervalBetweenTwoPackage --- 分包发送时的间隔
     * @param callback --- 写操作的回调
     */
    public void write(BleDevice bleDevice,
                      String uuid_service,
                      String uuid_write,
                      byte[] data,
                      boolean split,
                      boolean sendNextWhenLastSuccess,
                      long intervalBetweenTwoPackage,
                      BleWriteCallback callback) {

        //若没有预置callback，则抛出异常
        if (callback == null) {
            throw new IllegalArgumentException("BleWriteCallback can not be Null!");
        }
        //若待发送数据为null，则抛出异常
        if (data == null) {
            BleLog.e("data is Null!");
            callback.onWriteFailure(new OtherException("data is Null!"));
            return;
        }
        //若待发送数据大于20字节，且没有使能分包，则Log提示
        if (data.length > 20 && !split) {
            BleLog.w("Be careful: data's length beyond 20! Ensure MTU higher than 23, or use spilt write!");
        }

        //在“多个从机BLE控制器”中选择指定的已连接BLE设备
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        //若该设备不存在，说明未连接到指定BLE设备，就回调报错
        if (bleBluetooth == null) {
            callback.onWriteFailure(new OtherException("This device not connect!"));
        }
        //若设备存在
        else {
            //若使能分包，且待发送数据字节数>设定的每包数据上限值，则进行分包发送
            if (split && data.length > getSplitWriteNum()) {
                new SplitWriter().splitWrite(bleBluetooth, uuid_service, uuid_write, data,
                        sendNextWhenLastSuccess, intervalBetweenTwoPackage, callback);
            } else {
                //创建一个BleConnector对象
                //在已连设备的GATT中找到指定的服务及特征
                //
                bleBluetooth.newBleConnector()
                        .withUUIDString(uuid_service, uuid_write)
                        .writeCharacteristic(data, callback, uuid_write);
            }
        }
    }

    /**
     * read
     *
     * @param bleDevice --- 已连接的BLE设备
     * @param uuid_service --- 指定的服务UUID
     * @param uuid_read --- 指定的特征UUID
     * @param callback --- 读操作的回调
     */
    public void read(BleDevice bleDevice,
                     String uuid_service,
                     String uuid_read,
                     BleReadCallback callback) {
        //若未传入回调，则抛出异常
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }
        //在多路BLE控制器中找到入口指定的BLE设备
        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        //若指定的BLE设备不存在，则回调报错
        if (bleBluetooth == null) {
            callback.onReadFailure(new OtherException("This device is not connected!"));
        }
        //若指定的BLE设备存在，则
        else {
            bleBluetooth.newBleConnector()
                    .withUUIDString(uuid_service, uuid_read)
                    .readCharacteristic(callback, uuid_read);
        }
    }

    /**
     * read Rssi
     *
     * @param bleDevice
     * @param callback
     */
    public void readRssi(BleDevice bleDevice,
                         BleRssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onRssiFailure(new OtherException("This device is not connected!"));
        } else {
            bleBluetooth.newBleConnector().readRemoteRssi(callback);
        }
    }

    /**
     * set Mtu
     *
     * @param bleDevice
     * @param mtu
     * @param callback
     */
    public void setMtu(BleDevice bleDevice,
                       int mtu,
                       BleMtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
        }

        if (mtu > DEFAULT_MAX_MTU) {
            BleLog.e("requiredMtu should lower than 512 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should lower than 512 !"));
            return;
        }

        if (mtu < DEFAULT_MTU) {
            BleLog.e("requiredMtu should higher than 23 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should higher than 23 !"));
            return;
        }

        BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
        if (bleBluetooth == null) {
            callback.onSetMTUFailure(new OtherException("This device is not connected!"));
        } else {
            bleBluetooth.newBleConnector().setMtu(mtu, callback);
        }
    }

    /**
     * requestConnectionPriority
     *
     * @param connectionPriority Request a specific connection priority. Must be one of
     *                           {@link BluetoothGatt#CONNECTION_PRIORITY_BALANCED},
     *                           {@link BluetoothGatt#CONNECTION_PRIORITY_HIGH}
     *                           or {@link BluetoothGatt#CONNECTION_PRIORITY_LOW_POWER}.
     * @throws IllegalArgumentException If the parameters are outside of their
     *                                  specified range.
     */
    public boolean requestConnectionPriority(BleDevice bleDevice, int connectionPriority) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BleBluetooth bleBluetooth = multipleBluetoothController.getBleBluetooth(bleDevice);
            if (bleBluetooth == null) {
                return false;
            } else {
                return bleBluetooth.newBleConnector().requestConnectionPriority(connectionPriority);
            }
        }
        return false;
    }

    /**
     * is support ble?
     *
     * @return
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && context.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.enable();
        }
    }

    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled())
                bluetoothAdapter.disable();
        }
    }

    /**
     * judge Bluetooth is enable
     *
     * @return
     */
    public boolean isBlueEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }


    public BleDevice convertBleDevice(BluetoothDevice bluetoothDevice) {
        return new BleDevice(bluetoothDevice);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BleDevice convertBleDevice(ScanResult scanResult) {
        if (scanResult == null) {
            throw new IllegalArgumentException("scanResult can not be Null!");
        }
        BluetoothDevice bluetoothDevice = scanResult.getDevice();
        int rssi = scanResult.getRssi();
        ScanRecord scanRecord = scanResult.getScanRecord();
        byte[] bytes = null;
        if (scanRecord != null)
            bytes = scanRecord.getBytes();
        long timestampNanos = scanResult.getTimestampNanos();
        return new BleDevice(bluetoothDevice, rssi, bytes, timestampNanos);
    }

    public BleBluetooth getBleBluetooth(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            return multipleBluetoothController.getBleBluetooth(bleDevice);
        }
        return null;
    }

    public BluetoothGatt getBluetoothGatt(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            return bleBluetooth.getBluetoothGatt();
        return null;
    }

    public List<BluetoothGattService> getBluetoothGattServices(BleDevice bleDevice) {
        BluetoothGatt gatt = getBluetoothGatt(bleDevice);
        if (gatt != null) {
            return gatt.getServices();
        }
        return null;
    }

    public List<BluetoothGattCharacteristic> getBluetoothGattCharacteristics(BluetoothGattService service) {
        return service.getCharacteristics();
    }

    public void removeConnectGattCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeConnectGattCallback();
    }

    public void removeRssiCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeRssiCallback();
    }

    public void removeMtuChangedCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeMtuChangedCallback();
    }

    public void removeNotifyCallback(BleDevice bleDevice, String uuid_notify) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeNotifyCallback(uuid_notify);
    }

    public void removeIndicateCallback(BleDevice bleDevice, String uuid_indicate) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeIndicateCallback(uuid_indicate);
    }

    public void removeWriteCallback(BleDevice bleDevice, String uuid_write) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeWriteCallback(uuid_write);
    }

    public void removeReadCallback(BleDevice bleDevice, String uuid_read) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.removeReadCallback(uuid_read);
    }

    //清除指定的bleDevice的4类回调用的HashMap的内容
    //bleNotifyCallbackHashMap
    //bleIndicateCallbackHashMap
    //bleWriteCallbackHashMap
    //bleReadCallbackHashMap
    public void clearCharacterCallback(BleDevice bleDevice) {
        BleBluetooth bleBluetooth = getBleBluetooth(bleDevice);
        if (bleBluetooth != null)
            bleBluetooth.clearCharacterCallback();
    }

    public BleScanState getScanSate() {

        return BleScanner.getInstance().getScanState();
    }

    //获得所有已连接设备的List
    public List<BleDevice> getAllConnectedDevice() {
        if (multipleBluetoothController == null)
            return null;
        return multipleBluetoothController.getDeviceList();
    }

    /**
     * @param bleDevice
     * @return State of the profile connection. One of
     * {@link BluetoothProfile#STATE_CONNECTED},
     * {@link BluetoothProfile#STATE_CONNECTING},
     * {@link BluetoothProfile#STATE_DISCONNECTED},
     * {@link BluetoothProfile#STATE_DISCONNECTING}
     */
    public int getConnectState(BleDevice bleDevice) {
        if (bleDevice != null) {
            return bluetoothManager.getConnectionState(bleDevice.getDevice(), BluetoothProfile.GATT);
        } else {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
    }

    public boolean isConnected(BleDevice bleDevice) {
        return getConnectState(bleDevice) == BluetoothProfile.STATE_CONNECTED;
    }

    public boolean isConnected(String mac) {
        List<BleDevice> list = getAllConnectedDevice();
        for (BleDevice bleDevice : list) {
            if (bleDevice != null) {
                if (bleDevice.getMac().equals(mac)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void disconnect(BleDevice bleDevice) {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.disconnect(bleDevice);
        }
    }

    public void disconnectAllDevice() {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.disconnectAllDevice();
        }
    }

    public void destroy() {
        if (multipleBluetoothController != null) {
            multipleBluetoothController.destroy();
        }
    }


}
