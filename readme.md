# MQTTX-CLIENT Project

## 1 介绍

`mqttx-client` 基于 [MQTT v3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) 协议开发， 减少过度封装，提供便捷使用的
mqtt 客户端.

依赖:

1. java8
2. netty
3. logback

#### 1.1 快速启动

1 连接建立

```
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
```

发送 `pub` 消息

```
    String topic = "test"; // 主题
    byte[] paylaod = "hello".getBytes(StandardCharsets.UTF_8); // 载荷
    MqttQoS qoS = MqttQoS.AT_LEAST_ONCE; // qos
    client.publish(topic, paylaod, qoS);
```

发送 `sub` 消息

```
    List<TopicSub> topicSubList = Stream.of(new TopicSub("test", MqttQoS.AT_LEAST_ONCE)).collect(Collectors.toList());
    client.subscribe(topicSubList);
```

## 2 目录结构

```
\---java
    \---com
        \---jun
                AbstractMqttMessageReceiver.java
                Connect.java
                Immutable.java
                InMemoryQosServiceImpl.java
                IQosService.java
                MqttMessageHandler.java
                MqttMessageReceiver.java
                MqttxClient.java
                MqttxException.java
                ObjectUtils.java
                PubMsg.java
                RemoteServerProperties.java
                Session.java
                TopicSub.java
```

## 3 功能说明

#### 3.1 qos 支持

| qos0 | qos1 | qos2 |
| ---- | ---- |------|
| 支持 | 支持 | 不支持  |

`ISessionService` 用于支持 qos1，但是它的默认实现 `InMemorySessionServiceImpl` 依赖内存，当应用重启时，可能会导致消息丢失.
> 当 broker 返回 connAck 且成功，客户端会开启心跳，心跳除定时发送心跳报文外，也会通过 `ISessionService` 获取一个心跳周期未收到响应的报文
> 并补发.
