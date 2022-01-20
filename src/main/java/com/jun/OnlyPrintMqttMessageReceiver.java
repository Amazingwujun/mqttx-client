package com.jun;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 收到消息只打印 string 的接收器
 *
 * @author Jun
 * @since 1.0.0
 */
public final class OnlyPrintMqttMessageReceiver extends AbstractMqttMessageReceiver {

    private static final Logger log = LoggerFactory.getLogger(OnlyPrintMqttMessageReceiver.class);

    /**
     * @param qosService 报文状态服务
     * @param connect    connect 配置
     */
    public OnlyPrintMqttMessageReceiver(IQosService qosService, Connect connect) {
        super(qosService, connect);
    }

    @Override
    protected void onPub0(MqttMessage msg) {
        MqttPublishMessage mpm = (MqttPublishMessage) msg;
        log.info("收到 publish 消息: [{}]", mpm.payload().toString(StandardCharsets.UTF_8));
    }

    @Override
    public void onSubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        MqttSubAckMessage subAckMessage = (MqttSubAckMessage) msg;
        List<Integer> returnCodes = subAckMessage.payload().grantedQoSLevels();
        if (log.isDebugEnabled()) {
            log.debug("订阅响应: {}", returnCodes);
        }
    }

    @Override
    public void onUnsubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        if (log.isDebugEnabled()) {
            log.debug("取消订阅响应: {}", msg);
        }
    }
}
