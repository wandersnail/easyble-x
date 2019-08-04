package easyble2;

import androidx.annotation.NonNull;
import easyble2.util.Logger;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * date: 2019/8/3 12:02
 * author: zengfansheng
 */
public class EasyBLEBuilder {
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    BondController bondController;    
    DeviceCreator deviceCreator;
    ThreadMode methodDefaultThreadMode = ThreadMode.MAIN;
    ExecutorService executorService = DEFAULT_EXECUTOR_SERVICE;
    EventObservable eventObservable;
    ScanConfiguration scanConfiguration;    
    Logger logger;
    
    EasyBLEBuilder() {        
    }

    /**
     * 自定义线程池用来执行后台任务
     */
    public EasyBLEBuilder setExecutorService(@NonNull ExecutorService executorService) {
        Objects.requireNonNull(executorService);
        this.executorService = executorService;
        return this;
    }

    /**
     * 设备实例构建器
     */
    public EasyBLEBuilder setDeviceCreator(@NonNull DeviceCreator deviceCreator) {
        Objects.requireNonNull(executorService);
        this.deviceCreator = deviceCreator;
        return this;
    }

    /**
     * 配对控制器。如果设置了控制器，则会在连接时，尝试配对
     */
    public EasyBLEBuilder setBondController(@NonNull BondController bondController) {
        Objects.requireNonNull(executorService);
        this.bondController = bondController;
        return this;
    }

    /**
     * 观察者或者回调的方法在没有使用注解指定调用线程时，默认被调用的线程
     */
    public EasyBLEBuilder setMethodDefaultThreadMode(@NonNull ThreadMode mode) {
        Objects.requireNonNull(executorService);
        methodDefaultThreadMode = mode;
        return this;
    }

    /**
     * 被观察者，消息发布者
     */
    public EasyBLEBuilder setEventObservable(@NonNull EventObservable eventObservable) {
        Objects.requireNonNull(executorService);
        this.eventObservable = eventObservable;
        return this;
    }

    /**
     * 搜索配置
     */
    public EasyBLEBuilder setScanConfigation(@NonNull ScanConfiguration scanConfiguration) {
        Objects.requireNonNull(executorService);
        this.scanConfiguration = scanConfiguration;
        return this;
    }

    /**
     * 日志打印
     */
    public EasyBLEBuilder setLogger(@NonNull Logger logger) {
        Objects.requireNonNull(executorService);
        this.logger = logger;
        return this;
    }

    /**
     * 根据当前配置构建EasyBLE实例
     */
    public EasyBLE build() {
        synchronized (EasyBLE.class) {
            if (EasyBLE.instance != null) {
                throw new EasyBLEException("EasyBLE instance already exists. It can only be instantiated once.");
            }
            EasyBLE.instance = new EasyBLE(this);
            return EasyBLE.instance;
        }
    }
}
