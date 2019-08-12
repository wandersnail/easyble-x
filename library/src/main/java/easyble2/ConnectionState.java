package easyble2;

/**
 * 连接状态
 * <p>
 * date: 2019/8/12 14:26
 * author: zengfansheng
 */
public enum ConnectionState {
    /**
     * 已断开连接
     */
    DISCONNECTED,
    /**
     * 正在连接
     */
    CONNECTING,
    /**
     * 正在搜索重连
     */
    SCANNING_FOR_RECONNECTION,
    /**
     * 已连接，还未执行发现服务
     */
    CONNECTED,
    /**
     * 已连接，正在发现服务
     */
    SERVICE_DISCOVERING,
    /**
     * 已连接，成功发现服务
     */
    SERVICE_DISCOVERED,
    /**
     * 连接已释放
     */
    RELEASED
}
