package com.wandersnail.bledemo;

import android.app.Application;
import android.bluetooth.le.ScanSettings;

import com.snail.commons.AppHolder;
import com.snail.commons.methodpost.ThreadMode;

import easyble2.EasyBLE;
import easyble2.ScanConfiguration;

/**
 * date: 2019/8/4 15:14
 * author: zengfansheng
 */
public class MyApplication extends Application {
    private static MyApplication instance;
    private MyObservable observable = new MyObservable();
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        AppHolder.initialize(this);
        //构建自定义实例，需要在EasyBLE.getInstance()之前
        ScanConfiguration scanConfig = new ScanConfiguration()
                .setScanSettings(new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                        .build())
                .setScanPeriodMillis(15000)
                .setAcceptSysConnectedDevice(true)
                .setOnlyAcceptBleDevice(true);
        EasyBLE ble = EasyBLE.getBuilder().setScanConfigation(scanConfig)
                .setObserveAnnotationRequired(false)//不强制使用{@link Observe}注解才会收到被观察者的消息，强制使用的话，性能会好一些
                .setEventObservable(observable)
                .setMethodDefaultThreadMode(ThreadMode.BACKGROUND)//指定回调方法和观察者方法的默认线程
                .build();
        ble.setLogEnabled(true);//开启日志打印
        ble.initialize(this);
    }
    
    public static MyApplication getInstance() {
        return instance;
    }

    public MyObservable getObservable() {
        return observable;
    }
}
