# MQTTX-CLIENT Project

## 1 介绍

`mqttx-client` 基于 [MQTT v3.1.1](http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html) 协议开发， 减少过度封装，提供便捷使用的
mqtt 客户端.

关联项目: [MQTTX broker](https://github.com/Amazingwujun/mqttx)

依赖:

1. java8
2. netty
3. logback

安装 jar 到本地仓库

1. 通过源码安装：使用 `git clone` 将代码同步到本地后执行 `mvn clean install` 即可
2. 通过 jar 包安装
    1. [下载地址](https://github.com/Amazingwujun/mqttx-client/releases)
    2. 通过 mvn 指令将下载的 jar 包安装到仓库（参考 [mvn install本地安装jar到指定仓库](https://www.cnblogs.com/littleorange7/p/14741827.html) ）
3. 在 `pom.xml` 文件中声明

        <dependency>
            <groupId>com.github.jun</groupId>
            <artifactId>mqttx-client</artifactId>
            <version>1.0.0</version>
        </dependency>
至于要不要上传到 maven 中央仓库, 看情况吧.

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

`IQosService` 用于支持 qos1，但是它的默认实现 `InMemoryQosServiceImpl` 依赖内存，当应用重启时，可能会导致消息丢失.
> 当 broker 返回 connAck 且成功，客户端会开启心跳，心跳除定时发送心跳报文外，也会通过 `IQosService` 获取一个心跳周期未收到响应的报文
> 并补发.

#### 3.2 ssl 支持

暂不支持

#### 3.3 掉线重连

配置类 `RemoteServerProperties` 属性 `reconnectInterval` 关系是否需要重连及重连间隔.
