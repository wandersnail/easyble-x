package easyble2;

/**
 * date: 2019/8/5 16:10
 * author: zengfansheng
 */
class Inspector {
    /**
     * 对象为空时抛EasyBLEException
     *
     * @param obj 要检查的对象
     * @param message 异常概要消息
     */
    static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new EasyBLEException(message);
        return obj;
    }
}
