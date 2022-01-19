package com.jun.service;

import com.jun.RemoteServerProperties;
import com.jun.entity.PubMsg;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

/**
 * 内存实现，应用重启数据丢失！！！
 *
 * @author Jun
 * @since 1.0.0
 */
public class InMemorySessionServiceImpl implements ISessionService {

    private final Map<String, List<PubMsg>> cache = new HashMap<>();
    private static final ISessionService instance = new InMemorySessionServiceImpl();

    public static ISessionService instance() {
        return instance;
    }

    private InMemorySessionServiceImpl(){}

    @Override
    public List<PubMsg> findByRemoteServerProperties(InetSocketAddress socketAddress) {
        final String key = key(socketAddress);
        return cache.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public void savePubMsg(InetSocketAddress socketAddress, PubMsg msg) {
        if (msg.qoS == MqttQoS.AT_MOST_ONCE) {
            return;
        }
        synchronized (this) {
            final String key = key(socketAddress);
            List<PubMsg> list = cache.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(msg);
        }
    }

    @Override
    public boolean clear(InetSocketAddress socketAddress, Integer messageId) {
        synchronized (this) {
            final String key = key(socketAddress);
            List<PubMsg> list = cache.computeIfAbsent(key, k -> new ArrayList<>());
            return list.removeIf(e -> Objects.equals(messageId, e.messageId));
        }
    }

    private String key(InetSocketAddress socketAddress) {
        return socketAddress.getHostName() + socketAddress.getPort();
    }
}
