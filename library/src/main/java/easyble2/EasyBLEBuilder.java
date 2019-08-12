package easyble2;

import androidx.annotation.NonNull;
import com.snail.commons.methodpost.ThreadMode;
import easyble2.util.Logger;
import easyble2.annotation.Observe;
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
    boolean isObserveAnnotationRequired = false;
    
    EasyBLEBuilder() {        
    }

    /**
     * 自定义线程池用来执行后台任务
     */
    public EasyBLEBuilder setExecutorService(@NonNull ExecutorService executorService) {
        Inspector.requireNonNull(executorService, "executorService is null");
        this.executorService = executorService;
        return this;
    }

    /**
     * 设备实例构建器
     */
    public EasyBLEBuilder setDeviceCreator(@NonNull DeviceCreator deviceCreator) {
        Inspector.requireNonNull(deviceCreator, "deviceCreator is null");
        this.deviceCreator = deviceCreator;
        return this;
    }

    /**
     * 配对控制器。如果设置了控制器，则会在连接时，尝试配对
     */
    public EasyBLEBuilder setBondController(@NonNull BondController bondController) {
        Inspector.requireNonNull(bondController, "bondController is null");
        this.bondController = bondController;
        return this;
    }

    /**
     * 观察者或者回调的方法在没有使用注解指定调用线程时，默认被调用的线程
     */
    public EasyBLEBuilder setMethodDefaultThreadMode(@NonNull ThreadMode mode) {
        Inspector.requireNonNull(mode, "mode is null");
        methodDefaultThreadMode = mode;
        return this;
    }

    /**
     * 被观察者，消息发布者
     */
    public EasyBLEBuilder setEventObservable(@NonNull EventObservable eventObservable) {
        Inspector.requireNonNull(eventObservable, "eventObservable is null");
        this.eventObservable = eventObservable;
        return this;
    }

    /**
     * 搜索配置
     */
    public EasyBLEBuilder setScanConfigation(@NonNull ScanConfiguration scanConfiguration) {
        Inspector.requireNonNull(scanConfiguration, "scanConfiguration is null");
        this.scanConfiguration = scanConfiguration;
        return this;
    }

    /**
     * 日志打印
     */
    public EasyBLEBuilder setLogger(@NonNull Logger logger) {
        Inspector.requireNonNull(logger, "logger is null");
        this.logger = logger;
        return this;
    }

    /**
     * 是否强制使用{@link Observe}注解才会收到被观察者的消息。强制使用的话，性能会好一些
     */
    public EasyBLEBuilder setObserveAnnotationRequired(boolean observeAnnotationRequired) {
        isObserveAnnotationRequired = observeAnnotationRequired;
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
