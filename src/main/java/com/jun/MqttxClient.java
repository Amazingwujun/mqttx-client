package com.jun;

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
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <h1>mqttx 客户端</h1>
 * <p>使用方式:
 * <pre>
 *         // qos1 消息状态缓存服务
 *         IQosService qosService = InMemoryQosServiceImpl.instance();
 *
 *         // conn 报文内容
 *         Connect connect = new Connect("mqttx-client");
 *         // 可以很复杂
 *         // connect = new Connect("mqttx-client", "uname", "passwd".getBytes(), false, 60,
 *         //        true, "nani".getBytes(), "willTopic", MqttQoS.AT_LEAST_ONCE, true);
 *
 *         // MqttMessageReceiver 用于处理客户端收到的消息
 *         // AbstractMqttMessageReceiver: 实现了 MqttMessageReceiver, 提供：
 *         // 1. 自动 connect
 *         // 2. 短线重连
 *         // 3. 心跳
 *         // 等机制.
 *         MqttMessageReceiver receiver = new OnlyPrintMqttMessageReceiver(qosService, connect);
 *
 *         // mqtt broker 配置
 *         RemoteServerProperties remoteServerProperties = new RemoteServerProperties("192.168.32.35", 1883, Duration.ofSeconds(3), Duration.ofSeconds(5));
 *
 *         // 构建 mqtt client 并获取 session
 *         Session session = new MqttxClient(receiver, InMemoryQosServiceImpl.instance(), remoteServerProperties)
 *                 .start();
 *         // 阻塞至收到 broker 对 conn 报文的回复( connAck )
 *         try {
 *             session.connectFuture.get();
 *         } catch (InterruptedException | ExecutionException e) {
 *             e.printStackTrace();
 *         }
 * </pre>
 * 需要注意的点：
 * <ol>
 *     <li>当前版本不支持 qos2(会报错)</li>
 *     <li>对 qos1 消息保存默认实现为: {@link InMemoryQosServiceImpl}，实现基于内存，应用重启或崩溃会导致消息丢失.</li>
 *     <li>客户端支持断线重连, 重连配置见 {@link  RemoteServerProperties}</li>
 * </ol>
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

    public final RemoteServerProperties serverProperties;
    private final MqttMessageReceiver messageReceiver;
    private final IQosService sessionService;
    private Channel channel;
    private int messageIdGenerator = 0;

    public MqttxClient(MqttMessageReceiver mqttMessageReceiver, IQosService sessionService, RemoteServerProperties serverProperties) {
        if (mqttMessageReceiver == null) {
            throw new IllegalArgumentException("mqttMessageReceiver 不能为空");
        }

        this.messageReceiver = mqttMessageReceiver;
        this.serverProperties = serverProperties;
        this.sessionService = sessionService;
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
        if (ObjectUtils.isEmpty(topic)) {
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
            // 只可能是 AT_LEAST_ONCE(1)
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
     * 启动客户端.
     * </p>
     * 当 {@link  RemoteServerProperties#needReconnect()} 为 true 时且启动异常时，该方法会不断尝试重连.
     *
     * @return session，返回当前客户端关联的会话
     */
    public Session start() {
        if (channel != null && channel.isActive()) {
            return (Session) channel.attr(AttributeKey.valueOf(Session.ATTR_KEY)).get();
        }
        try {
            return start0(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    // todo ssl 支持
                    p
                            .addLast(MqttEncoder.INSTANCE)
                            .addLast(new MqttDecoder())
                            .addLast(new MqttMessageHandler(messageReceiver));
                }
            });
        } catch (Exception e) {
            throw new MqttxException("mqttx-client 启动异常: %s", e.getMessage());
        }
    }


    /**
     * 重新启动.
     * <p>
     * 注意：重新启动会导致 {@link Session} 被更新, 可通过 {@link Session#isSessionValid()} 方法判断.
     * </p>
     */
    public void restart() {
        if (serverProperties.needReconnect()) {
            log.info("{}秒后尝试重新连接 [{}:{}]", serverProperties.reconnectInterval,
                    serverProperties.socketAddress.getHostName(),
                    serverProperties.socketAddress.getPort());

            // 重新连接
            schedule(this::start, serverProperties.reconnectInterval.getSeconds());
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
     * 返回客户端持有的 {@link Channel}
     */
    public Channel nettyChannel() {
        return channel;
    }

    /**
     * 启动客户端，并给 channel 赋值
     *
     * @param channelInitializer 用于初始化 channel，主要是 {@link ChannelHandler}
     * @throws InterruptedException 见 {@link InterruptedException}
     */
    private Session start0(ChannelInitializer<SocketChannel> channelInitializer) throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b
                .group(WORKER)
                .channel(channel())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) serverProperties.connectTimeout.toMillis())
                .handler(channelInitializer);
        channel = b.connect(serverProperties.socketAddress).sync().channel();

        // 构建 session
        Session session = new Session(this);
        channel.attr(AttributeKey.valueOf(Session.ATTR_KEY)).set(session);
        return session;
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
