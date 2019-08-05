package easyble2;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * date: 2019/8/3 11:19
 * author: zengfansheng
 */
final class Poster {
    private final Handler mainHandler;
    private final BackgroundPoster backgroundPoster;
    private final EasyBLE easyBle;

    Poster(EasyBLE easyBle) {
        this.easyBle = easyBle;
        mainHandler = new Handler(Looper.getMainLooper());
        backgroundPoster = new BackgroundPoster(easyBle);
    }

    /**
     * 将Runnable放到主线程执行
     *
     * @param handler  主线程的handler
     * @param runnable 要执行的任务
     */
    static void postToMainThread(@NonNull Handler handler, @NonNull Runnable runnable) {
        Inspector.requireNonNull(runnable, "runnable is null");
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
        Inspector.requireNonNull(runnable, "runnable is null");
        if (method != null) {
            RunOn annotation = method.getAnnotation(RunOn.class);
            ThreadMode mode;
            if (annotation == null) {
                mode = easyBle.methodDefaultThreadMode;
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
        Inspector.requireNonNull(runnable, "runnable is null");
        switch (mode) {
            case MAIN:
                postToMainThread(mainHandler, runnable);
                break;
            case POSTING:
                runnable.run();
                break;
            case BACKGROUND:
                backgroundPoster.enqueue(runnable);
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
        Inspector.requireNonNull(methodName, "methodName is null");
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
     * @param methodInfo  方法信息实例
     */
    void post(@NonNull Object owner, @NonNull MethodInfo methodInfo) {
        Inspector.requireNonNull(methodInfo, "methodInfo is null");
        post(owner, methodInfo.name, methodInfo.pairs);
    }
}
