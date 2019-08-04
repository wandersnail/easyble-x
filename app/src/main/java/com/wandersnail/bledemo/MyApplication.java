package com.wandersnail.bledemo;

import android.app.Application;
import android.bluetooth.le.ScanSettings;
import com.snail.commons.AppHolder;
import easyble2.EasyBLE;
import easyble2.ScanConfiguration;
import easyble2.ThreadMode;

/**
 * date: 2019/8/4 15:14
 * author: zengfansheng
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppHolder.init(this);
        //构建自定义实例，需要在EasyBLE.getInstance()之前
        ScanConfiguration scanConfig = new ScanConfiguration()
                .setScanSettings(new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build())
                .setScanPeriodMillis(15000)
                .setAcceptSysConnectedDevice(true)
                .setOnlyAcceptBleDevice(true);
        EasyBLE ble = EasyBLE.builder().setScanConfigation(scanConfig)
                .setMethodDefaultThreadMode(ThreadMode.POSTING)//指定回调的默认线程，接口回调方式和观察者模式都生效
                .build();
        ble.setLogEnabled(true);//开启日志打印
        ble.initialize(this);
    }
}
