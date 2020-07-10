package com.next.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitDelayMqConfig {

    @Bean("delayDirectExchange")
    public DirectExchange delayDirectExchange() {
        DirectExchange directExchange = new DirectExchange(QueueConstants.DELAY_EXCHANGE, true, false);
        directExchange.setDelayed(true);
        return directExchange;
    }

    @Bean("delayNotifyQueue")
    public Queue delayNotifyQueue() {
        return new Queue(QueueConstants.DELAY_QUEUE);
    }

    @Bean("delayBindingNotify")
    public Binding delayBindingNotify(@Qualifier("delayDirectExchange") DirectExchange delayDirectExchange,
                                 @Qualifier("delayNotifyQueue") Queue delayNotifyQueue) {
        return BindingBuilder.bind(delayNotifyQueue).to(delayDirectExchange).with(QueueConstants.DELAY_ROUTING);
    }
}
