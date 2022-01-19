package com.jun.handler;

import com.jun.service.ISessionService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;

import java.net.InetSocketAddress;

/**
 * {@link MqttMessageType#PUBACK} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.PUBACK)
public class PubAckHandler implements IMqttHandler {

    private final ISessionService sessionService;

    public PubAckHandler(ISessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttPubAckMessage mpam = (MqttPubAckMessage) msg;
        final int messageId = mpam.variableHeader().messageId();

        // 清理 msg
        sessionService.clear((InetSocketAddress) ctx.channel().remoteAddress(), messageId);
    }
}
