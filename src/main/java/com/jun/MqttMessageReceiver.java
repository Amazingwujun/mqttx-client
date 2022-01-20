package com.jun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

/**
 * mqtt 消息接收者，具体逻辑由用户实现。
 * <p>
 * 此接口的方法实现不应该<strong>阻塞操作或耗时的方法</strong>
 * </p>
 *
 * @author Jun
 * @since 1.0.0
 */
public interface MqttMessageReceiver {

    /**
     * 收到 conn 报文响应
     *
     * @param msg {@link  io.netty.handler.codec.mqtt.MqttMessage}
     */
    void onConnAck(ChannelHandlerContext ctx, MqttMessage msg);

    /**
     * 收到 broker pub 类型消息
     *
     * @param msg {@link MqttPublishMessage}
     */
    void onPub(ChannelHandlerContext ctx, MqttMessage msg);

    /**
     * 收到 {@link MqttMessageType#PUBACK}
     *
     * @param msg {@link MqttPubAckMessage}
     */
    void onPubAck(ChannelHandlerContext ctx, MqttMessage msg);

    void onPubRec(ChannelHandlerContext ctx, MqttMessage msg);

    void onPubRel(ChannelHandlerContext ctx, MqttMessage msg);

    void onPubComp(ChannelHandlerContext ctx, MqttMessage msg);

    /**
     * 订阅响应
     *
     * @param msg {@link MqttSubAckMessage}
     */
    void onSubAck(ChannelHandlerContext ctx, MqttMessage msg);

    /**
     * 取消订阅响应
     *
     * @param msg {@link MqttUnsubAckMessage}
     */
    void onUnsubAck(ChannelHandlerContext ctx, MqttMessage msg);

    /**
     * 心跳响应包
     *
     * @param ctx {@link  ChannelHandlerContext}
     * @param msg {@link }
     */
    void onPingResp(ChannelHandlerContext ctx, MqttMessage msg);

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
}
