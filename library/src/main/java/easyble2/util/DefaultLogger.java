package easyble2.util;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * date: 2019/8/3 16:24
 * author: zengfansheng
 */
public class DefaultLogger implements Logger {
    private final String tag;
    private boolean isEnabled;

    public DefaultLogger(@NonNull String tag) {
        this.tag = tag;
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public void log(int priority, int type, @NonNull String msg) {
        if (isEnabled) {
            Log.println(priority, tag, msg);
        }
    }

    @Override
    public void log(int priority, int type, @NonNull String msg, @NonNull Throwable th) {
        if (isEnabled) {
            log(priority, type, msg + "\n" + Log.getStackTraceString(th));
        }
    }
}
