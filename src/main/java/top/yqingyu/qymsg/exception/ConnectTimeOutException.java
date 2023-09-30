package top.yqingyu.qymsg.exception;

import top.yqingyu.common.exception.QyRuntimeException;

public class ConnectTimeOutException extends QyRuntimeException {
    public ConnectTimeOutException() {
    }

    public ConnectTimeOutException(String message, Object... o) {
        super(message, o);
    }

    public ConnectTimeOutException(Throwable cause, String message, Object... o) {
        super(cause, message, o);
    }

    public ConnectTimeOutException(Throwable cause) {
        super(cause);
    }

    public ConnectTimeOutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Object... o) {
        super(message, cause, enableSuppression, writableStackTrace, o);
    }
}
