# MQTTX-CLIENT Project

## 1 介绍

`Mqttx` 基于 [MQTT v3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) 协议开发，实现基于 `netty`
减少过度封装，提供较为便捷的 mqtt 客户端.

依赖:

1. java8
2. netty
3. logback

#### 1.1 快速启动

1 连接建立

```
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
        
        // mqtt broker 配置
        RemoteServerProperties remoteServerProperties = new RemoteServerProperties("", 1773, Duration.ofSeconds(3), Duration.ofSeconds(5));
        
        // mqtt 相关配置
        MqttProperties properties = MqttProperties.builder()
                .withClientId("mqttx-client")
                .withUsername("username")
                .withPassword("password")
                .withCleanSession(true)
                // 心跳间隔
                .withHeartbeatInterval(Duration.ofMinutes(1))
                .withMqttMessageReceiver(mmr)
                .build();
        
        // 构建一个客户端实例
        MqttxClient client = new MqttxClient(properties, removeServerProperties);
        
        // 启动方式有两种
        // 异步
        client.startWithAsync();
        // 同步
        try {
            // 阻塞至 tcp 连接建立成功
            client.startWithBlock();
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            |   AbstractMqttMessageReceiver.java
            |   Immutable.java                  
            |   MqttMessageReceiver.java        
            |   MqttProperties.java             
            |   MqttxClient.java
            |   RemoteServerProperties.java
            |
            +---entity
            |       PubMsg.java
            |       TopicSub.java
            |
            +---handler
            |       ConnAckHandler.java
            |       IMqttHandler.java
            |       MqttHandler.java
            |       MqttMessageDelegatingHandler.java
            |       MqttMessageHandler.java
            |       PingAckHandler.java
            |       PubAckHandler.java
            |       PubCompHandler.java
            |       PublishHandler.java
            |       PubRecHandler.java
            |       PubRelHandler.java
            |       SubAckHandler.java
            |       UnsubAckHandler.java
            |
            \---service
                    InMemorySessionServiceImpl.java
                    ISessionService.java
```

## 3 功能说明

#### 3.1 qos 支持

| qos0 | qos1 | qos2 |
| ---- | ---- |------|
| 支持 | 支持 | 不支持  |

`ISessionService` 用于支持 qos1，但是它的默认实现 `InMemorySessionServiceImpl` 依赖内存，当应用重启时，可能会导致消息丢失.
> 当 broker 返回 connAck 且成功，客户端会开启心跳，心跳除定时发送心跳报文外，也会通过 `ISessionService` 获取一个心跳周期未收到响应的报文
> 并补发.
