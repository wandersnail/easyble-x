package easyble2;

/**
 * 指定执行线程
 * <p>
 * date: 2019/8/2 23:53
 * author: zengfansheng
 */
public enum ThreadMode {
    /**
     * 和调用者同一线程
     */
    POSTING,
    /**
     * 主线程，UI线程
     */
    MAIN,
    /**
     * 后台线程
     */
    BACKGROUND
}
