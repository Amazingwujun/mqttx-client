package com.jun;

import com.jun.entity.TopicSub;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AppTest {

    @Test
    public void shouldAnswerWithTrue() {
        // 生产条件下，用户自行实现
        MqttMessageReceiver mmr = new AbstractMqttMessageReceiver() {

            @Override
            public void onPub(MqttPublishMessage msg) {
                ByteBuf content = msg.payload();
                // 将消息打印出来
                System.out.println(content.toString(StandardCharsets.UTF_8));
            }

            @Override
            public void onSubAck(MqttSubAckMessage msg) {
                // todo 订阅响应处理
            }

            @Override
            public void onUnsubAck(MqttUnsubAckMessage msg) {
                // todo 删除订阅响应处理
            }

            @Override
            public void onConnSuccess(ChannelHandlerContext ctx) {
                // todo mqtt conn 登入成功响应处理
            }
        };

        MqttProperties properties = MqttProperties.builder()
                .withClientId("mqttx-client")
                .withUsername("username")
                .withPassword("password")
                .withCleanSession(true)
                // 心跳间隔
                .withHeartbeatInterval(Duration.ofMinutes(1))
                .withMqttMessageReceiver(mmr)
                .build();

        MqttxClient client = new MqttxClient(properties, new RemoteServerProperties("", 1773, Duration.ofSeconds(3), Duration.ofSeconds(5)));
        // 异步
        client.startWithAsync();
        // 同步
        try {
            // 阻塞至 tcp 连接建立成功
            client.startWithBlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MqttPublishMessage mpm = MqttMessageBuilders.publish()
                // 消息id, 推荐使用
                .messageId(1)
                .qos(MqttQoS.AT_LEAST_ONCE)
                .payload(Unpooled.wrappedBuffer("hello world".getBytes(StandardCharsets.UTF_8)))
                .topicName("test")
                .build();
        String topic = "test";
        byte[] paylaod = "hello".getBytes(StandardCharsets.UTF_8);
        MqttQoS qoS = MqttQoS.AT_LEAST_ONCE;
        client.publish(topic, paylaod, qoS);

        List<TopicSub> topicSubList = Stream.of(new TopicSub("test", MqttQoS.AT_LEAST_ONCE)).collect(Collectors.toList());
        client.subscribe(topicSubList);
    }

    @Test
    void test1() throws InterruptedException {
        // 生产条件下，用户自行实现
        MqttMessageReceiver mmr = new AbstractMqttMessageReceiver() {

            @Override
            public void onPub(MqttPublishMessage msg) {
                ByteBuf content = msg.payload();
                // 将消息打印出来
                System.out.println(content.toString(StandardCharsets.UTF_8));
            }

            @Override
            public void onSubAck(MqttSubAckMessage msg) {
                // todo 订阅响应处理
            }

            @Override
            public void onUnsubAck(MqttUnsubAckMessage msg) {
                // todo 删除订阅响应处理
            }

            @Override
            public void onConnSuccess(ChannelHandlerContext ctx) {
                // todo mqtt conn 登入成功响应处理
                System.out.println("登入成功");
            }
        };

        MqttProperties properties = MqttProperties.builder()
                .withClientId("mqttx-client")
                .withUsername("username")
                .withPassword("password")
                .withCleanSession(true)
                // 心跳间隔
                .withHeartbeatInterval(Duration.ofMinutes(1))
                .withMqttMessageReceiver(mmr)
                .build();

        MqttxClient client = new MqttxClient(properties, new RemoteServerProperties("192.168.32.35", 1883, Duration.ofSeconds(3), Duration.ofSeconds(5)));
        // 同步
        try {
            // 阻塞至 tcp 连接建立成功
            client.startWithBlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.publish("test", "nani".getBytes(StandardCharsets.UTF_8), MqttQoS.AT_LEAST_ONCE);

        TimeUnit.HOURS.sleep(1);
    }
}
