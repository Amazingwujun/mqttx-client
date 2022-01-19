package com.jun.handler;

import com.jun.MqttMessageReceiver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * {@link MqttMessageType#SUBACK} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.SUBACK)
public class SubAckHandler implements IMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(SubAckHandler.class);
    private final MqttMessageReceiver mqttMessageReceiver;

    public SubAckHandler(MqttMessageReceiver mqttMessageReceiver) {
        this.mqttMessageReceiver = mqttMessageReceiver;
    }

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttSubAckMessage subAckMessage = (MqttSubAckMessage) msg;
        List<Integer> returnCodes = subAckMessage.payload().grantedQoSLevels();
        log.info("订阅响应: {}", returnCodes);

        mqttMessageReceiver.onSubAck(subAckMessage);
    }
}
