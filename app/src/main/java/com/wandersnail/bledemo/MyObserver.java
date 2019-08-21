package com.wandersnail.bledemo;

import cn.wandersnail.ble.EventObserver;

/**
 * date: 2019/8/11 00:32
 * author: zengfansheng
 */
public interface MyObserver extends EventObserver {
    default void testObserver(MyEvent event) {}
}
