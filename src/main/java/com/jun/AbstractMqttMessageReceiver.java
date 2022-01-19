package com.jun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 提供基础方法支持.
 *
 * @author Jun
 * @since 1.0.0
 */
public abstract class AbstractMqttMessageReceiver implements MqttMessageReceiver {

    private MqttxClient mqttxClient;
    private volatile boolean isInit;

    @Override
    public void init(MqttxClient client) {
        if (isInit) {
            throw new UnsupportedOperationException("不支持再次初始化");
        }
        if (client == null) {
            throw new IllegalArgumentException("MqttxClient 不能为空");
        }
        this.mqttxClient = client;
        isInit = true;
    }

    @Override
    public MqttxClient client() {
        return mqttxClient;
    }

    /**
     * tcp 连接建立成功，发起 mqtt conn 报文
     *
     * @param ctx {@link  ChannelHandlerContext}
     */
    @Override
    public void onChannelActive(ChannelHandlerContext ctx) {
        // 这里检查一下初始化
        if (!isInit) {
            throw new IllegalStateException("MqttMessageReceiver 必须初始化!!!");
        }

        // 准备发出 mqtt conn 报文
        MqttProperties prop = mqttxClient.mqttProperties;
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
                .clientId(prop.clientId)
                .username(prop.username)
                .password(Optional.ofNullable(prop.password).map(e -> e.getBytes(StandardCharsets.UTF_8)).orElse(null))
                .cleanSession(prop.cleanSession)
                .keepAlive((int) prop.heartbeatInterval.getSeconds())
                .build();
        ctx.writeAndFlush(connectMessage);
    }

    /**
     * 连接被断开，执行重连逻辑
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    @Override
    public void onChannelInactive(ChannelHandlerContext ctx) {
        mqttxClient.restart();
    }
}
