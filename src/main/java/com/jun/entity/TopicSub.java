package com.jun.entity;

import com.jun.Immutable;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * 主题订阅对象
 *
 * @author Jun
 * @since 1.0.0
 */
@Immutable
public class TopicSub {

    public final String topic;
    public final MqttQoS qoS;

    public TopicSub(String topic, MqttQoS qoS) {
        if (topic == null || topic.trim().equals("")) {
            throw new IllegalArgumentException("非法的 topic");
        }
        if (qoS == null || qoS == MqttQoS.EXACTLY_ONCE) {
            throw new IllegalArgumentException("非法的 qos");
        }

        this.topic = topic;
        this.qoS = qoS;
    }
}
