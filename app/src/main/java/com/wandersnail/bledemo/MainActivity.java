package com.wandersnail.bledemo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.snail.commons.methodpost.RunOn;
import com.snail.commons.methodpost.ThreadMode;
import com.snail.commons.observer.Observe;
import com.snail.commons.util.StringUtils;
import com.snail.commons.util.ToastUtils;
import com.snail.treeadapter.Node;
import com.snail.treeadapter.TreeAdapter;
import com.wang.avi.AVLoadingIndicatorView;
import easyble2.*;
import easyble2.callback.NotificationChangeCallback;
import easyble2.callback.ReadCharacteristicCallback;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * date: 2019/8/2 23:33
 * author: zengfansheng
 */
public class MainActivity extends BaseActivity {
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
        //连接配置，举个例随意配置两项
        ConnectionConfiguration config = new ConnectionConfiguration();
        config.setConnectTimeoutMillis(10000);
        config.setRequestTimeoutMillis(1000);
//        connection = EasyBLE.getInstance().connect(device, config, observer);//回调监听连接状态，设置此回调不影响观察者接收连接状态消息
        connection = EasyBLE.getInstance().connect(device, config);//观察者监听连接状态         
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //释放连接
        EasyBLE.getInstance().releaseConnection(device);
    }

    /**
     * 使用{@link Observe}确定要接收消息，并在主线程执行方法
     */
    @Observe(ThreadMode.MAIN)
    @Override
    public void onConnectionStateChanged(@NonNull Device device) {
        Log.d("EasyBLE", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()) + ", 连接状态：" + device.getConnectionState());
        switch (device.getConnectionState()) {
            case SCANNING_FOR_RECONNECTION:
                ivDisconnected.setVisibility(View.INVISIBLE);
                break;
            case CONNECTING:
                layoutConnecting.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.VISIBLE);
                ivDisconnected.setVisibility(View.INVISIBLE);
                break;
            case DISCONNECTED:
                layoutConnecting.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.INVISIBLE);
                ivDisconnected.setVisibility(View.VISIBLE);
                break;
            case SERVICE_DISCOVERED:
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


    /**
     * 使用{@link Observe}确定要接收消息，方法在{@link EasyBLEBuilder#setMethodDefaultThreadMode(ThreadMode)}指定的线程执行
     */
    @Observe
    @Override
    public void onNotificationChanged(@NonNull Request request, boolean isEnabled) {
        Log.d("EasyBLE", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()) + ", 通知/Indication：" + (isEnabled ? "开启" : "关闭"));
        if (request.getType() == RequestType.SET_NOTIFICATION) {
            if (isEnabled) {
                ToastUtils.showShort("通知开启了");
            } else {
                ToastUtils.showShort("通知关闭了");
            }
        } else {
            if (isEnabled) {
                ToastUtils.showShort("Indication开启了");
            } else {
                ToastUtils.showShort("Indication关闭了");
            }
        }
    }

    /**
     * 如果{@link EasyBLEBuilder#setObserveAnnotationRequired(boolean)}设置为false时，无论加不加{@link Observe}注解都会收到消息。
     * 设置为true时，必须加{@link Observe}才会收到消息。
     * 默认为false，方法默认执行线程在{@link EasyBLEBuilder#setMethodDefaultThreadMode(ThreadMode)}指定
     */
    @Override
    public void onCharacteristicWrite(@NonNull Request request, @NonNull byte[] value) {
        Log.d("EasyBLE", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()) + ", 成功写入：" + StringUtils.toHex(value, " "));
        ToastUtils.showShort("成功写入：" + StringUtils.toHex(value, " "));
    }

    private void initViews() {
        adapter = new ListViewAdapter(lv, itemList);
        lv.setAdapter(adapter);
        adapter.setOnInnerItemClickListener((item, adapterView, view, i) -> {
            final List<String> menuItems = new ArrayList<>();
            if (item.hasNotifyProperty) {
                menuItems.add("开关通知");
            }
            if (item.hasReadProperty) {
                menuItems.add("读取特征值");
            }
            if (item.hasWriteProperty) {
                menuItems.add("写入测试数据");
            }
            new AlertDialog.Builder(MainActivity.this)
                    .setItems(menuItems.toArray(new String[0]), (dialog, which) -> {
                        dialog.dismiss();
                        switch (menuItems.get(which)) {
                            case "开关通知":
                                setNotification(item);
                                break;
                            case "读取特征值":
                                readCharacteristic(item);
                                break;
                            default:
                                writeCharacteristic(item);
                                break;
                        }
                    })
                    .show();
        });
    }

    private void writeCharacteristic(@NotNull Item item) {
        Request.WriteCharacteristicBuilder builder = Request.getWriteCharacteristicBuilder(item.service.getUuid(), 
                item.characteristic.getUuid(), ("Multi-pass deformation also shows that in high-temperature rolling process, " +
                        "the material will be softened as a result of the recovery and recrystallization, " +
                        "so the rolling force is reduced and the time interval of the passes of rough rolling should be longer.").getBytes());
        //根据需要设置写入配置
        builder.setWriteOptions(new WriteOptions.Builder()
                .setPackageSize(20)
                .setPackageWriteDelayMillis(5)
                .setRequestWriteDelayMillis(10)
                .setWaitWriteResult(true)
                .setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                .build());
        //不设置回调，使用观察者模式接收结果
        builder.build().execute(connection);
    }

    private void readCharacteristic(@NotNull Item item) {
        Request.Builder<ReadCharacteristicCallback> builder = Request.getReadCharacteristicBuilder(item.service.getUuid(), item.characteristic.getUuid());
        builder.setTag(UUID.randomUUID().toString());
        builder.setPriority(Integer.MAX_VALUE);//设置请求优先级
        //设置了回调则观察者不会收到此次请求的结果消息
        builder.setCallback(new ReadCharacteristicCallback() {
            //注解可以指定回调线程
            @RunOn(ThreadMode.BACKGROUND)
            @Override
            public void onCharacteristicRead(@NonNull Request request, @NonNull byte[] value) {
                Log.d("EasyBLE", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()) + ", 读取到特征值：" + StringUtils.toHex(value, " "));
                ToastUtils.showShort("读取到特征值：" + StringUtils.toHex(value, " "));
            }

            //不使用注解指定线程的话，使用构建器设置的默认线程
            @Override
            public void onRequestFailed(@NonNull Request request, int failType, @Nullable Object value) {

            }
        });
        builder.build().execute(connection);
    }

    private void setNotification(@NotNull Item item) {
        boolean isEnabled = connection.isNotificationOrIndicationEnabled(item.service.getUuid(), item.characteristic.getUuid());
        Request.Builder<NotificationChangeCallback> builder = Request.getSetNotificationBuilder(item.service.getUuid(), item.characteristic.getUuid(), !isEnabled);
        //不设置回调，使用观察者模式接收结果
        builder.build().execute(connection);
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
