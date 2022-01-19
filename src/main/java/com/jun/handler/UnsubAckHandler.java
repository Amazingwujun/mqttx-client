package com.jun.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link MqttMessageType#UNSUBACK} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.UNSUBACK)
public class UnsubAckHandler implements IMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(UnsubAckHandler.class);

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        log.info("unsub msg : {}", msg);
    }
}
