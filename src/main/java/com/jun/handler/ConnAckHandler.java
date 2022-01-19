package com.jun.handler;

import com.jun.MqttMessageReceiver;
import com.jun.entity.PubMsg;
import com.jun.service.ISessionService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * {@link MqttMessageType#CONNACK} 消息处理器
 *
 * @author Jun
 * @since 1.0.0
 */
@MqttHandler(type = MqttMessageType.CONNACK)
public class ConnAckHandler implements IMqttHandler {

    private static final Logger log = LoggerFactory.getLogger(ConnAckHandler.class);
    // 1100|0000
    private static final MqttMessage heartbeatMessage = MqttMessageFactory.newMessage(
            new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0),
            null, null);

    private final Duration heartbeatInterval;
    private final MqttMessageReceiver mqttMessageReceiver;
    private final ISessionService sessionService;

    public ConnAckHandler(Duration heartbeatInterval, MqttMessageReceiver mqttMessageReceiver, ISessionService sessionService) {
        this.heartbeatInterval = heartbeatInterval;
        this.mqttMessageReceiver = mqttMessageReceiver;
        this.sessionService = sessionService;
    }

    @Override
    public void process(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttConnAckMessage connAckMessage = (MqttConnAckMessage) msg;
        MqttConnectReturnCode returnCode = connAckMessage.variableHeader().connectReturnCode();

        // 登入结果判断
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        if (returnCode == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
            log.info("登入 mqtt broker [{}:{}] 成功", host, port);

            // 启动心跳
            startHeartbeat(ctx);

            mqttMessageReceiver.onConnSuccess(ctx);
        } else {
            log.error("登入 mqtt broker [{}:{}] 失败, 响应码: [{}]", host, port, returnCode.name());
        }
    }

    /**
     * 定时心跳除开执行发送心跳报文任务外，同时也可以 republish msg
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    private void startHeartbeat(ChannelHandlerContext ctx) {
        ctx.executor().scheduleAtFixedRate(() -> {
            final Channel channel = ctx.channel();
            if (channel != null && channel.isActive()) {
                ctx.writeAndFlush(heartbeatMessage);
            }

            // 补发信息
            sessionService.findByRemoteServerProperties((InetSocketAddress) ctx.channel().remoteAddress())
                    .stream()
                    .map(PubMsg::toMqttPublishMessage)
                    .forEach(ctx::writeAndFlush);
        }, 0, heartbeatInterval.getSeconds(), TimeUnit.SECONDS);
    }
}
