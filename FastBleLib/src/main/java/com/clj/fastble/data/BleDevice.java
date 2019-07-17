package com.clj.fastble.data;


import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

//为了能在不同的Activity间传送较为复杂的object对象，就需要将对象进行“序列化/反序列化”，也就是需要实现Parcelable接口
//1.定义最简JavaBean
//2.实现Parcelable接口必须的
//      序列化方法：writeToParcel()
//      反序列化方法：Creator<BleDevice> CREATOR = new Creator<BleDevice>()
public class BleDevice implements Parcelable {

    //最简JavaBean
    //1.定义一些属性
    //2.定义读取/写入属性的方法
    private BluetoothDevice mDevice;
    private byte[] mScanRecord;
    private int mRssi;
    private long mTimestampNanos;//保存上次onLeScan后在MainActivity中获得的当前时间戳
    private long mTime;//两次时间戳的差值

    public BluetoothDevice getDevice() {
        return mDevice;
    }
    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }
    public void setScanRecord(byte[] scanRecord) {
        this.mScanRecord = scanRecord;
    }

    public int getRssi() {
        return mRssi;
    }
    public void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    public long getTimestampNanos() {
        return mTimestampNanos;
    }
    public void setTimestampNanos(long timestampNanos) {
        this.mTimestampNanos = timestampNanos;
    }

    public long getTime() {
        return mTime;
    }
    public void setTime(long time) {
        this.mTime = time;
    }

    public String getName() {
        if (mDevice != null)
            return mDevice.getName();
        return null;
    }

    public String getMac() {
        if (mDevice != null)
            return mDevice.getAddress();
        return null;
    }

    public String getKey() {
        if (mDevice != null)
            return mDevice.getName() + mDevice.getAddress();
        return "";
    }

    public int getBond(){
        if (mDevice != null)
            return mDevice.getBondState();
        return -1;

    }

    //构造函数
    public BleDevice(BluetoothDevice device) {
        mDevice = device;
    }

    //构造函数
    public BleDevice(BluetoothDevice device, int rssi, byte[] scanRecord, long timestampNanos) {
        mDevice = device;
        mScanRecord = scanRecord;
        mRssi = rssi;
        mTimestampNanos = timestampNanos;
    }

    //构造函数
    //用于对接收到的Parcelable类对象in进行“反序列化”得到真实的对象
    protected BleDevice(Parcel in) {
        mDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        mScanRecord = in.createByteArray();
        mRssi = in.readInt();
        mTimestampNanos = in.readLong();
    }

    //为实现Parcelable接口，必须override的方法
    //将需要传送的对象写入到Parcel容器中，实现对象的序列化
    //注意：此处写入的顺序必须与createFromParcel()中读出的顺序一致！
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDevice, flags);
        dest.writeByteArray(mScanRecord);
        dest.writeInt(mRssi);
        dest.writeLong(mTimestampNanos);
    }

    //为实现Parcelable接口，必须override的方法
    @Override
    public int describeContents() {
        return 0;
    }

    //为实现Parcelable接口，必须override的方法
    //从Parcel容器中获取序列化的对象，并将其反序列化，得到该对象的实例
    //注意：此处读出的顺序必须与writeToParcel()中写入的顺序一致！
    public static final Creator<BleDevice> CREATOR = new Creator<BleDevice>() {
        @Override
        public BleDevice createFromParcel(Parcel in) {
            return new BleDevice(in);
        }

        @Override
        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };
}
