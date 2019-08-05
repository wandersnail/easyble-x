package easyble2;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * date: 2019/8/3 11:19
 * author: zengfansheng
 */
class RunnablePoster {
    @NonNull
    private ExecutorService executor;
    @NonNull
    private Handler mainHandler;

    RunnablePoster(@NonNull ExecutorService executor, @NonNull Handler mainHandler) {
        this.executor = executor;
        this.mainHandler = mainHandler;
    }

    /**
     * 将Runnable放到主线程执行
     *
     * @param handler  主线程的handler
     * @param runnable 要执行的任务
     */
    static void postToMainThread(@NonNull Handler handler, @NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    /**
     * 根据方法上带的注解，将任务post到指定线程执行。如果方法上没有带注解，使用配置的默认值
     *
     * @param method   方法
     * @param runnable 要执行的任务
     */
    void post(@Nullable Method method, @NonNull Runnable runnable) {
        if (method != null) {
            RunOn annotation = method.getAnnotation(RunOn.class);
            ThreadMode mode;
            if (annotation == null) {
                mode = EasyBLE.instance.methodDefaultThreadMode;
            } else {
                mode = annotation.threadMode();
            }
            post(mode, runnable);
        }
    }

    /**
     * 将任务post到指定线程执行。
     *
     * @param mode     指定任务执行线程
     * @param runnable 要执行的任务
     */
    void post(@NonNull ThreadMode mode, @NonNull Runnable runnable) {
        switch (mode) {
            case MAIN:
                postToMainThread(mainHandler, runnable);
                break;
            case POSTING:
                runnable.run();
                break;
            case BACKGROUND:
                executor.execute(runnable);
                break;
        }
    }

    /**
     * 将任务post到指定线程执行
     *
     * @param owner      方法的所在的对象实例
     * @param methodName 方法名
     * @param pairs      参数信息对
     */
    void post(@NonNull final Object owner, @NonNull String methodName, @Nullable TypeValuePair... pairs) {
        if (pairs == null || pairs.length == 0) {
            try {
                final Method method = owner.getClass().getMethod(methodName);
                post(method, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(owner);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception ignored) {
            }
        } else {
            final Object[] params = new Object[pairs.length];
            final Class<?>[] paramTypes = new Class[pairs.length];
            for (int i = 0; i < pairs.length; i++) {
                TypeValuePair pair = pairs[i];
                params[i] = pair.value;
                paramTypes[i] = pair.type;
            }
            try {
                final Method method = owner.getClass().getMethod(methodName, paramTypes);
                post(method, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            method.invoke(owner, params);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 将任务post到指定线程执行
     *
     * @param owner 方法的所在的对象实例
     * @param info  方法信息实例
     */
    void post(@NonNull Object owner, @NonNull MethodInfo info) {
        post(owner, info.name, info.pairs);
    }
}
