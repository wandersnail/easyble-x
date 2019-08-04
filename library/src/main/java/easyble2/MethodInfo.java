package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * date: 2019/8/3 09:28
 * author: zengfansheng
 */
public class MethodInfo {
    public @NonNull String name;
    public @Nullable TypeValuePair[] pairs;

    public MethodInfo(@NonNull String name, @Nullable TypeValuePair... pairs) {
        this.name = name;
        this.pairs = pairs;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public TypeValuePair[] getPairs() {
        return pairs;
    }

    public void setPairs(@Nullable TypeValuePair[] pairs) {
        this.pairs = pairs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInfo)) return false;

        MethodInfo that = (MethodInfo) o;

        if (!name.equals(that.name)) return false;
        if (pairs != null) {
            if (((MethodInfo) o).pairs == null) return false;
            return Arrays.equals(pairs, that.pairs);
        } else return ((MethodInfo) o).pairs == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + Arrays.hashCode(pairs);
        return result;
    }
}
