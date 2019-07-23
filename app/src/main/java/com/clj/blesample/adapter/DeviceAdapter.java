package com.clj.blesample.adapter;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.blesample.RssiUtils;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

//Adapter: 因为 ListView 只是一个 View,不能添加子项，因此在呈现数据的时候就需要某种工具将数据呈现在
//  ListView 上，而 Adapter 就能充当此角色。常用的 Adapter：ArrayAdapter、BaseAdapter等。
//
//Adapter的定义
//  继承 BaseAdapter (可在继承的时候指定泛型，扩展使用);
//  重写Override四个基本方法：
//      getCount()：获取List数据源的总的数量，返回 int 类型的结果；
//      getItem(int position) ：获取List数据源中指定位置的item，返回该item数据；
//      getItemId(int position)：获取List数据源中指定位置数据的id，返回该数据的id，一般以数据所在的位置作为它的id；
//      getView(int position,View convertView,ViewGroup parent)：关键方法，用于确定列表项
//  创建 ViewHolder （包含列表项的控件。）
//
//ViewHolder的理解
//  要想使用 ListView 就需要编写一个 Adapter 将List数据源中的数据适配到 ListView上，而为了节省资源提高运行效率，一般
//  自定义类 ViewHolder 来减少 findViewById() 的使用以及避免过多地 inflate view，从而实现目的。


//BaseAdapter可以完成自己定义的Adapter，可以将任何复杂组合的数据和资源，以任何你想要的显示效果展示给大家。
//继承BaseAdapter之后，需要重写以下四个方法：getCount，getItem，getItemId，getView。
//
//系统在绘制ListView之前，将会先调用getCount方法来获取Item的个数。每绘制一个Item就会调用一次getView方法，
//在getView中引用事先定义好的layout布局确定显示的效果并返回一个View对象作为一个Item显示出来。
//这两个方法是自定ListView显示效果中最为重要的，同时只要重写好了这两个方法，ListView就能完全按开发者的要求显示。
//而getItem和getItemId方法将会在调用ListView的响应方法的时候被调用到。

//若修改ListView的显示内容：
//  1.修改布局文件
//  2.修改ViewHolder中的相应控件
//  3.修改holder与各个控件的关联
//  4.修改将数据赋值给item中相应的控件
//  5.修改为此item上的各类“点击事件”设置回调

public class DeviceAdapter extends BaseAdapter {
    //暂存创建本类对象时传入的上下文
    private Context context;
    //List数据源（BleDevice类型的List）
    private List<BleDevice> bleDeviceList;

    //定义“显示一个item用的View”内包含的控件
    //若修改ListView的显示内容：2.修改此处的ViewHolder中的相应控件
    class ViewHolder {
        View view_item;
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_bond;
        TextView txt_rssi;
        TextView txt_range;
        TextView txt_timestamp;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_disconnect;
        Button btn_connect;
        Button btn_detail;
    }

    //构造函数
    //1.将创建本类对象时传入的上下文保存
    //2.为List数据源分配空间
    public DeviceAdapter(Context context) {
        this.context = context;
        bleDeviceList = new ArrayList<>();
    }

    //将入口传入的一个数据item添加到List数据源中
    public void addDevice(BleDevice bleDevice) {
        //若List数据源中已有入口传入的设备，就先删除
        removeDevice(bleDevice);
        //向List数据源中添加入口传入的设备
        bleDeviceList.add(bleDevice);
    }

    //将入口指定的一个数据item从List数据源中移除
    public void removeDevice(BleDevice bleDevice) {
        //历遍List数据源
        for (int i = 0; i < bleDeviceList.size(); i++) {
            //从List数据源中获取指定的一个数据item
            BleDevice device = bleDeviceList.get(i);
            //判断这个数据item是否与入口传入的数据相同，若相同则从List数据源中移除
            //判断依据是：这个BLE设备的“广播名称+MAC地址”
            if (bleDevice.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }

    //将已连接的BLE设备从List数据源中移除
    public void clearConnectedDevice() {
        //历遍List数据源
        for (int i = 0; i < bleDeviceList.size(); i++) {
            //从List数据源中获取指定的一个数据item
            BleDevice device = bleDeviceList.get(i);
            //判断该BLE设备是否已连接，若已连接，则从List数据源中移除
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    //将未连接的BLE设备从List数据源中移除
    public void clearScanDevice() {
        //历遍List数据源
        for (int i = 0; i < bleDeviceList.size(); i++) {
            //从List数据源中获取指定的一个数据item
            BleDevice device = bleDeviceList.get(i);
            //判断该BLE设备是否未连接，若未连接，则从List数据源中移除
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    //清空List数据源
    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }



    //返回List数据源中item的总个数
    @Override
    public int getCount() {
        return bleDeviceList.size();
    }

    //根据入口传入的编号，获得List数据源中对应的item
    @Override
    public BleDevice getItem(int position) {
        //若入口传入的编号>List数据源中数据总个数，则返回null
        if (position > bleDeviceList.size())
            return null;
        //返回入口传入编号对应的List数据源中的item
        return bleDeviceList.get(position);
    }

    //根据入口传入的编号，获得List数据源中对应的item的ID
    //本程序未用item的ID，所以返回0
    @Override
    public long getItemId(int position) {
        return 0;
    }

    //每绘制一个Item就会调用一次getView方法，即通过此方法得到绘制当前Item的具体内容。
    //1.若convertView不存在则构建，若已存在则从暂存中取出后复用
    //2.获取当前Item待绘制的List数据源中的相应位置的数据（数据以对象方式存储）
    //3.将数据赋值给item中相应的控件
    //4.为此item上的各类“点击事件”设置回调
    //5.返回更新好的“显示一个item用的View”，用于刷新显示
    //
    //在getView中引用事先定义好的layout布局确定显示的效果并返回一个View对象作为一个Item显示出来。
    //参数1=本次需要绘制的Item在List数据源中的位置
    //参数2=本次需要绘制的“显示一个item用的View”对象。若它为null则需要构建，若非null则可重用
    //参数3=？？？没用到
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //获得List数据源中指定位置的item，即一个BLE设备
        final BleDevice bleDevice = getItem(position);
        //声明一个“显示一个item用的View”
        ViewHolder holder;

        //1.若convertView不存在则构建，若已存在则从暂存中取出后复用
        //若当前“显示一个item用的View”非null，即已存在
        if (convertView != null) {
            //从暂存中取出“显示一个item用的View”，这样就不需要再次构建了
            holder = (ViewHolder) convertView.getTag();
        }
        //若当前“显示一个item用的View”为null，即不存在，那么就需要构建View
        else {
            //引入指定的布局
            //若修改ListView的显示内容：1.需要修改此处的布局文件
            convertView = View.inflate(context, R.layout.adapter_device, null);
            //创建一个ViewHolder类型的对象，即“显示一个item用的View”
            holder = new ViewHolder();
            //将布局中的控件与这个“显示一个item用的View”相关联
            //若修改ListView的显示内容：3.需要修改此处的holder与各个控件的关联
            holder.view_item = (View) convertView; 
            holder.img_blue = (ImageView) convertView.findViewById(R.id.img_blue);
            holder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
            holder.txt_mac = (TextView) convertView.findViewById(R.id.txt_mac);
            holder.txt_bond = (TextView) convertView.findViewById(R.id.txt_bond);
            holder.txt_rssi = (TextView) convertView.findViewById(R.id.txt_rssi);
            holder.txt_range = (TextView) convertView.findViewById(R.id.txt_range);
            holder.txt_timestamp = (TextView) convertView.findViewById(R.id.txt_timestamp);
            holder.layout_idle = (LinearLayout) convertView.findViewById(R.id.layout_idle);
            holder.layout_connected = (LinearLayout) convertView.findViewById(R.id.layout_connected);
            holder.btn_disconnect = (Button) convertView.findViewById(R.id.btn_disconnect);
            holder.btn_connect = (Button) convertView.findViewById(R.id.btn_connect);
            holder.btn_detail = (Button) convertView.findViewById(R.id.btn_detail);

            //将此新建的“显示一个item用的View”暂存
            convertView.setTag(holder);
        }

        //2.获取当前Item待绘制的List数据源中的相应位置的数据（数据以对象方式存储）
        //若此设备非null
        if (bleDevice != null) {
            //获取此设备的连接状态
            boolean isConnected = BleManager.getInstance().isConnected(bleDevice);
            //获取此设备的名称
            String name = bleDevice.getName();
            if(name==null)  name = "N/A";
            //获取此设备的MAC地址
            String mac = bleDevice.getMac();
            //获取此设备的绑定状态
            int bond = bleDevice.getBond();
            String str_bond = null;
            if(bond == 10){
                str_bond = "未绑定";
            }
            else if(bond == 11){
                str_bond = "绑定中";
            }
            else if(bond == 12){
                str_bond = "已绑定";
            }
            //获取此设备的RSSI值
            int rssi = bleDevice.getRssi();
            //获取此设备的两次被扫描到的时间间隔
            long time = bleDevice.getTime();

            //3.将数据赋值给item中相应的控件
            //若修改ListView的显示内容：4.需要修改此处的将数据赋值给item中相应的控件
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
            holder.txt_bond.setText(str_bond);
            holder.txt_rssi.setText(String.valueOf(rssi));
            double range = RssiUtils.getLeDistance(rssi);
            holder.txt_range.setText(String.format("%.2f", range));
            holder.txt_timestamp.setText(String.valueOf(time));
            //若为连接，则显示连接的图示
            if (isConnected) {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.txt_name.setTextColor(0xFF1DE9B6);
                holder.txt_mac.setTextColor(0xFF1DE9B6);
                holder.txt_bond.setTextColor(0xFF1DE9B6);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
            }
            //若为断开，则显示断开的图示
            else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.txt_bond.setTextColor(0xFF000000);
                //若已绑定，则文字颜色改为绿色
                if(holder.txt_bond.getText()=="已绑定")
                    holder.txt_bond.setTextColor(0xFF1DE9B6);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
            }
        }

        //4.为此item上的各类“点击事件”设置回调
        //响应“显示一个item用的View”内的点击“整个item”的事件，即回调用户预置的相关处理函数
        //若修改ListView的显示内容：5.需要修改此处的为此item上的各类“点击事件”设置回调
        holder.view_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //若已设置过回调，则进行回调
                if (mListener != null) {
                    mListener.onAdvData(bleDevice);
                }                
            }
        });
                
        //响应“显示一个item用的View”内的点击“连接键”的事件，即回调用户预置的相关处理函数
        holder.btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //若已设置过回调，则进行回调
                if (mListener != null) {
                    mListener.onConnect(bleDevice);
                }
            }
        });

        //响应“显示一个item用的View”内的点击“断开键”的事件，即回调用户预置的相关处理函数
        holder.btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //若已设置过回调，则进行回调
                if (mListener != null) {
                    mListener.onDisConnect(bleDevice);
                }
            }
        });

        //响应“显示一个item用的View”内的点击“进入键”的事件，即回调用户预置的相关处理函数
        holder.btn_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //若已设置过回调，则进行回调
                if (mListener != null) {
                    mListener.onDetail(bleDevice);
                }
            }
        });

        //5.返回更新好的“显示一个item用的View”，用于刷新显示
        return convertView;
    }


    //为此item上的各类“点击事件”设置回调
    //
    //回调用的接口
    public interface OnDeviceClickListener {
        //当点击表格中“显示一个item用的View”内的“整个item”后进行回调
        void onAdvData(BleDevice bleDevice);
        //当点击表格中“显示一个item用的View”内的“连接”键后进行回调
        void onConnect(BleDevice bleDevice);
        //当点击表格中“显示一个item用的View”内的“断开”键后进行回调
        void onDisConnect(BleDevice bleDevice);
        //当点击表格中“显示一个item用的View”内的“进入”键后进行回调
        void onDetail(BleDevice bleDevice);
    }

    //预埋在本程序内部的钩子
    private OnDeviceClickListener mListener;

    //在本程序外部调用此方法来设置回调，即挂钩
    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
