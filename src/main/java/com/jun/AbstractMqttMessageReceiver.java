package com.jun;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * 提供基础逻辑支持.
 * 主要包括
 * <ol>
 *     <li>定时心跳发送</li>
 *     <li>qos1 支持</li>
 *     <li>掉线重连，见 {@link this#onChannelInactive(ChannelHandlerContext)}</li>
 * </ol>
 *
 * @author Jun
 * @since 1.0.0
 */
public abstract class AbstractMqttMessageReceiver implements MqttMessageReceiver {

    private static final Logger log = LoggerFactory.getLogger(AbstractMqttMessageReceiver.class);
    // 1100|0000
    private static final MqttMessage heartbeatMessage = MqttMessageFactory.newMessage(
            new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0),
            null, null);
    private final IQosService qosService;
    private final Connect connect;

    /**
     * @param qosService 报文状态服务
     * @param connect    connect 配置
     */
    public AbstractMqttMessageReceiver(IQosService qosService, Connect connect) {
        if (qosService == null) {
            throw new IllegalArgumentException("qosService 不能为空");
        }
        if (connect == null) {
            throw new IllegalArgumentException("connect 不能为空");
        }

        this.qosService = qosService;
        this.connect = connect;
    }


    @Override
    public void onConnAck(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttConnAckMessage connAckMessage = (MqttConnAckMessage) msg;
        MqttConnectReturnCode returnCode = connAckMessage.variableHeader().connectReturnCode();

        // 登入结果判断
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String host = socketAddress.getAddress().getHostAddress();
        int port = socketAddress.getPort();
        try {
            if (returnCode == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                log.info("登入 mqtt broker [{}:{}] 成功", host, port);

                // 启动心跳
                startHeartbeat(ctx);
            } else {
                log.warn("登入 mqtt broker [{}:{}] 失败, 响应码: [{}]", host, port, returnCode.name());
            }
            // 完成 future
            session(ctx).connectFuture.complete(returnCode);
        } catch (Throwable e) {
            session(ctx).connectFuture.completeExceptionally(e);
            throw e;
        }
    }

    @Override
    public void onPub(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
        MqttFixedHeader fixedHeader = publishMessage.fixedHeader();
        MqttPublishVariableHeader mqttPublishVariableHeader = (MqttPublishVariableHeader) msg.variableHeader();

        // 获取 qos、topic、packetId、retain、payload
        MqttQoS mqttQoS = fixedHeader.qosLevel();
        int packetId = mqttPublishVariableHeader.packetId();

        // 消息处理
        switch (mqttQoS) {
            case AT_MOST_ONCE: {
                // 交付消息
                onPub0(msg);
                break;
            }
            case AT_LEAST_ONCE: {
                MqttMessage pubAck = MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttMessageIdVariableHeader.from(packetId),
                        null
                );
                ctx.writeAndFlush(pubAck);

                // 交付消息
                onPub0(msg);
                break;
            }
            case EXACTLY_ONCE: {
                throw new UnsupportedOperationException("客户端不支持 qos2");
            }
        }
    }

    /**
     * 子类实现此方法以便处理 pub 消息
     *
     * @param msg 报文
     */
    abstract protected void onPub0(MqttMessage msg);

    @Override
    public void onPubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttPubAckMessage mpam = (MqttPubAckMessage) msg;
        final int messageId = mpam.variableHeader().messageId();

        // 清理 msg
        qosService.clear((InetSocketAddress) ctx.channel().remoteAddress(), messageId);
    }

    @Override
    public void onPubRec(ChannelHandlerContext ctx, MqttMessage msg) {
        throw new UnsupportedOperationException("不支持 qos2");
    }

    @Override
    public void onPubRel(ChannelHandlerContext ctx, MqttMessage msg) {
        throw new UnsupportedOperationException("不支持 qos2");
    }

    @Override
    public void onPubComp(ChannelHandlerContext ctx, MqttMessage msg) {
        throw new UnsupportedOperationException("不支持 qos2");
    }


    @Override
    public void onPingResp(ChannelHandlerContext ctx, MqttMessage msg) {
        if (log.isDebugEnabled()) {
            log.debug("ping ack: {}", msg);
        }
    }

    /**
     * tcp 连接建立成功，发起 mqtt conn 报文
     *
     * @param ctx {@link  ChannelHandlerContext}
     */
    @Override
    public void onChannelActive(ChannelHandlerContext ctx) {
        // 心跳校验 handler 加入
        ctx.pipeline().addFirst(new IdleStateHandler(0, 0, (int) (connect.keepAlive * 1.5)));

        // 构建并发送 mqtt conn 报文
        MqttMessageBuilders.ConnectBuilder connectBuilder = MqttMessageBuilders.connect()
                .clientId(connect.clientId)
                .username(connect.username)
                .password(connect.password)
                .cleanSession(connect.cleanSession)
                .keepAlive(connect.keepAlive);
        if (connect.willFlag) {
            connectBuilder
                    .willFlag(true)
                    .willMessage(connect.willMessage)
                    .willTopic(connect.willTopic)
                    .willQoS(connect.willQos)
                    .willRetain(connect.willRetain);
        }

        ctx.writeAndFlush(connectBuilder.build());
    }

    /**
     * 连接被断开，执行重连逻辑
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    @Override
    public void onChannelInactive(ChannelHandlerContext ctx) {
        client(ctx).start();
    }

    /**
     * 通过 {@link io.netty.channel.Channel} 获取关联 {@link Session}
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    private Session session(ChannelHandlerContext ctx) {
        return (Session) ctx.channel().attr(AttributeKey.valueOf(Session.ATTR_KEY)).get();
    }

    /**
     * 通过 {@link io.netty.channel.Channel} 获取关联 {@code MqttxClient}
     *
     * @param ctx {@link ChannelHandlerContext}
     */
    private MqttxClient client(ChannelHandlerContext ctx) {
        return session(ctx).mqttxClient;
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
            qosService.findByRemoteServerProperties((InetSocketAddress) ctx.channel().remoteAddress())
                    .stream()
                    .map(PubMsg::toMqttPublishMessage)
                    .forEach(ctx::writeAndFlush);
        }, 0, connect.keepAlive, TimeUnit.SECONDS);
    }
}
