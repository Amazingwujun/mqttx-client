package com.jun;

import java.time.Duration;

/**
 * mqtt 相关配置项
 *
 * @author Jun
 * @since 1.0.0
 */
@Immutable
public class MqttProperties {

    public final String clientId;

    public final String username;

    public final String password;

    public final boolean cleanSession;

    public final Duration heartbeatInterval;

    public final MqttMessageReceiver mqttMessageReceiver;

    /**
     * 新建配置项实例
     *
     * @param clientId            客户端id
     * @param username            用户名
     * @param password            密码
     * @param cleanSession        见协议说明 3.1.2.4 Clean Session.
     * @param heartbeatInterval   心跳间隔
     * @param mqttMessageReceiver {@link MqttMessageReceiver}
     */
    public MqttProperties(String clientId, String username, String password,
                          boolean cleanSession, Duration heartbeatInterval,
                          MqttMessageReceiver mqttMessageReceiver) {
        if (clientId == null || clientId.trim().equals("")) {
            throw new IllegalArgumentException("clientId 不能为空");
        }
        if (heartbeatInterval == null) {
            throw new IllegalArgumentException("heartbeatInterval can't be null");
        }
        if (mqttMessageReceiver == null) {
            throw new IllegalArgumentException("mqttMessageReceiver can't be null");
        }

        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.cleanSession = cleanSession;
        this.heartbeatInterval = heartbeatInterval;
        this.mqttMessageReceiver = mqttMessageReceiver;
    }

    public static MqttPropertiesBuilder builder() {
        return new MqttPropertiesBuilder();
    }

    public static final class MqttPropertiesBuilder {
        private String clientId;
        private String username;
        private String password;
        /**
         * cleanSession 默认为 true
         */
        private boolean cleanSession = true;
        private Duration heartbeatInterval;
        private MqttMessageReceiver mqttMessageReceiver;

        private MqttPropertiesBuilder() {
        }

        public MqttPropertiesBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public MqttPropertiesBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public MqttPropertiesBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public MqttPropertiesBuilder withCleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public MqttPropertiesBuilder withHeartbeatInterval(Duration heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public MqttPropertiesBuilder withMqttMessageReceiver(MqttMessageReceiver mqttMessageReceiver) {
            this.mqttMessageReceiver = mqttMessageReceiver;
            return this;
        }

        public MqttProperties build() {
            return new MqttProperties(clientId, username, password, cleanSession, heartbeatInterval, mqttMessageReceiver);
        }
    }
}
