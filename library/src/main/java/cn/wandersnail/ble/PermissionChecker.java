package cn.wandersnail.ble;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import cn.wandersnail.commons.base.AppHolder;

/**
 * 权限检查
 * 
 * date: 2021/11/22 15:58
 * author: zengfansheng
 */
class PermissionChecker {
    static boolean hasPermission(@Nullable Context context, @NonNull String permission) {
        Activity activity = context instanceof Activity ? (Activity) context : AppHolder.getInstance().getTopActivity();
        context = context == null ? EasyBLE.instance.getContext() : context;
        if (activity == null) {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        }
    }
}
