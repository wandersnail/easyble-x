package easyble2;

import androidx.annotation.NonNull;
import com.snail.commons.methodpost.ThreadMode;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * date: 2019/8/9 14:01
 * author: zengfansheng
 */
class ObserverMethod {
    private final Method method;    
    private final ThreadMode threadMode;

    ObserverMethod(@NonNull Method method, @NonNull ThreadMode threadMode) {
        this.method = method;
        this.threadMode = threadMode;
    }

    Method getMethod() {
        return method;
    }

    ThreadMode getThreadMode() {
        return threadMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObserverMethod)) return false;
        ObserverMethod that = (ObserverMethod) o;
        return method.equals(that.method) && threadMode == that.threadMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, threadMode);
    }
}
