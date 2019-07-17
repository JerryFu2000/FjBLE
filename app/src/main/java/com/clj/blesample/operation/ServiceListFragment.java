package com.clj.blesample.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

//@TargetApi
//就是在你使用了android Lint检查工具的时候，为了防止代码出现提示性错误而设计的。
//说白了，如果你关闭了android Lint，那么这个对你屁用都没有。
//@Target的作用在于提示：
//  使用高编译版本的代码，为了通用性兼容运行此代码的低版本平台。要求程序员做出区分对待的加载。如用内部类等方式区分加载。
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ServiceListFragment extends Fragment {

    private TextView txt_name, txt_mac;
    private ResultAdapter mResultAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //为本Fragment引入布局
        View v = inflater.inflate(R.layout.fragment_service_list, null);
        //初始化本Fragment（本质上Fragment就是个View）
        initView(v);
        //呈现数据
        showData();
        //返回本Fragment（本质上Fragment就是个View）
        return v;
    }

    //初始化本Fragment（本质上Fragment就是个View）
    //参数=引入的布局
    private void initView(View v) {
        txt_name = (TextView) v.findViewById(R.id.txt_name);
        txt_mac = (TextView) v.findViewById(R.id.txt_mac);
        //创建适配器类对象，并用mResultAdapter来指向它
        //通过getActivity获得当前Fragment相关联的Activity的实例
        mResultAdapter = new ResultAdapter(getActivity());
        //用ListView来列出在已连接设备中扫描到的所有服务，并为他们设置好适配器
        ListView listView_device = (ListView) v.findViewById(R.id.list_service);
        listView_device.setAdapter(mResultAdapter);
        //为适配器对象设置监听
        listView_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //当按下item
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获得用户点选的该位置处的服务
                BluetoothGattService service = mResultAdapter.getItem(position);
                //通过getActivity获得当前Fragment相关联的Activity的实例
                //然后将用户在本Fragment点选的结果，传送给相关联的Activity的实例
                //即在相关联的Activity的实例中保存好用户具体选择了哪个service
                ((OperationActivity) getActivity()).setBluetoothGattService(service);
                //通过getActivity获得当前Fragment相关联的Activity的实例
                //通过调用Activity的方法，切换进入新的page，即此服务的详情
                ((OperationActivity) getActivity()).changePage(1);
            }
        });
    }

    //呈现数据
    private void showData() {
        //通过getActivity获得当前Fragment相关联的Activity的实例
        //在其它Fragment中调用此方法，可以获得用户当时点选了哪个具体的BleDevice
        //而此BleDevice是MainActivity通过Intent传给OperationActivity的
        BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
        String name = bleDevice.getName();
        String mac = bleDevice.getMac();
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);

        //通过getActivity获得当前Fragment相关联的Activity的实例
        txt_name.setText(String.valueOf(getActivity().getString(R.string.name) + name));
        //通过getActivity获得当前Fragment相关联的Activity的实例
        txt_mac.setText(String.valueOf(getActivity().getString(R.string.mac) + mac));

        //清空数据源
        mResultAdapter.clear();
        //将扫描到的已连接设备内部的所有服务，依次添加到数据源中
        for (BluetoothGattService service : gatt.getServices()) {
            mResultAdapter.addResult(service);
        }
        //通知Android去调用Adapter的getView()方法来刷新ListView
        mResultAdapter.notifyDataSetChanged();
    }

    //为了将数据能呈现在ListView上，而使用的Adapter
    private class ResultAdapter extends BaseAdapter {
        //暂存创建本类对象时传入的上下文
        private Context context;
        //数据源（BluetoothGattService类型的List）
        private List<BluetoothGattService> bluetoothGattServices;

        //构造函数
        //1.将创建本类对象时传入的上下文保存
        //2.为数据源分配空间
        ResultAdapter(Context context) {
            this.context = context;
            bluetoothGattServices = new ArrayList<>();
        }

        //将入口传入的数据添加到数据源
        void addResult(BluetoothGattService service) {
            bluetoothGattServices.add(service);
        }

        //清空数据源
        void clear() {
            bluetoothGattServices.clear();
        }

        //返回数据源中item的总个数
        @Override
        public int getCount() {
            return bluetoothGattServices.size();
        }

        //根据入口传入的编号，获得数据源中对应的item
        @Override
        public BluetoothGattService getItem(int position) {
            if (position > bluetoothGattServices.size())
                return null;
            return bluetoothGattServices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        //每绘制一个Item就会调用一次getView方法，
        //在getView中引用事先定义好的layout布局确定显示的效果并返回一个View对象作为一个Item显示出来。
        //参数1=本次需要绘制的Item在数据源中的位置
        //参数2=本次需要绘制的“显示一个item用的View”对象。若它为null则需要构建，若非null则可重用
        //参数3=？？？没用到
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //“显示一个item用的View”
            ViewHolder holder;
            //若当前“显示一个item用的View”非null，即已存在
            if (convertView != null) {
                //从暂存中取出“显示一个item用的View”，这样就不需要再次构建了
                holder = (ViewHolder) convertView.getTag();
            }
            //若当前“显示一个item用的View”为null，即不存在，那么就需要构建View
            else {
                //引入指定的布局
                convertView = View.inflate(context, R.layout.adapter_service, null);
                //创建一个ViewHolder类型的对象，即“显示一个item用的View”
                holder = new ViewHolder();
                //将布局中的控件与这个“显示一个item用的View”相关联
                holder.txt_title = (TextView) convertView.findViewById(R.id.txt_title);
                holder.txt_uuid = (TextView) convertView.findViewById(R.id.txt_uuid);
                holder.txt_type = (TextView) convertView.findViewById(R.id.txt_type);
                //将此新建的“显示一个item用的View”暂存
                convertView.setTag(holder);
            }

            //在“显示一个item用的View”上显示指定位置的item的相关数据内容
            //本页面显示内容：
            //  Toolbar: 服务列表
            //  设备广播名：xxx
            //  MAC：xx:xx:xx:xx:xx:xx
            //  服务(0)
            //  UUID
            //  服务类型(主服务)
            //获得数据源中指定位置的item，即一个服务
            BluetoothGattService service = bluetoothGattServices.get(position);
            String uuid = service.getUuid().toString();

            //通过getActivity获得当前Fragment相关联的Activity的实例
            holder.txt_title.setText(String.valueOf(getActivity().getString(R.string.service) + "(" + position + ")"));
            holder.txt_uuid.setText(uuid);
            //通过getActivity获得当前Fragment相关联的Activity的实例
            holder.txt_type.setText(getActivity().getString(R.string.type));

            //返回更新好的“显示一个item用的View”，用于刷新显示
            return convertView;
        }

        //定义“显示一个item用的View”内包含的控件
        class ViewHolder {
            TextView txt_title;
            TextView txt_uuid;
            TextView txt_type;
        }
    }
}
