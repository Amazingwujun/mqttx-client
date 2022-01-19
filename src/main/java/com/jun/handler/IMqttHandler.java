package com.jun.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;

/**
 * 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
public interface IMqttHandler {

    /**
     * 处理方法
     *
     * @param ctx 见 {@link ChannelHandlerContext}
     * @param msg 解包后的数据
     */
    void process(ChannelHandlerContext ctx, MqttMessage msg);
}
