package com.jun.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MqttMessageType#PUBREC} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.PUBREC)
public class PubRecHandler implements IMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(PubRecHandler.class);

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        log.warn("pub receive msg : {}", msg);
    }
}
