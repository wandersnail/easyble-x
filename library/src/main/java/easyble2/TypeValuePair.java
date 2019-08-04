package easyble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * date: 2019/8/3 09:21
 * author: zengfansheng
 */
public class TypeValuePair {
    public @Nullable Object value;
    public @NonNull Class<?> type;

    public TypeValuePair(@NonNull Class<?> type, @Nullable Object value) {
        this.type = type;
        this.value = value;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    @NonNull
    public Class<?> getType() {
        return type;
    }

    public void setType(@NonNull Class<?> type) {
        this.type = type;
    }
}
