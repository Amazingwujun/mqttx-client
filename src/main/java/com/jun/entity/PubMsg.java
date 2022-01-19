package com.jun.entity;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;

import java.util.Objects;

/**
 * publish  消息
 *
 * @author Jun
 * @since 1.0.0
 */
public class PubMsg {

    public final String topic;
    public final MqttQoS qoS;
    public final boolean retained;
    public final byte[] payload;
    public final int messageId;

    public PubMsg(String topic, MqttQoS qoS, boolean retained, byte[] payload, int messageId) {
        this.topic = topic;
        this.qoS = qoS;
        this.retained = retained;
        this.payload = payload;
        this.messageId = messageId;
    }

    /**
     * 转为 {@link  MqttPublishMessage}
     */
    public MqttPublishMessage toMqttPublishMessage(){
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, true, qoS, retained, 0),
                new MqttPublishVariableHeader(topic, messageId),
                Unpooled.wrappedBuffer(payload)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PubMsg pubMsg = (PubMsg) o;
        return messageId == pubMsg.messageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
