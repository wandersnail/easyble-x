package com.wandersnail.bledemo;

import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.snail.commons.observer.Observe;
import com.snail.commons.util.ToastUtils;
import easyble2.EasyBLE;

/**
 * date: 2019/8/11 09:42
 * author: zengfansheng
 */
public class BaseActivity extends AppCompatActivity implements MyObserver {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasyBLE.getInstance().registerObserver(this);
    }

    @Observe
    @Override
    public void testObserver(MyEvent event) {
        ToastUtils.showShort(event.msg);
        Log.d("EasyBLE", event.msg + ", 主线程: " + (Looper.myLooper() == Looper.getMainLooper()));
    }

    @Override
    protected void onDestroy() {
        EasyBLE.getInstance().unregisterObserver(this);
        super.onDestroy();
    }
}
