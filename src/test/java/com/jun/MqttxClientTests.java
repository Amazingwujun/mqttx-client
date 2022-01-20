package com.jun;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class MqttxClientTests {

    @Test
    public void connectTest() {
        // qos1 消息状态缓存服务
        IQosService qosService = InMemoryQosServiceImpl.instance();

        // conn 报文内容
        Connect connect = new Connect("mqttx-client");
        // 可以很复杂
        // connect = new Connect("mqttx-client", "uname", "passwd".getBytes(), false, 60,
        //        true, "nani".getBytes(), "willTopic", MqttQoS.AT_LEAST_ONCE, true);

        // MqttMessageReceiver 用于处理客户端收到的消息
        // AbstractMqttMessageReceiver: 实现了 MqttMessageReceiver, 提供：
        // 1. 自动 connect
        // 2. 短线重连
        // 3. 心跳
        // 等机制.
        MqttMessageReceiver receiver = new OnlyPrintMqttMessageReceiver(qosService, connect);

        // mqtt broker 配置
        RemoteServerProperties remoteServerProperties = new RemoteServerProperties("192.168.32.35", 1883, Duration.ofSeconds(3), Duration.ofSeconds(5));

        // 构建 mqtt client 并获取 session
        Session session = new MqttxClient(receiver, InMemoryQosServiceImpl.instance(), remoteServerProperties)
                .start();
        // 阻塞至收到 broker 对 conn 报文的回复( connAck )
        try {
            session.connectFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
