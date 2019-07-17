package com.clj.blesample.operation;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.clj.blesample.R;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CharacteristicListFragment extends Fragment {

    private ResultAdapter mResultAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //为本Fragment引入布局
        View v = inflater.inflate(R.layout.fragment_characteric_list, null);
        //初始化本Fragment（本质上Fragment就是个View）
        initView(v);
        //返回本Fragment（本质上Fragment就是个View）
        return v;
    }

    //初始化本Fragment（本质上Fragment就是个View）
    //参数=引入的布局
    private void initView(View v) {
        //创建适配器类对象，并用mResultAdapter来指向它
        //通过getActivity获得当前Fragment相关联的Activity的实例
        mResultAdapter = new ResultAdapter(getActivity());
        //用ListView来列出在已连接设备中用户指定服务中扫描到的所有特征，并为他们设置好适配器
        ListView listView_device = (ListView) v.findViewById(R.id.list_service);
        listView_device.setAdapter(mResultAdapter);
        //为适配器对象设置监听
        listView_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //当按下item
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothGattCharacteristic characteristic = mResultAdapter.getItem(position);
                //创建一个List对象用于弹出对话框
                final List<Integer> propList = new ArrayList<>();
                List<String> propNameList = new ArrayList<>();
                //获得此特征值的属性
                int charaProp = characteristic.getProperties();
                //若属性中包含有READ，则添加
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    propList.add(CharacteristicOperationFragment.PROPERTY_READ);
                    propNameList.add("Read");
                }
                //若属性中包含有WRITE，则添加
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    propList.add(CharacteristicOperationFragment.PROPERTY_WRITE);
                    propNameList.add("Write");
                }
                //若属性中包含有WRITE_NO_RESPONSE，则添加
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    propList.add(CharacteristicOperationFragment.PROPERTY_WRITE_NO_RESPONSE);
                    propNameList.add("Write No Response");
                }
                //若属性中包含有NOTIFY，则添加
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    propList.add(CharacteristicOperationFragment.PROPERTY_NOTIFY);
                    propNameList.add("Notify");
                }
                //若属性中包含有INDICATE，则添加
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    propList.add(CharacteristicOperationFragment.PROPERTY_INDICATE);
                    propNameList.add("Indicate");
                }

                //此特征值的属性的种类>1，即2种以上
                if (propList.size() > 1) {
                    //构建对话框
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getActivity().getString(R.string.select_operation_type))
                            .setItems(propNameList.toArray(new String[propNameList.size()]), new DialogInterface.OnClickListener() {
                                //当点选
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //指明后续将对哪个特征值进行操作
                                    ((OperationActivity) getActivity()).setCharacteristic(characteristic);
                                    //根据用户的点选，设置charaProp
                                    ((OperationActivity) getActivity()).setCharaProp(propList.get(which));
                                    //切换进入新的page，即此特征的详情
                                    ((OperationActivity) getActivity()).changePage(2);
                                }
                            })
                            //显示对话框
                            .show();
                }
                //此特征值的属性的种类>0，即只有1种
                else if (propList.size() > 0) {
                    //指明后续将对哪个特征值进行操作
                    ((OperationActivity) getActivity()).setCharacteristic(characteristic);
                    //设置charaProp
                    ((OperationActivity) getActivity()).setCharaProp(propList.get(0));
                    //切换进入新的page，即此特征的详情
                    ((OperationActivity) getActivity()).changePage(2);
                }
            }
        });
    }

    //呈现数据
    public void showData() {
        //通过getActivity获得当前Fragment相关联的Activity的实例
        //获得BluetoothGattService类的对象，用service指向它
        BluetoothGattService service = ((OperationActivity) getActivity()).getBluetoothGattService();
        //清空数据源
        mResultAdapter.clear();
        //将已连接设备中用户指定服务中扫描到的所有特征，依次添加到数据源中
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            mResultAdapter.addResult(characteristic);
        }
        //通知Android去调用Adapter的getView()方法来刷新ListView
        mResultAdapter.notifyDataSetChanged();
    }

    //为了将数据能呈现在ListView上，而使用的Adapter
    private class ResultAdapter extends BaseAdapter {
        //暂存创建本类对象时传入的上下文
        private Context context;
        //数据源（BluetoothGattCharacteristic类型的List）
        private List<BluetoothGattCharacteristic> characteristicList;

        //构造函数
        //1.将创建本类对象时传入的上下文保存
        //2.为数据源分配空间
        ResultAdapter(Context context) {
            this.context = context;
            characteristicList = new ArrayList<>();
        }

        //将入口传入的数据添加到数据源
        void addResult(BluetoothGattCharacteristic characteristic) {
            characteristicList.add(characteristic);
        }

        //清空数据源
        void clear() {
            characteristicList.clear();
        }

        //返回数据源中item的总个数
        @Override
        public int getCount() {
            return characteristicList.size();
        }

        //根据入口传入的编号，获得数据源中对应的item
        @Override
        public BluetoothGattCharacteristic getItem(int position) {
            if (position > characteristicList.size())
                return null;
            return characteristicList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        //每绘制一个Item就会调用一次getView方法，
        //在getView中引用事先定义好的layout布局确定显示的效果并返回一个View对象作为一个Item显示出来。
        //参数1=本次需要绘制的Item在数据源中的位置
        //参数2=本次需要绘制的View对象。若它为null则需要构建，若非null则可重用
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
            //若当前“显示一个item用的View”为null，即不存在，那么就需要构建
            else {
                //引入指定的布局
                convertView = View.inflate(context, R.layout.adapter_service, null);
                //创建一个ViewHolder类型的对象，即“显示一个item用的View”
                holder = new ViewHolder();
                //将布局中的各个控件与这个“显示一个item用的View”相关联
                holder.txt_title = (TextView) convertView.findViewById(R.id.txt_title);
                holder.txt_uuid = (TextView) convertView.findViewById(R.id.txt_uuid);
                holder.txt_type = (TextView) convertView.findViewById(R.id.txt_type);
                holder.img_next = (ImageView) convertView.findViewById(R.id.img_next);
                //将此新建的“显示一个item用的View”暂存
                convertView.setTag(holder);
            }

            //在“显示一个item用的View”上显示指定位置的item的相关数据内容
            //本页面显示内容：
            //  Toolbar: 特征列表
            //  特性(0)
            //  UUID
            //  特性(Read, Write, Notify)
            //获得数据源中指定位置的item，即一个特征值
            BluetoothGattCharacteristic characteristic = characteristicList.get(position);
            //获得此特征值的UUID
            String uuid = characteristic.getUuid().toString();

            //通过getActivity获得当前Fragment相关联的Activity的实例
            holder.txt_title.setText(String.valueOf(getActivity().getString(R.string.characteristic) + "（" + position + ")"));
            holder.txt_uuid.setText(uuid);

            //创建一个StringBuilder，后续用于显示
            StringBuilder property = new StringBuilder();
            //获得此特征值的属性
            int charaProp = characteristic.getProperties();
            //若属性中包含有READ，则添加“Read, ”
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                property.append("Read");
                property.append(" , ");
            }
            //若属性中包含有WRITE，则添加“Write, ”
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                property.append("Write");
                property.append(" , ");
            }
            //若属性中包含有WRITE_NO_RESPONSE，则添加“Write No Response, ”
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                property.append("Write No Response");
                property.append(" , ");
            }
            //若属性中包含有NOTIFY，则添加“Notify, ”
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                property.append("Notify");
                property.append(" , ");
            }
            //若属性中包含有INDICATE，则添加“Indicate, ”
            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                property.append("Indicate");
                property.append(" , ");
            }
            //若StringBuilder的长度>1，则删除末尾的", "
            if (property.length() > 1) {
                property.delete(property.length() - 2, property.length() - 1);
            }
            //
            if (property.length() > 0) {
                //通过getActivity获得当前Fragment相关联的Activity的实例
                holder.txt_type.setText(String.valueOf(getActivity().getString(R.string.characteristic) + "( " + property.toString() + ")"));
                holder.img_next.setVisibility(View.VISIBLE);
            } else {
                holder.img_next.setVisibility(View.INVISIBLE);
            }

            //返回更新好的“显示一个item用的View”，用于刷新显示
            return convertView;
        }

        //定义“显示一个item用的View”内包含的控件
        class ViewHolder {
            TextView txt_title;
            TextView txt_uuid;
            TextView txt_type;
            ImageView img_next;
        }
    }
}
