package com.jun;

import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * mqtt connect 报文对象
 *
 * @author Jun
 * @since 1.0.0
 */
@Immutable
public class Connect {

    /**
     * 默认客户端心跳周期 60s
     */
    public static final int DEFAULT_KEEP_ALIVE = 60;

    public final String clientId;

    public final String username;

    public final byte[] password;

    public final boolean cleanSession;

    public final int keepAlive;

    public final boolean willFlag;

    public final byte[] willMessage;

    public final String willTopic;

    public final MqttQoS willQos;

    public final boolean willRetain;

    /**
     * 创建一个对象
     * <ol>
     *     <li>没有用户信息</li>
     *     <li>cleanSession = true</li>
     *     <li>心跳 一分钟</li>
     *     <li>无遗嘱消息</li>
     * </ol>
     *
     * @param clientId 客户端id，不允许为空
     */
    public Connect(String clientId) {
        this(clientId, null, null, true, DEFAULT_KEEP_ALIVE);
    }

    /**
     * 创建一个对象，该 {@code connect} 对象无遗嘱消息
     *
     * @param clientId     客户端id，不允许为空
     * @param username     用户名
     * @param password     密码
     * @param cleanSession 是否为 cleanSession 会话
     * @param keepAlive    心跳
     */
    public Connect(String clientId, String username, byte[] password, boolean cleanSession, int keepAlive) {
        this(clientId, username, password, cleanSession, keepAlive, false, null, null, null, false);
    }

    /**
     * create new Connect
     *
     * @param clientId     客户端id，不允许为空
     * @param username     用户名
     * @param password     密码
     * @param cleanSession 是否为 cleanSession 会话
     * @param keepAlive    心跳
     * @param willFlag     遗嘱消息 flag
     * @param willMessage  遗嘱消息内容
     * @param willTopic    遗嘱消息主题
     * @param willQos      遗嘱消息 qos
     * @param willRetain   遗嘱消息是否 retain
     */
    public Connect(String clientId, String username, byte[] password, boolean cleanSession, int keepAlive,
                   boolean willFlag, byte[] willMessage, String willTopic, MqttQoS willQos, boolean willRetain) {
        if (ObjectUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("clientId 不能为空");
        }

        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.cleanSession = cleanSession;
        this.keepAlive = keepAlive;
        this.willFlag = willFlag;
        this.willMessage = willMessage;
        this.willTopic = willTopic;
        this.willQos = willQos;
        this.willRetain = willRetain;
    }
}
