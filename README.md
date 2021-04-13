# Android BLE开发框架

## 基于此库编写的BLE调试助手——BLE调试宝，各大应用市场都已上架，欢迎下载使用。下面放两个常用的应用市场地址
[![](https://img.shields.io/badge/Download-%E5%8D%8E%E4%B8%BA%E5%BA%94%E7%94%A8%E5%B8%82%E5%9C%BA-red)](https://appstore.huawei.com/app/C100302733)

[![](https://img.shields.io/badge/Download-小米应用商店-yellow.svg)](http://app.mi.com/details?id=cn.zfs.bledebugger)

> 第一次进入是主页的话，重新再进一次就是应用详情页了

## 最新版本

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cn.wandersnail/easyble-x/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cn.wandersnail/easyble-x)
[![Download](https://api.bintray.com/packages/wandersnail/androidx/easyble-x/images/download.svg)](https://bintray.com/wandersnail/androidx/easyble-x/_latestVersion)
[![Release](https://jitpack.io/v/wandersnail/easyble-x.svg)](https://jitpack.io/#wandersnail/easyble-x)
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

1. 因为使用了jdk8的一些特性，需要在module的build.gradle里添加如下配置：
```
//纯java的项目
android {
	compileOptions {
		sourceCompatibility JavaVersion.VERSION_1_8
		targetCompatibility JavaVersion.VERSION_1_8
	}
}

//有kotlin的项目还需要在project的build.gradle里添加
allprojects {
    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        kotlinOptions {
            jvmTarget = '1.8'
            apiVersion = '1.3'
            languageVersion = '1.3'
        }
    }
}
```

2. module的build.gradle中的添加依赖，自行修改为最新版本，同步后通常就可以用了：
```
dependencies {
	...
	implementation 'cn.wandersnail:easyble-x:latestVersion'
	implementation 'cn.wandersnail:common-full:latestVersion'
}
```

3. 如果从jcenter下载失败。在project的build.gradle里的repositories添加内容，最好两个都加上，添加完再次同步即可。
```
allprojects {
	repositories {
		...
		mavenCentral()
		maven { url 'https://dl.bintray.com/wandersnail/androidx/' }
	}
}
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
		.setObserveAnnotationRequired(false)//不强制使用{@link Observe}注解才会收到被观察者的消息，强制使用的话，性能会好一些
		.setMethodDefaultThreadMode(ThreadMode.MAIN)//指定回调方法和观察者方法的默认线程
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

```
-keep class * implements cn.wandersnail.commons.observer.Observe {
	public <methods>;
}
#保持 Serializable 不被混淆
-keep class * implements cn.wandersnail.ble.Request {
    !private *;
}
```

## Demo效果预览
![image](https://s2.ax1x.com/2020/02/29/3sWVn1.png)
![image](https://s2.ax1x.com/2020/02/29/3sWAXR.png)
![image](https://s2.ax1x.com/2020/02/29/3sWe76.gif)
