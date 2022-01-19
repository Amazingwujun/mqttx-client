package com.jun.handler;

import com.jun.MqttProperties;
import com.jun.service.ISessionService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * mqtt 报文处理器
 *
 * @author Jun
 * @since 1.0.0
 */
public class MqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

    public static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);
    private final MqttProperties mqttProperties;
    private final MqttMessageDelegatingHandler mqttMessageDelegatingHandler;

    public MqttMessageHandler(MqttProperties mqttProperties, ISessionService sessionService) {
        this.mqttProperties = mqttProperties;
        this.mqttMessageDelegatingHandler = new MqttMessageDelegatingHandler(mqttProperties, sessionService);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) {
        // 解码异常处理
        if (msg.decoderResult().isFailure()) {
            exceptionCaught(ctx, msg.decoderResult().cause());
            return;
        }

        // 消息处理
        mqttMessageDelegatingHandler.handle(ctx, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 打印日志
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        log.info("完成与 mqtt broker[{}:{}] 的 tcp 握手", host, port);

        mqttProperties.mqttMessageReceiver.onChannelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        mqttProperties.mqttMessageReceiver.onChannelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        if (evt instanceof IdleStateEvent) {
            if (IdleState.ALL_IDLE == ((IdleStateEvent) evt).state()) {
                log.info("mqtt broker[{}:{}] 心跳超时", host, port);
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
}
