package com.jun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * mqtt 报文处理器
 *
 * @author Jun
 * @since 1.0.0
 */
public final class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    public static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);
    private final MqttMessageReceiver messageReceiver;

    public MqttMessageHandler(MqttMessageReceiver mqttMessageReceiver) {
        if (mqttMessageReceiver == null) {
            throw new IllegalArgumentException("mqttMessageReceiver 不允许为 null");
        }

        this.messageReceiver = mqttMessageReceiver;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        // 解码异常处理
        if (msg.decoderResult().isFailure()) {
            exceptionCaught(ctx, msg.decoderResult().cause());
            return;
        }

        final MqttMessageType messageType = msg.fixedHeader().messageType();
        switch (messageType) {
            case CONNACK:
                messageReceiver.onConnAck(ctx, msg);
                break;
            case PUBLISH:
                messageReceiver.onPub(ctx, msg);
                break;
            case PUBACK:
                messageReceiver.onPubAck(ctx, msg);
                break;
            case PUBREC:
                messageReceiver.onPubRec(ctx, msg);
                break;
            case PUBREL:
                messageReceiver.onPubRel(ctx, msg);
                break;
            case PUBCOMP:
                messageReceiver.onPubComp(ctx, msg);
                break;
            case SUBACK:
                messageReceiver.onSubAck(ctx, msg);
                break;
            case UNSUBACK:
                messageReceiver.onUnsubAck(ctx, msg);
                break;
            case PINGRESP:
                messageReceiver.onPingResp(ctx, msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 打印日志
        log.info("完成与 mqtt broker[{}] 的 tcp 握手", brokerInfo(ctx));

        messageReceiver.onChannelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        messageReceiver.onChannelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            if (IdleState.ALL_IDLE == ((IdleStateEvent) evt).state()) {
                log.warn("mqtt broker[{}] 心跳超时", brokerInfo(ctx));
                ctx.close();
            }
        }
    }

    /**
     * 异常捕获
     *
     * @param ctx   {@link ChannelHandlerContext}
     * @param cause 异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(String.format("发生异常:[%s]，关闭连接", cause.getMessage()), cause);

        ctx.close();
    }

    private String brokerInfo(ChannelHandlerContext ctx) {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        return String.format("%s:%s", host, port);
    }
}
