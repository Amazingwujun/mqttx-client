package com.jun;

/**
 * 项目异常
 *
 * @author Jun
 * @since 1.0.0
 */
public class MqttxException extends RuntimeException {

    public MqttxException() {
        super();
    }

    public MqttxException(String message) {
        super(message);
    }

    public MqttxException(String format, Object... args) {
        super(String.format(format, args));
    }

    public MqttxException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttxException(Throwable cause) {
        super(cause);
    }
}
