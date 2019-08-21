package cn.wandersnail.ble;

import androidx.annotation.NonNull;

/**
 * 配对控制器
 * <p>
 * date: 2019/8/3 12:59
 * author: zengfansheng
 */
public interface BondController {
    /**
     * 配对控制
     * 
     * @param device 设备
     */
    boolean accept(@NonNull Device device);
}
