package com.jun;

import com.jun.entity.PubMsg;
import com.jun.entity.TopicSub;
import com.jun.handler.MqttMessageHandler;
import com.jun.service.ISessionService;
import com.jun.service.InMemorySessionServiceImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h1>mqttx 客户端</h1>
 *
 * @author Jun
 * @since 1.0.0
 */
public class MqttxClient {

    private static final Logger log = LoggerFactory.getLogger(MqttxClient.class);
    private static final EventLoopGroup WORKER;
    private static final ScheduledExecutorService MONITOR = Executors.newSingleThreadScheduledExecutor();

    static {
        if (Epoll.isAvailable()) {
            WORKER = new EpollEventLoopGroup();
        } else {
            WORKER = new NioEventLoopGroup();
        }
    }

    private final ISessionService sessionService;
    public final MqttProperties mqttProperties;
    public final RemoteServerProperties serverProperties;
    private Channel channel;
    private int messageIdGenerator = 0;

    public MqttxClient(MqttProperties mqttProperties, RemoteServerProperties serverProperties) {
        if (mqttProperties == null || serverProperties == null) {
            throw new IllegalArgumentException("mqttProperties and ServerProperties 必须同时存在");
        }

        this.mqttProperties = mqttProperties;
        this.serverProperties = serverProperties;

        // 将client注入到 MqttMessageReceiver
        mqttProperties.mqttMessageReceiver.init(this);
        this.sessionService = InMemorySessionServiceImpl.instance();
    }

    /**
     * 发送 mqtt 消息
     *
     * @param topic    消息主题
     * @param payload  消息内容
     * @param qoS      消息的 qos 级别，不支持 {@link MqttQoS#EXACTLY_ONCE}
     * @param retained 是否为保留消息
     * @return true if send
     */
    public boolean publish(String topic, byte[] payload, MqttQoS qoS, boolean retained) {
        if (topic == null) {
            throw new IllegalArgumentException("topic 不能为空");
        }
        if (qoS == null || qoS == MqttQoS.EXACTLY_ONCE) {
            throw new IllegalArgumentException("非法的 qos");
        }
        if (channel == null || !channel.isActive()) {
            return false;
        }

        // 构建 publish msg
        MqttPublishMessage mpm;
        if (qoS == MqttQoS.AT_MOST_ONCE) {
            mpm = MqttMessageBuilders.publish()
                    .topicName(topic)
                    .payload(Unpooled.wrappedBuffer(payload))
                    .qos(qoS)
                    .retained(retained)
                    .build();
        } else {
            // 只可能是 AT_LEAST_ONCE(1),
            final int messageId = nextMessageId();
            mpm = MqttMessageBuilders.publish()
                    .topicName(topic)
                    .payload(Unpooled.wrappedBuffer(payload))
                    .qos(qoS)
                    .messageId(messageId)
                    .retained(retained)
                    .build();

            // 保存 pub 消息
            sessionService.savePubMsg(serverProperties.socketAddress, new PubMsg(topic, qoS, retained, payload, messageId));
        }

        channel.writeAndFlush(mpm);
        return true;
    }

    /**
     * 发送 mqtt 消息，retained = false
     *
     * @param topic   消息主题
     * @param payload 消息内容
     * @param qoS     消息的 qos 级别，不支持 {@link MqttQoS#EXACTLY_ONCE}
     * @return true if send
     */
    public boolean publish(String topic, byte[] payload, MqttQoS qoS) {
        return publish(topic, payload, qoS, false);
    }

    /**
     * 发送订阅请求
     *
     * @param topicSubs 订阅参数
     * @return true if send
     */
    public boolean subscribe(List<TopicSub> topicSubs) {
        if (topicSubs == null || topicSubs.isEmpty()) {
            throw new IllegalArgumentException("订阅列表不能为空");
        }
        if (channel == null || !channel.isActive()) {
            return false;
        }


        // 构建订阅信息对象
        MqttMessageBuilders.SubscribeBuilder subscribeBuilder = MqttMessageBuilders.subscribe();
        topicSubs.forEach(topicSub -> subscribeBuilder.addSubscription(topicSub.qoS, topicSub.topic));
        MqttSubscribeMessage msm = subscribeBuilder.messageId(nextMessageId()).build();

        channel.writeAndFlush(msm);
        return true;
    }

    /**
     * 发送解除订阅请求
     *
     * @param unsubTopics 解除订阅的主题
     */
    public boolean unsubscribe(List<String> unsubTopics) {
        if (unsubTopics == null || unsubTopics.isEmpty()) {
            throw new IllegalArgumentException("订阅列表不能为空");
        }
        if (channel == null || !channel.isActive()) {
            return false;
        }

        // 构建删除订阅对象
        MqttMessageBuilders.UnsubscribeBuilder unsubscribeBuilder = MqttMessageBuilders.unsubscribe().messageId(nextMessageId());
        unsubTopics.forEach(unsubscribeBuilder::addTopicFilter);
        MqttUnsubscribeMessage mum = unsubscribeBuilder.build();

        channel.writeAndFlush(mum);
        return true;
    }

    /**
     * 异步启动客户端.
     * </p>
     * 当 {@link  RemoteServerProperties#needReconnect()} 为 true 时且启动异常时，该方法会不断尝试重连.
     */
    public void startWithAsync() {
        if (channel != null && channel.isActive()) {
            return;
        }
        try {
            start0(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    // todo ssl 支持
                    p
                            .addLast(new IdleStateHandler(0, 0, (int) mqttProperties.heartbeatInterval.getSeconds()))
                            .addLast(MqttEncoder.INSTANCE)
                            .addLast(new MqttDecoder())
                            .addLast(new MqttMessageHandler(mqttProperties, sessionService));
                }
            });
        } catch (Exception e) {
            log.error("MqttxClient 启动失败", e);
            restart();
        }
    }

    /**
     * 启动客户端，该方法阻塞至与服务端成功建立 tcp 连接.
     */
    public void startWithBlock() throws InterruptedException {
        if (channel != null && channel.isActive()) {
            return;
        }
        final CountDownLatch waiter = new CountDownLatch(1);
        try {
            start0(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    // todo ssl 支持
                    p
                            .addLast(new ChannelInboundHandlerAdapter() {

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    super.channelActive(ctx);
                                    waiter.countDown();
                                }
                            })
                            .addLast(new IdleStateHandler(0, 0, (int) mqttProperties.heartbeatInterval.getSeconds()))
                            .addLast(MqttEncoder.INSTANCE)
                            .addLast(new MqttDecoder())
                            .addLast(new MqttMessageHandler(mqttProperties, sessionService));
                }
            });
        } catch (Exception e) {
            log.error("MqttxClient 启动失败", e);
            restart();
        }
        waiter.await();
    }

    /**
     * 重新启动
     */
    public void restart() {
        if (serverProperties.needReconnect()) {
            log.info("{}秒后尝试重新连接 [{}:{}]", serverProperties.reconnectInterval,
                    serverProperties.socketAddress.getHostName(),
                    serverProperties.socketAddress.getPort());

            // 重新连接
            schedule(this::startWithAsync, serverProperties.reconnectInterval.getSeconds());
        }
    }

    /**
     * 获取消息 id
     */
    public synchronized int nextMessageId() {
        int id = ++messageIdGenerator;
        if ((id & 0xffff) == 0) {
            return ++messageIdGenerator;
        }
        return id;
    }

    /**
     * 启动客户端，并给 channel 赋值
     *
     * @param channelInitializer 用于初始化 channel，主要是 {@link ChannelHandler}
     * @throws InterruptedException 见 {@link InterruptedException}
     */
    private void start0(ChannelInitializer<SocketChannel> channelInitializer) throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b
                .group(WORKER)
                .channel(channel())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) serverProperties.connectTimeout.toMillis())
                .handler(channelInitializer);
        channel = b.connect(serverProperties.socketAddress).sync().channel();
    }

    /**
     * 获取 {@link SocketChannel}
     *
     * @return if system support epoll then return {@link EpollSocketChannel} otherwise return {@link NioSocketChannel}
     */
    private Class<? extends Channel> channel() {
        return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

    /**
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     */
    private void schedule(Runnable command, long delay) {
        MONITOR.schedule(command, delay, TimeUnit.SECONDS);
    }

}
