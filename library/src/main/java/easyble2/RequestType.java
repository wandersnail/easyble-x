package easyble2;

/**
 * 请求类型
 * <p>
 * date: 2019/8/9 22:10
 * author: zengfansheng
 */
public enum RequestType {
    /**
     * 开关通知
     */
    SET_NOTIFICATION,
    /**
     * 开关Indication
     */
    SET_INDICATION,
    /**
     * 读特征值
     */
    READ_CHARACTERISTIC,
    /**
     * 读描述符
     */
    READ_DESCRIPTOR,
    /**
     * 读信号强度
     */
    READ_RSSI,
    /**
     * 写特征值
     */
    WRITE_CHARACTERISTIC,
    /**
     * 修改最大传输单元
     */
    CHANGE_MTU,
    /**
     * 读物物理层发送器和接收器
     */
    READ_PHY,
    /**
     * 设置物理层发送器和接收器偏好
     */
    SET_PREFERRED_PHY
}
