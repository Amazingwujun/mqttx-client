package com.jun.handler;

import com.jun.MqttMessageReceiver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MqttMessageType#PUBLISH} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.PUBLISH)
public class PublishHandler implements IMqttHandler {

    private final MqttMessageReceiver mqttMessageReceiver;

    public PublishHandler(MqttMessageReceiver mqttMessageReceiver) {
        this.mqttMessageReceiver = mqttMessageReceiver;
    }

    /**
     * publish 消息处理.
     * <p>
     * 当前版本不支持处理 {@link MqttQoS#EXACTLY_ONCE} 级别的消息
     * </p>
     *
     * @param ctx 见 {@link ChannelHandlerContext}
     * @param msg 解包后的数据
     */
    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
        MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
        MqttPublishVariableHeader mqttPublishVariableHeader = (MqttPublishVariableHeader) msg.variableHeader();

        // 获取qos、topic、packetId、retain、payload
        MqttQoS mqttQoS = fixedHeader.qosLevel();
        int packetId = mqttPublishVariableHeader.packetId();

        // 消息处理
        switch (mqttQoS) {
            case AT_MOST_ONCE: {
                mqttMessageReceiver.onPub(publishMessage);
                break;
            }
            case AT_LEAST_ONCE: {
                mqttMessageReceiver.onPub(publishMessage);
                MqttMessage pubAck = MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttMessageIdVariableHeader.from(packetId),
                        null
                );
                ctx.writeAndFlush(pubAck);
                break;
            }
            case EXACTLY_ONCE: {
                throw new  UnsupportedOperationException("不支持 qos2");
            }
        }
    }
}
