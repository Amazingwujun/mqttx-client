package com.jun;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 代表当前 mqtt 会话.
 * <p> tcp 连接建立时，session 就会创建.
 *
 * @author Jun
 * @since 1.0.0
 */
public class Session {

    public static final String ATTR_KEY = "session";

    public final MqttxClient mqttxClient;

    /**
     * 每当 {@link MqttxClient} 尝试连接 broker 时，都会关联一个新的 {@code Session} 对象，而 {@code connectFuture} 则代表 conn
     * 报文最终响应结果.
     */
    public final CompletableFuture<MqttConnectReturnCode> connectFuture = new CompletableFuture<>();

    /**
     * 构建一个新的会话
     *
     * @param mqttxClient 当前会话关联的客户端
     */
    public Session(MqttxClient mqttxClient) {
        this.mqttxClient = mqttxClient;
    }

    /**
     * 检查当前会话是否有效.
     * <p> 当前会话与当前客户端的 {@link MqttxClient#nettyChannel()} 关联的 session 相等（指针地址相同）,则表明当前会话
     * 为有效会话.
     *
     * @return true if current session is valid
     */
    public boolean isSessionValid() {
        Session sessionWithChannel = (Session) Optional.of(mqttxClient)
                .map(MqttxClient::nettyChannel)
                .map(channel -> channel.attr(AttributeKey.valueOf(ATTR_KEY)))
                .map(Attribute::get)
                .orElse(null);
        return this == sessionWithChannel;
    }

    /**
     * 与 broker conn 是否成功, 1：tcp 连接建立成功, 2: conn 报文 broker 响应成功.
     * <p>注意：该方法会阻塞当前线程，直至连接成功.
     */
    public boolean isConnected() throws ExecutionException, InterruptedException {
        return connectFuture.get() == MqttConnectReturnCode.CONNECTION_ACCEPTED;
    }
}
