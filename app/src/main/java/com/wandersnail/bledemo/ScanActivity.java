package com.wandersnail.bledemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.wandersnail.ble.Device;
import cn.wandersnail.ble.EasyBLE;
import cn.wandersnail.ble.callback.ScanListener;
import cn.wandersnail.commons.helper.PermissionsRequester;
import cn.wandersnail.widget.listview.BaseListAdapter;
import cn.wandersnail.widget.listview.BaseViewHolder;
import cn.wandersnail.widget.listview.PullRefreshLayout;

/**
 * date: 2019/8/4 15:13
 * author: zengfansheng
 */
public class ScanActivity extends AppCompatActivity {
    private ListAdapter listAdapter;
    private PullRefreshLayout refreshLayout;
    private LinearLayout layoutEmpty;
    private List<Device> devList = new ArrayList<>();
    private PermissionsRequester permissionsRequester;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        initViews();
        EasyBLE.getInstance().addScanListener(scanListener);        
        initialize();
    }

    private void initViews() {
        refreshLayout = findViewById(R.id.refreshLayout);
        ListView lv = findViewById(R.id.lv);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        refreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent));
        listAdapter = new ListAdapter(this, devList);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(ScanActivity.this, MainActivity.class);
            intent.putExtra("device", devList.get(position));
            startActivity(intent);
        });
        refreshLayout.setOnRefreshListener(() -> {
            if (EasyBLE.getInstance().isInitialized()) {
                EasyBLE.getInstance().stopScan();
                doStartScan();
            }
            refreshLayout.postDelayed(() -> refreshLayout.setRefreshing(false), 500);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();        
        EasyBLE.getInstance().release();
        System.exit(0);
    }

    private ScanListener scanListener = new ScanListener() {
        @Override
        public void onScanStart() {
            invalidateOptionsMenu();
        }

        @Override
        public void onScanStop() {
            invalidateOptionsMenu();
        }

        @Override
        public void onScanResult(@NotNull Device device) {
            layoutEmpty.setVisibility(View.INVISIBLE);
            listAdapter.add(device);
        }

        @Override
        public void onScanError(int errorCode, @NotNull String errorMsg) {
            switch(errorCode) {
                case ScanListener.ERROR_LACK_LOCATION_PERMISSION://缺少定位权限		
            		break;
                case ScanListener.ERROR_LOCATION_SERVICE_CLOSED://位置服务未开启		
            		break;
                case ScanListener.ERROR_SCAN_FAILED://搜索失败
                    break;
            }
        }
    };
    
    //需要进行检测的权限
    private List<String> getNeedPermissions() {
        List<String> list = new ArrayList<>();
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return list;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Main", "onResume");
        if (EasyBLE.getInstance().isInitialized()) {
            if (EasyBLE.getInstance().isBluetoothOn()) {
                doStartScan();
            } else {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
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
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.menuProgress);
        item.setActionView(R.layout.toolbar_indeterminate_progress);
        item.setVisible(EasyBLE.getInstance().isScanning());
        return super.onCreateOptionsMenu(menu);
    }

    private void initialize() {
        //动态申请权限
        permissionsRequester = new PermissionsRequester(this);
        permissionsRequester.setCallback(list -> {
            
        });
        permissionsRequester.checkAndRequest(getNeedPermissions());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsRequester.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void doStartScan() {
        listAdapter.clear();
        layoutEmpty.setVisibility(View.VISIBLE);
        EasyBLE.getInstance().startScan();
    }

    private class ListAdapter extends BaseListAdapter<Device> {
        private HashMap<String, Long> updateTimeMap = new HashMap<>();
        private HashMap<String, TextView> rssiViews = new HashMap<>();

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
                public void onBind(Device device, int i) {
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
