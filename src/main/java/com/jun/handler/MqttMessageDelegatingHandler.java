package com.jun.handler;

import com.jun.MqttMessageReceiver;
import com.jun.MqttProperties;
import com.jun.service.ISessionService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
public class MqttMessageDelegatingHandler {

    /**
     * 客户端需要处理的消息类别共9种：
     * <ol>
     *     <li>{@link MqttMessageType#CONNACK}</li>
     *     <li>{@link MqttMessageType#PUBLISH}</li>
     *     <li>{@link MqttMessageType#PUBACK}</li>
     *     <li>{@link MqttMessageType#PUBREC}</li>
     *     <li>{@link MqttMessageType#PUBREL}</li>
     *     <li>{@link MqttMessageType#PUBCOMP}</li>
     *     <li>{@link MqttMessageType#SUBACK}</li>
     *     <li>{@link MqttMessageType#UNSUBACK}</li>
     *     <li>{@link MqttMessageType#PINGRESP}</li>
     * </ol>
     */
    private final Map<MqttMessageType, IMqttHandler> handlerMap = new HashMap<>(9);

    /**
     * 将处理器置入 {@link #handlerMap}
     */
    public MqttMessageDelegatingHandler(MqttProperties properties, ISessionService sessionService) {
        MqttMessageReceiver mqttMessageReceiver = properties.mqttMessageReceiver;

        handlerMap.put(MqttMessageType.CONNACK, new ConnAckHandler(properties.heartbeatInterval, mqttMessageReceiver, sessionService));
        handlerMap.put(MqttMessageType.PUBLISH, new PublishHandler(mqttMessageReceiver));
        handlerMap.put(MqttMessageType.PUBACK, new PubAckHandler(sessionService));
        handlerMap.put(MqttMessageType.PUBREC, new PubRecHandler());
        handlerMap.put(MqttMessageType.PUBREL, new PubRelHandler());
        handlerMap.put(MqttMessageType.PUBCOMP, new PubCompHandler());
        handlerMap.put(MqttMessageType.SUBACK, new SubAckHandler(mqttMessageReceiver));
        handlerMap.put(MqttMessageType.UNSUBACK, new UnsubAckHandler());
        handlerMap.put(MqttMessageType.PINGRESP, new PingAckHandler());
    }

    /**
     * 将消息委派给真正的 {@link MqttMessageHandler}
     *
     * @param ctx         {@link ChannelHandlerContext}
     * @param mqttMessage {@link MqttMessageType}
     */
    public void handle(ChannelHandlerContext ctx, MqttMessage mqttMessage) {
        MqttMessageType mqttMessageType = mqttMessage.fixedHeader().messageType();
        Optional.ofNullable(handlerMap.get(mqttMessageType))
                .ifPresent(mqttMessageHandler -> mqttMessageHandler.process(ctx, mqttMessage));
    }
}
