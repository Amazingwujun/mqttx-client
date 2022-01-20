package com.jun;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * 服务器 <code>ip:port</code> 信息
 *
 * @author Jun
 * @since 1.0.0
 */
@Immutable
public class RemoteServerProperties {
    //@formatter:off

    public final InetSocketAddress socketAddress;
    /** 连接超时，单位秒 */
    public final Duration connectTimeout ;
    /** 重连间隔 */
    public final Duration reconnectInterval;

    //@formatter:on

    /**
     * create new instance
     *
     * @param host              ip
     * @param port              端口
     * @param connectTimeout    连接超时时间
     * @param reconnectInterval 重连间隔，可以为 null(代表无需重连)
     */
    public RemoteServerProperties(String host, int port, Duration connectTimeout, Duration reconnectInterval) {
        if (host == null) {
            throw new IllegalArgumentException("host 不能为空");
        }
        this.socketAddress = new InetSocketAddress(host, port);
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
        this.reconnectInterval = reconnectInterval;
    }

    /**
     * 是否需要重连
     *
     * @return true if client need reconnect
     */
    public boolean needReconnect() {
        return reconnectInterval != null;
    }

}
