package com.clj.blesample.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.clj.blesample.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleIndicateCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CharacteristicOperationFragment extends Fragment {

    public static final int PROPERTY_READ = 1;
    public static final int PROPERTY_WRITE = 2;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 3;
    public static final int PROPERTY_NOTIFY = 4;
    public static final int PROPERTY_INDICATE = 5;

    private LinearLayout layout_container;
    private List<String> childList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
        initView(v);
        return v;
    }

    private void initView(View v) {
        layout_container = (LinearLayout) v.findViewById(R.id.layout_container);
    }

    public void showData() {
        //通过getActivity获得当前Fragment相关联的Activity的实例
        //获取 MainActivity通过Intent传给OperationActivity的“当前已连接的设备”
        final BleDevice bleDevice = ((OperationActivity) getActivity()).getBleDevice();
        //通过getActivity获得当前Fragment相关联的Activity的实例
        //获取 用户在CharacteristicListFragment中点选的待操作的Characteristic
        final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
        //通过getActivity获得当前Fragment相关联的Activity的实例
        //获取 用户在CharacteristicListFragment中点选Characteristic后弹出对话框中 选择的具体操作
        //注意：该操作的值与此Characteristic的属性相同
        final int charaProp = ((OperationActivity) getActivity()).getCharaProp();

        //创建一个字符串=点选的待操作的特征值的UUID+此特征值的属性
        String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

        //依次让布局中的子元素都消失
        for (int i = 0; i < layout_container.getChildCount(); i++) {
            layout_container.getChildAt(i).setVisibility(View.GONE);
        }
        //若List中已包含有“点选的待操作的特征值的UUID+此特征值的属性”
        if (childList.contains(child)) {
            //在布局中引入"Tag=设备名称+MAC地址+特征值UUID+此特征值的属性"的自定义View，并显示
            layout_container.findViewWithTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp).setVisibility(View.VISIBLE);
        }
        //若List中没有“点选的待操作的特征值的UUID+此特征值的属性”
        else {
            //将“点选的待操作的特征值的UUID+此特征值的属性”添加到List中
            childList.add(child);

            //通过getActivity获得当前Fragment相关联的Activity的实例
            //引入自定义的View（垂直型LinearLayout内有2个TextView）
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
            //为此View设定Tag=设备名称+MAC地址+特征值UUID+此特征值的属性
            view.setTag(bleDevice.getKey() + characteristic.getUuid().toString() + charaProp);
            //获得垂直型LinearLayout
            LinearLayout layout_add = (LinearLayout) view.findViewById(R.id.layout_add);
            //
            final TextView txt_title = (TextView) view.findViewById(R.id.txt_title);
            //通过getActivity获得当前Fragment相关联的Activity的实例
            txt_title.setText(String.valueOf(characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
            final TextView txt = (TextView) view.findViewById(R.id.txt);
            txt.setMovementMethod(ScrollingMovementMethod.getInstance());

            //根据之前用户选择的操作进行散转
            switch (charaProp) {
                //若之前选择了“读”操作
                case PROPERTY_READ: {
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    //引入布局（ 垂直型LinearLayout(垂直型LinearLayout(一个Button)) ）
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    //获得布局中按键的实例
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    //令按键上显示“读”
                    btn.setText(getActivity().getString(R.string.read));
                    //为此按键注册点击响应的回调
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //对蓝牙设备的读操作
                            //参数1=指定的bleDevice
                            //参数2=指定的服务的UUID
                            //参数3=指定的特征的UUID
                            //参数4=读操作的回调
                            BleManager.getInstance().read(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    new BleReadCallback() {
                                        //若读取成功，则将读到的数据添加到TextView中
                                        @Override
                                        public void onReadSuccess(final byte[] data) {
                                            //1.将更新UI界面的程序放在Runnable中
                                            //2.将此Runnable对象最终传给Activity.runOnUiThread(Runnable)
                                            //若当前线程是UI线程,那么行动是立即执行
                                            //若当前线程不是UI线程,那么行动是发布到事件队列，等待进入UI线程中执行
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, HexUtil.formatHexString(data, true));
                                                }
                                            });
                                        }
                                        //若读取失败，则将异常添加到TextView中
                                        @Override
                                        public void onReadFailure(final BleException exception) {
                                            //1.将更新UI界面的程序放在Runnable中
                                            //2.将此Runnable对象最终传给Activity.runOnUiThread(Runnable)
                                            //若当前线程是UI线程,那么行动是立即执行
                                            //若当前线程不是UI线程,那么行动是发布到事件队列，等待进入UI线程中执行
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, exception.toString());
                                                }
                                            });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE: {
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    btn.setText(getActivity().getString(R.string.write));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            //对蓝牙设备的写操作
                            //参数1=指定的bleDevice
                            //参数2=指定的服务的UUID
                            //参数3=指定的待写入的字节数组
                            //参数4=写操作的回调
                            BleManager.getInstance().write(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    HexUtil.hexStringToBytes(hex),
                                    new BleWriteCallback() {

                                        @Override
                                        public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                            //1.将更新UI界面的程序放在Runnable中
                                            //2.将此Runnable对象最终传给Activity.runOnUiThread(Runnable)
                                            //若当前线程是UI线程,那么行动是立即执行
                                            //若当前线程不是UI线程,那么行动是发布到事件队列，等待进入UI线程中执行
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, "write success, current: " + current
                                                            + " total: " + total
                                                            + " justWrite: " + HexUtil.formatHexString(justWrite, true));
                                                }
                                            });
                                        }

                                        @Override
                                        public void onWriteFailure(final BleException exception) {
                                            //1.将更新UI界面的程序放在Runnable中
                                            //2.将此Runnable对象最终传给Activity.runOnUiThread(Runnable)
                                            //若当前线程是UI线程,那么行动是立即执行
                                            //若当前线程不是UI线程,那么行动是发布到事件队列，等待进入UI线程中执行
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, exception.toString());
                                                }
                                            });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE_NO_RESPONSE: {
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    btn.setText(getActivity().getString(R.string.write));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            BleManager.getInstance().write(
                                    bleDevice,
                                    characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    HexUtil.hexStringToBytes(hex),
                                    new BleWriteCallback() {

                                        @Override
                                        public void onWriteSuccess(final int current, final int total, final byte[] justWrite) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, "write success, current: " + current
                                                            + " total: " + total
                                                            + " justWrite: " + HexUtil.formatHexString(justWrite, true));
                                                }
                                            });
                                        }

                                        @Override
                                        public void onWriteFailure(final BleException exception) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    addText(txt, exception.toString());
                                                }
                                            });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_NOTIFY: {
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //通过getActivity获得当前Fragment相关联的Activity的实例
                            if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                                //通过getActivity获得当前Fragment相关联的Activity的实例
                                btn.setText(getActivity().getString(R.string.close_notification));
                                BleManager.getInstance().notify(
                                        bleDevice,
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new BleNotifyCallback() {

                                            @Override
                                            public void onNotifySuccess() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, "notify success");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onNotifyFailure(final BleException exception) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, exception.toString());
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCharacteristicChanged(byte[] data) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, HexUtil.formatHexString(characteristic.getValue(), true));
                                                    }
                                                });
                                            }
                                        });
                            } else {
                                //通过getActivity获得当前Fragment相关联的Activity的实例
                                btn.setText(getActivity().getString(R.string.open_notification));
                                BleManager.getInstance().stopNotify(
                                        bleDevice,
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_INDICATE: {
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    //通过getActivity获得当前Fragment相关联的Activity的实例
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //通过getActivity获得当前Fragment相关联的Activity的实例
                            if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                                //通过getActivity获得当前Fragment相关联的Activity的实例
                                btn.setText(getActivity().getString(R.string.close_notification));
                                BleManager.getInstance().indicate(
                                        bleDevice,
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new BleIndicateCallback() {

                                            @Override
                                            public void onIndicateSuccess() {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, "indicate success");
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onIndicateFailure(final BleException exception) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, exception.toString());
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onCharacteristicChanged(byte[] data) {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        addText(txt, HexUtil.formatHexString(characteristic.getValue(), true));
                                                    }
                                                });
                                            }
                                        });
                            } else {
                                //通过getActivity获得当前Fragment相关联的Activity的实例
                                btn.setText(getActivity().getString(R.string.open_notification));
                                BleManager.getInstance().stopIndicate(
                                        bleDevice,
                                        characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;
            }

            layout_container.addView(view);
        }
    }

    //若当前Fragment已被添加到一个Activity中，且Activity非null，则
    private void runOnUiThread(Runnable runnable) {
        if (isAdded() && getActivity() != null)
            getActivity().runOnUiThread(runnable);
    }

    //向指定的TextView控件中添加指定的内容content
    //添加完后换行，并计算TextView控件内的行数*行高，若大于TextView控件的高度，就垂直滚动到“刚好显示最后一行”
    private void addText(TextView textView, String content) {
        textView.append(content);
        textView.append("\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }


}
