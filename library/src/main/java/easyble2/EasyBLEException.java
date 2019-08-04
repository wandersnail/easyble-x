package easyble2;

/**
 * date: 2019/8/3 12:08
 * author: zengfansheng
 */
public class EasyBLEException extends RuntimeException {
    private static final long serialVersionUID = -7775315841108791634L;

    public EasyBLEException(String message) {
        super(message);
    }

    public EasyBLEException(String message, Throwable cause) {
        super(message, cause);
    }

    public EasyBLEException(Throwable cause) {
        super(cause);
    }
}
