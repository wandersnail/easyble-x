package com.wandersnail.bledemo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.snail.commons.utils.ToastUtils;
import com.snail.treeadapter.Node;
import com.snail.treeadapter.OnInnerItemClickListener;
import com.snail.treeadapter.TreeAdapter;
import com.wang.avi.AVLoadingIndicatorView;
import easyble2.*;
import easyble2.callback.CharacteristicReadCallback;
import easyble2.callback.ConnectionStateChangeListener;
import easyble2.util.BleUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * date: 2019/8/2 23:33
 * author: zengfansheng
 */
public class MainActivity extends AppCompatActivity {
    private Device device;
    private ListView lv;
    private FrameLayout layoutConnecting;
    private AVLoadingIndicatorView loadingIndicator;
    private ImageView ivDisconnected;
    private List<Item> itemList = new ArrayList<>();
    private ListViewAdapter adapter;
    private Connection connection;

    private void assignViews() {
        lv = findViewById(R.id.lv);
        layoutConnecting = findViewById(R.id.layoutConnecting);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        ivDisconnected = findViewById(R.id.ivDisconnected);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        device = getIntent().getParcelableExtra("device");
        setContentView(R.layout.activity_main);
        assignViews();
        initViews();
        //注册观察者
        EasyBLE.getInstance().registerObserver(observer);
        //连接配置，举个例随意配置两项
        ConnectionConfiguration config = new ConnectionConfiguration();
        config.setConnectTimeoutMillis(10000);
        config.setRequestTimeoutMillis(1000);
//        connection = EasyBLE.getInstance().connect(device, config, connectionStateChangeListener);//回调监听连接状态
        connection = EasyBLE.getInstance().connect(device, config);//观察者监听连接状态         
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EasyBLE.getInstance().unregisterObserver(observer);
        //释放连接
        EasyBLE.getInstance().releaseConnection(device);
    }

    private ConnectionStateChangeListener connectionStateChangeListener = new ConnectionStateChangeListener() {
        @Override
        public void onConnectionStateChanged(@NonNull Device device) {

        }

        @Override
        public void onConnectFailed(@NonNull Device device, int failType) {

        }

        @Override
        public void onConnectTimeout(@NonNull Device device, int type) {

        }
    };

    private EventObserver observer = new SimpleEventObserver() {
        //可以使用注解指定回调线程
        @RunOn(threadMode = ThreadMode.MAIN)
        @Override
        public void onConnectionStateChanged(@NonNull Device device) {
            switch (device.connectionState) {
                case Connection.STATE_SCANNING:
                    ivDisconnected.setVisibility(View.INVISIBLE);
                    break;
                case Connection.STATE_CONNECTING:
                    layoutConnecting.setVisibility(View.VISIBLE);
                    loadingIndicator.setVisibility(View.VISIBLE);
                    ivDisconnected.setVisibility(View.INVISIBLE);
                    break;
                case Connection.STATE_DISCONNECTED:
                    layoutConnecting.setVisibility(View.VISIBLE);
                    loadingIndicator.setVisibility(View.INVISIBLE);
                    ivDisconnected.setVisibility(View.VISIBLE);
                    break;
                case Connection.STATE_SERVICE_DISCOVERED:
                    layoutConnecting.setVisibility(View.INVISIBLE);
                    loadingIndicator.setVisibility(View.INVISIBLE);
                    itemList.clear();
                    int id = 0;
                    List<BluetoothGattService> services = connection.getGatt().getServices();
                    for (BluetoothGattService service : services) {
                        int pid = id;
                        Item item = new Item();
                        item.setId(pid);
                        item.isService = true;
                        item.service = service;
                        itemList.add(item);
                        id++;
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            Item i = new Item();
                            i.setId(id++);
                            i.setPId(pid);
                            i.setLevel(1);
                            i.service = service;
                            i.characteristic = characteristic;
                            itemList.add(i);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    break;
            }
        }

        @Override
        public void onConnectFailed(@NonNull Device device, int failType) {

        }

        @Override
        public void onConnectTimeout(@NonNull Device device, int type) {

        }

        @Override
        public void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, boolean isEnabled) {
            if (isEnabled) {
                ToastUtils.showShort("通知开启了");
            } else {
                ToastUtils.showShort("通知关闭了");
            }
        }

        @Override
        public void onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
            Log.d("onCharacteristicWrite", "成功写入：" + BleUtils.bytesToHex(value));
            ToastUtils.showShort("成功写入：" + BleUtils.bytesToHex(value));
        }
    };

    private void initViews() {
        adapter = new ListViewAdapter(lv, itemList);
        lv.setAdapter(adapter);
        adapter.setOnInnerItemClickListener(new OnInnerItemClickListener<Item>() {
            @Override
            public void onClick(@NotNull final Item item, @NotNull AdapterView<?> adapterView, @NotNull View view, int i) {
                final List<String> menuItems = new ArrayList<>();
                if (item.hasNotifyProperty) {
                    menuItems.add("开关通知");
                }
                if (item.hasReadProperty) {
                    menuItems.add("读取特征值");
                }
                final String writeItem = String.format(Locale.US, "写数据: test write(%s)", BleUtils.bytesToHex("test write".getBytes()));
                if (item.hasWriteProperty) {
                    menuItems.add(writeItem);
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setItems(menuItems.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                switch (menuItems.get(which)) {
                                    case "开关通知":
                                        boolean isEnabled = connection.isNotificationOrIndicationEnabled(item.service.getUuid(), item.characteristic.getUuid());
                                        //使用观察者模式接收结果
                                        connection.setNotificationEnabled("enable nofity", item.service.getUuid(), item.characteristic.getUuid(), !isEnabled);
                                        //使用回调方式接收结果
//                                        connection.setNotificationEnabled("enable nofity", item.service.getUuid(), item.characteristic.getUuid(), true, new NotificationChangedCallback() {
//                                            @Override
//                                            public void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, boolean isEnabled) {
//
//                                            }
//
//                                            @Override
//                                            public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {
//
//                                            }
//                                        });
                                        break;
                                    case "读取特征值":
                                        //使用观察者模式接收结果
//                                        connection.readCharacteristic("read characteristic", item.service.getUuid(), item.characteristic.getUuid());
                                        //使用回调方式接收结果
                                        connection.readCharacteristic("read characteristic", item.service.getUuid(), item.characteristic.getUuid(), new CharacteristicReadCallback() {
                                            @Override
                                            @RunOn(threadMode = ThreadMode.BACKGROUND)
                                            public void onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
                                                Log.d("onCharacteristicRead", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()));
                                                Log.d("onCharacteristicRead", "读取到特征值：" + BleUtils.bytesToHex(value));
                                                ToastUtils.showShort("读取到特征值：" + BleUtils.bytesToHex(value));
                                            }

                                            @Override
                                            public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {

                                            }
                                        });
                                        break;
                                    default:
                                        connection.writeCharacteristic("write characteristic", item.service.getUuid(), item.characteristic.getUuid(), "test write".getBytes());
                                        break;
                                }
                            }
                        })
                        .show();
            }
        });
    }

    private class ListViewAdapter extends TreeAdapter<Item> {
        ListViewAdapter(@NotNull ListView lv, @NotNull List<? extends Item> nodes) {
            super(lv, nodes);
        }

        @NotNull
        @Override
        protected Holder<Item> getHolder(int i) {
            //根据位置返回不同布局
            int type = getItemViewType(i);
            if (type == 1) {//服务
                return new Holder<Item>() {
                    private ImageView iv;
                    private TextView tvUuid;

                    @Override
                    public void setData(Item item, int position) {
                        iv.setVisibility(item.hasChild() ? View.VISIBLE : View.INVISIBLE);
                        iv.setBackgroundResource(item.isExpand() ? R.drawable.expand : R.drawable.fold);
                        tvUuid.setText(item.service.getUuid().toString());
                    }

                    @NotNull
                    @Override
                    protected View createConvertView() {
                        View view = View.inflate(MainActivity.this, R.layout.item_service, null);
                        iv = view.findViewById(R.id.ivIcon);
                        tvUuid = view.findViewById(R.id.tvUuid);
                        return view;
                    }
                };
            } else {
                return new Holder<Item>() {
                    private TextView tvUuid;
                    private TextView tvProperty;

                    @Override
                    public void setData(Item item, int i) {
                        tvUuid.setText(item.characteristic.getUuid().toString());
                        //获取权限列表
                        tvProperty.setText(getPropertiesString(item));
                    }

                    @NotNull
                    @Override
                    protected View createConvertView() {
                        View view = View.inflate(MainActivity.this, R.layout.item_characteristic, null);
                        tvUuid = view.findViewById(R.id.tvUuid);
                        tvProperty = view.findViewById(R.id.tvProperty);
                        return view;
                    }
                };
            }
        }

        private String getPropertiesString(Item node) {
            StringBuilder sb = new StringBuilder();
            int[] properties = new int[]{BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE};
            String[] propertyStrs = new String[]{"WRITE", "INDICATE", "NOTIFY", "READ", "SIGNED_WRITE", "WRITE_NO_RESPONSE"};
            for (int i = 0; i < properties.length; i++) {
                int property = properties[i];
                if ((node.characteristic.getProperties() & property) != 0) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(propertyStrs[i]);
                    if (property == BluetoothGattCharacteristic.PROPERTY_NOTIFY || property == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                        node.hasNotifyProperty = true;
                    }
                    if (property == BluetoothGattCharacteristic.PROPERTY_WRITE || property == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) {
                        node.hasWriteProperty = true;
                    }
                    if (property == BluetoothGattCharacteristic.PROPERTY_READ) {
                        node.hasReadProperty = true;
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public int getViewTypeCount() {
            return super.getViewTypeCount() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).isService ? 1 : 0;
        }
    }

    private class Item extends Node<Item> {
        boolean isService;
        BluetoothGattService service;
        BluetoothGattCharacteristic characteristic;
        boolean hasNotifyProperty;
        boolean hasWriteProperty;
        boolean hasReadProperty;
    }
}
