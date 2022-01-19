package com.jun.service;

import com.jun.entity.PubMsg;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 会话服务
 *
 * @author Jun
 * @since 1.0.0
 */
public interface ISessionService {

    /**
     * 通过 {@link InetSocketAddress} 找到与之关联待补发的消息
     *
     * @param socketAddress remote broker server info
     */
    List<PubMsg> findByRemoteServerProperties(InetSocketAddress socketAddress);

    /**
     * 存储待补发的消息。
     * <p>
     * 如果不允许丢失消息，实现必须落盘
     * </p>
     *
     * @param socketAddress remote broker server info
     * @param msg           待补发消息
     */
    void savePubMsg(InetSocketAddress socketAddress, PubMsg msg);

    /**
     * 移除指定 broker 关联的补发消息
     *
     * @param socketAddress remote broker server info
     * @param messageId     消息id
     * @return true if clear success
     */
    boolean clear(InetSocketAddress socketAddress, Integer messageId);
}
