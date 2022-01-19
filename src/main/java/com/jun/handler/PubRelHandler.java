package com.jun.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MqttMessageType#PUBREL} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.PUBREL)
public class PubRelHandler implements IMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(PubRelHandler.class);

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        log.warn("pub release msg : {}", msg);
    }
}
