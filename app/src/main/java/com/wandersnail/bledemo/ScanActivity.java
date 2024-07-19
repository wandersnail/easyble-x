package com.wandersnail.bledemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wandersnail.bledemo.databinding.ActivityScanBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.wandersnail.ble.Device;
import cn.wandersnail.ble.EasyBLE;
import cn.wandersnail.ble.ScannerType;
import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.commons.helper.PermissionsRequester2;
import cn.wandersnail.commons.poster.ThreadMode;
import cn.wandersnail.commons.util.ToastUtils;
import cn.wandersnail.widget.listview.BaseListAdapter;
import cn.wandersnail.widget.listview.BaseViewHolder;

/**
 * date: 2019/8/4 15:13
 * author: zengfansheng
 */
public class ScanActivity extends BaseViewBindingActivity<ActivityScanBinding> {
    private ListAdapter listAdapter;
    private final List<Device> devList = new ArrayList<>();
    private PermissionsRequester2 permissionsRequester;
    private boolean scanning;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
        EasyBLE.getInstance().addScanListener(scanListener);        
        checkPermissions();
    }

    private void initViews() {
        listAdapter = new ListAdapter(this, devList);
        binding.lv.setAdapter(listAdapter);
        binding.lv.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(ScanActivity.this, MainActivity.class);
            intent.putExtra("device", devList.get(position));
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();        
        EasyBLE.getInstance().release();
        System.exit(0);
    }

    private final ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStart() {
            scanning = true;
            invalidateOptionsMenu();
        }

        @Override
        public void onScanStop() {
            scanning = false;
            invalidateOptionsMenu();
        }

        @Override
        public void onScanResult(@NonNull Device device, boolean isConnectedBySys) {
            binding.layoutEmpty.setVisibility(View.INVISIBLE);
            listAdapter.add(device);
        }

        @Override
        public void onScanError(int errorCode, @NotNull String errorMsg) {
            List<String> list = new ArrayList<>();
            switch(errorCode) {
                case ScanListener.ERROR_LACK_LOCATION_PERMISSION://缺少定位权限
                    ToastUtils.showShort("缺少定位权限");
                    if (getApplicationInfo().targetSdkVersion >= 29) {//target sdk版本在29以上的需要精确定位权限才能搜索到蓝牙设备
                        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
                    } else {
                        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                    }
                    permissionsRequester.checkAndRequest(list);
            		break;
                case ScanListener.ERROR_LOCATION_SERVICE_CLOSED://位置服务未开启	
                    ToastUtils.showShort("位置服务未开启");    
            		break;
                case ScanListener.ERROR_LACK_SCAN_PERMISSION://缺少搜索权限	
                    ToastUtils.showShort("缺少搜索权限");
                    list.add(Manifest.permission.BLUETOOTH_SCAN);
                    permissionsRequester.checkAndRequest(list);
            		break;
                case ScanListener.ERROR_LACK_CONNECT_PERMISSION://缺少连接权限
                    ToastUtils.showShort("缺少连接权限");
                    list.add(Manifest.permission.BLUETOOTH_CONNECT);
                    permissionsRequester.checkAndRequest(list);
                    break;
                case ScanListener.ERROR_SCAN_FAILED://搜索失败
                    ToastUtils.showShort("搜索出错：" + errorCode);
                    break;
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Main", "onResume");
        if (EasyBLE.getInstance().isInitialized()) {
            if (EasyBLE.getInstance().isBluetoothOn()) {
                doStartScan();
            } else {
                try {
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } catch (Exception ignore) {
                }
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (EasyBLE.getInstance().isInitialized()) {
            EasyBLE.getInstance().stopScan();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        MenuItem item = menu.findItem(R.id.menuProgress);
        item.setActionView(R.layout.toolbar_indeterminate_progress);
        item.setVisible(scanning);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuScan) {
            doStartScan();
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkPermissions() {
        //动态申请权限
        permissionsRequester = new PermissionsRequester2(this);
        permissionsRequester.setCallback(list -> {
            
        });
    }

    private void doStartScan() {
        listAdapter.clear();
        binding.layoutEmpty.setVisibility(View.VISIBLE);
        if (binding.rbClassic.isChecked()) {
            EasyBLE.getInstance().scanConfiguration.setScannerType(ScannerType.CLASSIC);
        } else if (binding.rbLe.isChecked()) {
            EasyBLE.getInstance().scanConfiguration.setScannerType(ScannerType.LE);
        } else {
            EasyBLE.getInstance().scanConfiguration.setScannerType(ScannerType.LEGACY);
        }
        EasyBLE.getInstance().stopScanQuietly();
        EasyBLE.getInstance().startScan();
    }

    @NonNull
    @Override
    public Class<ActivityScanBinding> getViewBindingClass() {
        return ActivityScanBinding.class;
    }

    private static class ListAdapter extends BaseListAdapter<Device> {
        private final HashMap<String, Long> updateTimeMap = new HashMap<>();
        private final HashMap<String, TextView> rssiViews = new HashMap<>();

        ListAdapter(@NotNull Context context, @NotNull List<Device> list) {
            super(context, list);
        }

        @NotNull
        @Override
        protected BaseViewHolder<Device> createViewHolder(int i) {
            return new BaseViewHolder<Device>() {
                TextView tvName;
                TextView tvAddr;
                TextView tvRssi;

                @Override
                public void onBind(@NonNull Device device, int i) {
                    rssiViews.put(device.getAddress(), tvRssi);
                    tvName.setText(device.getName().isEmpty() ? "N/A" : device.getName());
                    tvAddr.setText(device.getAddress());
                    tvRssi.setText("" + device.getRssi());
                }

                @NotNull
                @Override
                public View createView() {
                    View view = View.inflate(context, R.layout.item_scan, null);
                    tvName = view.findViewById(R.id.tvName);
                    tvAddr = view.findViewById(R.id.tvAddr);
                    tvRssi = view.findViewById(R.id.tvRssi);
                    return view;
                }
            };
        }

        void clear() {
            getItems().clear();
            notifyDataSetChanged();
        }

        void add(Device device) {
            Device dev = null;
            for (Device item : getItems()) {
                if (item.equals(device)) {
                    dev = item;
                    break;
                }
            }
            if (dev == null) {
                updateTimeMap.put(device.getAddress(), System.currentTimeMillis());
                getItems().add(device);
                notifyDataSetChanged();
            } else {
                Long time = updateTimeMap.get(device.getAddress());
                if (time == null || System.currentTimeMillis() - time > 2000) {
                    updateTimeMap.put(device.getAddress(), System.currentTimeMillis());
                    if (dev.getRssi() != device.getRssi()) {
                        dev.setRssi(device.getRssi());
                        final TextView tvRssi = rssiViews.get(device.getAddress());
                        if (tvRssi != null) {
                            tvRssi.setText("" + device.getRssi());
                            tvRssi.setTextColor(Color.BLACK);
                            tvRssi.postDelayed(() -> tvRssi.setTextColor(0xFF909090), 800);
                        }
                    }
                }
            }
        }
    }
}
