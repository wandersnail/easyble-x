## 推荐一款工具箱【蜗牛工具箱】

> 涵盖广，功能丰富。生活实用、效率办公、图片处理等等，还有隐藏的VIP功能，总之很多惊喜的功能。各大应用市场搜索【蜗牛工具箱】安装即可。

<div align="center">
    <img src="https://pic1.imgdb.cn/item/685a0c1658cb8da5c8696135.png" width=150>
    <img src="https://pic1.imgdb.cn/item/685a0c1758cb8da5c869613d.png" width=150>
    <img src="https://pic1.imgdb.cn/item/685a0c1758cb8da5c869613e.png" width=150>
    <img src="https://pic1.imgdb.cn/item/685a0c1758cb8da5c8696140.png" width=150>
</div>

**部分功能介绍**

- 【滚动字幕】超实用应援打call神器，输入文字内容使文字在屏幕中滚动显示；
- 【振动器】可自定义振动频率、时长，达到各种有意思的效果；
- 【测量仪器】手机当直尺、水平仪、指南针、分贝仪；
- 【文件加解密】可加密任意文件，可用于私密文件分享；
- 【金额转大写】将阿拉伯数字类型的金额转成中文大写；
- 【二维码】调用相机扫描或扫描图片识别二维码，支持解析WiFi二维码获取密码，输入文字生成相应的二维码；
- 【图片模糊处理】将图片进行高斯模糊处理，毛玻璃效果；
- 【黑白图片上色】黑白图片变彩色；
- 【成语词典】查询成语拼音、释义、出处、例句；
- 【图片拼接】支持长图、4宫格、9宫格拼接；
- 【自动点击】自动连点器，解放双手；
- 【图片加水印】图片上添加自定义水印；
- 【网页定时刷新】设定刷新后自动定时刷新网页；
- 【应用管理】查看本机安装的应用详细信息，并可提取安装包分享；
- 【BLE调试】低功耗蓝牙GATT通信调试，支持主从模式，可多设备同时连接，实时日志；
- 【SPP蓝牙调试】经典蓝牙Socket通信调试，支持自定义UUID，多设备同时连接，实时日志；
- 【USB调试】USB串口调试，兼容芯片多，实时日志；
- 【MQTT调试】MQTT通信调试，实时日志、自定义按键、订阅主题保存；
- 【TCP/UDP调试】支持TCP客户端、TCP服务端、UDP客户端、UDP服务端；
- 【私密相册】加密存储图片，保护个人隐私；
- ……

已集成上百个小工具，持续更新中...

点击下方按钮或扫码下载【蜗牛工具箱】

[![](https://img.shields.io/badge/下载-%E8%9C%97%E7%89%9B%E5%B7%A5%E5%85%B7%E7%AE%B1-red.svg)](https://www.pgyer.com/8AN5OhVd)

<img src="https://www.pgyer.com/app/qrcode/8AN5OhVd" width=150>

----------------------------------------------

# Android BLE开发框架使用说明

## 最新版本

[![Maven Central](https://img.shields.io/maven-central/v/cn.wandersnail/easyble-x.svg?color=4AC61C)](https://central.sonatype.com/artifact/cn.wandersnail/easyble-x/versions)
[![Release](https://jitpack.io/v/cn.wandersnail/easyble-x.svg)](https://jitpack.io/#cn.wandersnail/easyble-x)
[![](https://img.shields.io/badge/源码-github-blue.svg)](https://github.com/wandersnail/easyble-x)
[![](https://img.shields.io/badge/源码-码云-red.svg)](https://gitee.com/fszeng/easyble-x)

## 功能

- 支持多设备同时连接
- 支持广播包解析
- 支持连接同时配对
- 支持搜索系统已连接设备
- 支持搜索器设置
- 支持自定义搜索过滤条件
- 支持自动重连、最大重连次数限制、直接重连或搜索到设备再重连控制
- 支持请求延时及发送延时设置
- 支持分包大小设置、最大传输单元设置
- 支持观察者监听或回调方式。注意：观察者监听和回调只能取其一！
- 支持使用注解@RunOn控制回调线程
- 支持设置回调或观察者的方法默认执行线程
- 支持发送设置（是否等待发送结果回调再发送下一包）
- 支持写入模式设置
- 支持设置连接的传输方式
- 支持连接超时设置

## 配置

1. module的build.gradle中的添加依赖，自行将latestVersion修改为各自最新版本，commons-android [最新版本](https://gitee.com/fszeng/commons-android) ，同步后通常就可以用了：

```
dependencies {
    ...
    implementation 'cn.wandersnail:easyble-x:latestVersion'
    implementation 'cn.wandersnail:commons-android:latestVersion'
}
```

1. 如果从jcenter下载失败。在project的build.gradle里的repositories添加内容，最好两个都加上，添加完再次同步即可。

```
allprojects {
    repositories {
        ...
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. AndroidManifest.xml配置权限，以下权限在SDK中已配置，如想去除某些可在app的AndroidManifest声明对应权限，然后加上tools:node="remove"

```
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
<!--  ACCESS_COARSE_LOCATION在target 28以下可使用  -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<!--  ACCESS_FINE_LOCATION 在target 29以上必须，否则不会有搜索结果  -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<!--  BLUETOOTH_SCAN 在target 31以上必须，否则直接抛异常  -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<!--  BLUETOOTH_CONNECT 在target 31以上必须，否则直接抛异常  -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## 使用方法

### 初始化SDK

**实例化有两种方式：**

1. 使用默认方式自动构建实例，直接获取实例即可

```
//实例化并初始化
EasyBLE.getInstance().initialize(this);
```

2. 构建自定义实例，必须在EasyBLE.getInstance()之前！！

```
ScanConfiguration scanConfig = new ScanConfiguration()
        .setScanSettings(new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)//搜索模式
                .build())
        .setScanPeriodMillis(15000)//搜索周期
        .setAcceptSysConnectedDevice(true)//是否将系统蓝牙已连接的设备放到搜索结果中
        .setOnlyAcceptBleDevice(true);//是否过滤非ble设备
EasyBLE ble = EasyBLE.getBuilder().setScanConfiguration(scanConfig)
        .setScannerType(ScannerType.LE)//指定蓝牙扫描器，默认为系统Android5.0以上使用ScannerType.LE
        .setExecutorService(executorService)//自定义线程池用来执行后台任务，也可使用默认
        .setDeviceCreator(creator)//设备实例构建器。返回搜索结果时的设备对象由此构建器实例化
        .setObserveAnnotationRequired(false)//不强制使用{@link Observe}注解才会收到被观察者的消息，默认为false
        .setMethodDefaultThreadMode(ThreadMode.MAIN)//指定回调方法和观察者方法的默认线程，默认为ThreadMode.MAIN
        .build();
ble.initialize(this);
```

### 销毁SDK

```
//如果中途需要修改配置重新实例化，调用此方法后即可重新构建EasyBLE实例
EasyBLE.getInstance().destroy();
```

### 日志输出控制

```
EasyBLE.getInstance().setLogEnabled(true);//开启日志打印
```

### 蓝牙搜索

1. 定义搜索监听器
   
   > Android6.0以上搜索需要至少模糊定位权限，如果targetSdkVersion设置29以上需要精确定位权限。权限需要动态申请

```
private ScanListener scanListener = new ScanListener() {
    @Override
    public void onScanStart() {
        //搜索开始
    }

    @Override
    public void onScanStop() {
        //搜索停止
    }

    /**
     * 搜索到BLE设备
     *
     * @param device           搜索到的设备
     * @param isConnectedBySys 是否已被系统蓝牙连接上
     */
    @Override
    public void onScanResult(@NonNull Device device, boolean isConnectedBySys) {
        //搜索结果
    }

    @Override
    public void onScanError(int errorCode, @NotNull String errorMsg) {
        switch(errorCode) {
            case ScanListener.ERROR_LACK_LOCATION_PERMISSION://缺少定位权限        
                break;
            case ScanListener.ERROR_LOCATION_SERVICE_CLOSED://位置服务未开启        
                break;
            case ScanListener.ERROR_LACK_LOCATION_PERMISSION://缺少定位权限        
                break;
            case ScanListener.ERROR_LACK_SCAN_PERMISSION://targetSdkVersion大于等于Android12时，缺少搜索权限(发现附近设备)    
                break;
            case ScanListener.ERROR_LACK_CONNECT_PERMISSION://targetSdkVersion大于等于Android12时，缺少连接权限    
                break;    
            case ScanListener.ERROR_SCAN_FAILED://搜索失败
                break;
        }
    }
};
```

2. 添加监听

```
EasyBLE.getInstance().addScanListener(scanListener);
```

3. 开始搜索

```
EasyBLE.getInstance().startScan();
```

4. 停止搜索

```
EasyBLE.getInstance().stopScan();
```

5. 停止监听

```
EasyBLE.getInstance().removeScanListener(scanListener);
```

### 观察者模式数据及事件

1. 定义观察者。实现EventObserver接口即可：

```
public class MainActivity extends AppCompatActivity implements EventObserver {
    /**
     * 使用{@link Observe}确定要接收消息，{@link RunOn}指定在主线程执行方法，设置{@link Tag}防混淆后找不到方法
     */
    @Tag("onConnectionStateChanged") 
    @Observe
    @RunOn(ThreadMode.MAIN)
    @Override
    public void onConnectionStateChanged(@NonNull Device device) {
        switch (device.getConnectionState()) {
            case SCANNING_FOR_RECONNECTION:
                break;
            case CONNECTING:
                break;
            case DISCONNECTED:
                break;
            case SERVICE_DISCOVERED:
                break;
        }
    }

    /**
     * 使用{@link Observe}确定要接收消息，方法在{@link EasyBLEBuilder#setMethodDefaultThreadMode(ThreadMode)}指定的线程执行
     */
    @Observe
    @Override
    public void onNotificationChanged(@NonNull Request request, boolean isEnabled) {
        if (request.getType() == RequestType.SET_NOTIFICATION) {
            if (isEnabled) {
                Log.d("EasyBLE", "通知开启了");
            } else {
                Log.d("EasyBLE", "通知关闭了");
            }
        } else {
            if (isEnabled) {
                Log.d("EasyBLE", "Indication开启了");
            } else {
                Log.d("EasyBLE", "Indication关闭了");
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
        Log.d("EasyBLE", "成功写入：" + StringUtils.toHex(value, " "));
    }
}
```

2. 注册观察者

```
EasyBLE.getInstance().registerObserver(observer);
```

3. 取消注册观察者

```
EasyBLE.getInstance().unregisterObserver(observer);
```

### 连接

1. 连接配置

```
//连接配置，举个例随意配置两项
ConnectionConfiguration config = new ConnectionConfiguration();
config.setConnectTimeoutMillis(10000);
config.setRequestTimeoutMillis(1000);
```

2. 建立连接

```
connection = EasyBLE.getInstance().connect(device, config);//观察者监听连接状态
```

3. 断开连接，还可重连

```
EasyBLE.getInstance().disconnectConnection(device);//断开指定连接
//EasyBLE.getInstance().disconnectAllConnections();//断开所有连接
```

4. 释放连接，不可重连，需要重新建立连接

```
EasyBLE.getInstance().releaseConnection(device);//释放指定连接
//EasyBLE.getInstance().releaseAllConnections();//释放所有连接
```

### 读写特征值、开启Notify

**两种方式：**

1. 接口回调方式

```
//开关通知
boolean isEnabled = connection.isNotificationOrIndicationEnabled(serviceUuid, characteristicUuid);
Request.Builder<NotificationChangeCallback> builder = Request.getSetNotificationBuilder(serviceUuid, characteristicUuid, !isEnabled);
//不设置回调，使用观察者模式接收结果
builder.build().execute(connection);

//读取特征值
Request.Builder<ReadCharacteristicCallback> builder = Request.getReadCharacteristicBuilder(serviceUuid, characteristicUuid);
builder.setTag(UUID.randomUUID().toString());
builder.setPriority(Integer.MAX_VALUE);//设置请求优先级
//设置了回调则观察者不会收到此次请求的结果消息
builder.setCallback(new ReadCharacteristicCallback() {
    //注解可以指定回调线程
    @RunOn(ThreadMode.BACKGROUND)
    @Override
    public void onCharacteristicRead(@NonNull Request request, @NonNull byte[] value) {
        Log.d("EasyBLE", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()));
        Log.d("EasyBLE", "读取到特征值：" + StringUtils.toHex(value, " "));
    }

    //不使用注解指定线程的话，使用构建器设置的默认线程
    @Override
    public void onRequestFailed(@NonNull Request request, int failType, @Nullable Object value) {

    }
});
builder.build().execute(connection);

//写特征值
Request.WriteCharacteristicBuilder builder = Request.getWriteCharacteristicBuilder(serviceUuid, characteristicUuid, "test write".getBytes());
//根据需要设置写入配置
builder.setWriteOptions(new WriteOptions.Builder()
        .setPackageSize(20)
        .setPackageWriteDelayMillis(5)
        .setRequestWriteDelayMillis(10)
        .setWaitWriteResult(true)
        .setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        .build());
//不设置回调，使用观察者模式接收结果
connection.execute(builder.build());
```

2. 使用观察者模式接收结果。不在请求构建器中设置回调即可

### 释放SDK，释放后必须重新初始化后方可使用

```
EasyBLE.getInstance().release();
```

### 代码混淆

如果使用jar方式依赖，需要添加一下混淆规则。使用aar或直接远程依赖不需要额外添加，库里自带混淆规则

```
-keep class * implements cn.wandersnail.commons.observer.Observe {
    public <methods>;
}
-keep class * implements cn.wandersnail.ble.Request {
    !private *;
}
```

## Demo效果预览

![image](https://s2.ax1x.com/2020/02/29/3sWVn1.png)
![image](https://s2.ax1x.com/2020/02/29/3sWAXR.png)
![image](https://s2.ax1x.com/2020/02/29/3sWe76.gif)
