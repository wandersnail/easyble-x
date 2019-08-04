# Android BLE开发框架

## 最新版本
[![](https://jitpack.io/v/wandersnail/easyble2.svg)](https://jitpack.io/#wandersnail/easyble2)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wandersnail/easyble2/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.wandersnail/easyble2)
[![Download](https://api.bintray.com/packages/wandersnail/android/easyble2/images/download.svg) ](https://bintray.com/wandersnail/android/easyble2/_latestVersion)

## 功能
- 支持多设备同时连接
- 支持连接同时配对
- 支持搜索已连接设备
- 支持搜索器设置
- 支持自定义搜索过滤条件
- 支持自动重连、最大重连次数限制、直接重连或搜索到设备再重连控制
- 支持请求延时及发送延时设置
- 支持分包大小设置、最大传输单元设置
- 支持注册和取消通知监听
- 支持回调方式，支持使用注解@RunOn控制回调线程。注意：观察者监听和回调只能取其一！
- 支持发送设置（是否等待发送结果回调再发送下一包）
- 支持写入模式设置
- 支持设置连接的传输方式
- 支持连接超时设置

## 配置

1. module的build.gradle中的添加依赖，自行修改为最新版本，同步后通常就可以用了：
```
dependencies {
	...
	implementation 'com.github.wandersnail:easyble2:latestVersion'
}
```

2. 如果从jcenter下载失败。在project的build.gradle里的repositories添加内容，最好两个都加上，有时jitpack会抽风，同步不下来。添加完再次同步即可。
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
		maven { url 'https://dl.bintray.com/wandersnail/android/' }
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
				.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
				.build())
		.setScanPeriodMillis(15000)
		.setAcceptSysConnectedDevice(true)
		.setOnlyAcceptBleDevice(true);
EasyBLE ble = EasyBLE.builder().setScanConfigation(scanConfig)
		.setMethodDefaultThreadMode(ThreadMode.POSTING)//指定回调的默认线程，接口回调方式和观察者模式都生效
		.build();
ble.initialize(this);
```

### 日志输出控制

```
EasyBLE.getInstance().setLogEnabled(true);//开启日志打印
```

### 蓝牙搜索

1. 定义搜索监听器

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

	@Override
	public void onScanResult(@NotNull Device device) {
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

1. 定义观察者

```
private EventObserver observer = new SimpleEventObserver() {
	//可以使用注解指定回调线程
	@RunOn(threadMode = ThreadMode.MAIN)
	@Override
	public void onConnectionStateChanged(@NonNull Device device) {
		switch(device.connectionState) {
			case Connection.STATE_SCANNING:				
				break;
			case Connection.STATE_CONNECTING:
				break;
			case Connection.STATE_DISCONNECTED:
				break;
			case Connection.STATE_SERVICE_DISCOVERED:
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
			Log.d("EasyBLE", "通知开启了");
		} else {
			Log.d("EasyBLE", "通知关闭了");
		}
	}

	@Override
	public void onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
		Log.d("EasyBLE", "成功写入：" + BleUtils.bytesToHex(value));
	}
};
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
//connection = EasyBLE.getInstance().connect(device, config, connectionStateChangeListener);//回调监听连接状态
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
connection.setNotificationEnabled("toggle nofity", serviceUuid, characteristicUuid, true, new NotificationChangedCallback() {
	@Override
	public void onNotificationChanged(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull UUID descriptorUuid, boolean isEnabled) {

	}

	@Override
	public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {

	}
});

//读取特征值
connection.readCharacteristic("read characteristic", serviceUuid, characteristicUuid, new CharacteristicReadCallback() {
	@Override
	@RunOn(threadMode = ThreadMode.BACKGROUND)
	public void onCharacteristicRead(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
		Log.d("onCharacteristicRead", "主线程：" + (Looper.getMainLooper() == Looper.myLooper()));
		Log.d("onCharacteristicRead", "读取到特征值：" + BleUtils.bytesToHex(value));
	}

	@Override
	public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {

	}
});

//写特征值
connection.writeCharacteristic("write characteristic", serviceUuid, characteristicUuid, "test write".getBytes(), new CharacteristicWriteCallback() {
	@Override
	public void onCharacteristicWrite(@Nullable String tag, @NonNull Device device, @NonNull UUID serviceUuid, @NonNull UUID characUuid, @NonNull byte[] value) {
		
	}

	@Override
	public void onRequestFailed(@NonNull Device device, @NonNull Request request, int failType) {

	}
});
```

2. 使用观察者模式接收结果。请求的方法名和接口回调方式一致，只是方法参数少了一个接口的实例。结果接收方法上面有写到。写几个请求的例子：

```
connection.setNotificationEnabled("toggle nofity", serviceUuid, characteristicUuid, isEnabled);

connection.readCharacteristic("read characteristic", serviceUuid, characteristicUuid);

connection.writeCharacteristic("write characteristic", serviceUuid, characteristicUuid, "test write".getBytes());
```

### 释放SDK，释放后必须重新初始化后方可使用

```
EasyBLE.getInstance().release();
```

## Demo效果预览
![image](https://github.com/wandersnail/easyble2/blob/master/screenshot/preview.gif)
![image](https://github.com/wandersnail/easyble2/blob/master/screenshot/20190804220341.png)
![image](https://github.com/wandersnail/easyble2/blob/master/screenshot/20190804220312.png)

## 基于此库的BLE调试app
[![](https://img.shields.io/badge/Download-App%20Store-yellow.svg)](http://app.mi.com/details?id=cn.zfs.bledebugger)
[![](https://img.shields.io/badge/Download-APK-blue.svg)](https://raw.githubusercontent.com/wandersnail/myapks/master/bleutility-v2.10.apk)