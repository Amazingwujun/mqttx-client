package com.jun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;

/**
 * mqtt 消息接收者，具体逻辑由用户实现。
 * <p>
 * 此接口的方法实现不应该存在阻塞等耗时的方法
 * </p>
 *
 * @author Jun
 * @since 1.0.0
 */
public interface MqttMessageReceiver {

    /**
     * 注入客户端.
     * <p>
     * 当 mqtt client 初始化时，将自身的引用注入到 mqttMessageReceiver. 见 {@link  MqttxClient#MqttxClient(MqttProperties, RemoteServerProperties)}
     * 构造逻辑
     * </p>
     *
     * @param client 客户端
     */
    void init(MqttxClient client);

    /**
     * 获得 receiver 对应的 {@link MqttxClient}
     *
     * @return mqtt 客户端
     */
    MqttxClient client();

    /**
     * 收到 broker pub 类型消息
     *
     * @param msg {@link MqttPublishMessage}
     */
    void onPub(MqttPublishMessage msg);

    /**
     * 订阅响应
     *
     * @param msg {@link MqttSubAckMessage}
     */
    void onSubAck(MqttSubAckMessage msg);

    /**
     * 取消订阅响应
     *
     * @param msg {@link MqttUnsubAckMessage}
     */
    void onUnsubAck(MqttUnsubAckMessage msg);

    /**
     * tcp 连接建立成功
     *
     * @param ctx {@link  ChannelHandlerContext}
     */
    void onChannelActive(ChannelHandlerContext ctx);

    /**
     * tcp 连接断开
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    void onChannelInactive(ChannelHandlerContext ctx);

    /**
     * mqtt conn 成功
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    void onConnSuccess(ChannelHandlerContext ctx);
}
