package com.next.mq;

import com.next.util.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Component
@Slf4j
public class RabbitMqClient {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(MessageBody messageBody) {
        try {
            String uuid = UUID.randomUUID().toString();
            CorrelationData correlationData = new CorrelationData(uuid);
            rabbitTemplate.convertAndSend(QueueConstants.COMMON_EXCHANGE, QueueConstants.COMMON_ROUTING,
                    JsonMapper.obj2String(messageBody), new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);// 消息持久化
                            log.info("message send, {}", message);
                            return message;
                        }
                    }, correlationData);
        } catch (Exception e) {
            log.error("message send exception, msg:{}", messageBody.toString(), e);
        }
    }

    public void sendDelay(MessageBody messageBody, int delayMillSeconds) {
        try {
            messageBody.setDelay(delayMillSeconds);
            String uuid = UUID.randomUUID().toString();
            CorrelationData correlationData = new CorrelationData(uuid);
            rabbitTemplate.convertAndSend(QueueConstants.DELAY_EXCHANGE, QueueConstants.DELAY_ROUTING,
                    JsonMapper.obj2String(messageBody), new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);// 消息持久化
                            message.getMessageProperties().setDelay(delayMillSeconds);
                            log.info("delay message send, {}", message);
                            return message;
                        }
                    }, correlationData);
        } catch (Exception e) {
            log.error("delay message send exception, msg:{}", messageBody.toString(), e);
        }
    }
}
